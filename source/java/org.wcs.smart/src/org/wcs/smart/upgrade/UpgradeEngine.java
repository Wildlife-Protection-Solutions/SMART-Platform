package org.wcs.smart.upgrade;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.derby.tools.ij;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.v200.Upgrader112To200;
import org.wcs.smart.upgrade.v300.Upgrader200To300;
import org.wcs.smart.upgrade.v300.Upgrader300To302;
import org.wcs.smart.upgrade.v310.Upgrader302To310;
import org.wcs.smart.upgrade.v320.Upgrader310To320;
import org.wcs.smart.upgrade.v321.Upgrader320To321;
import org.wcs.smart.upgrade.v330.Upgrader321To330;
import org.wcs.smart.upgrade.v330.Upgrader330To331;
import org.wcs.smart.upgrade.v400.Upgrader331To400;
import org.wcs.smart.upgrade.v400.Upgrader400To401;
import org.wcs.smart.upgrade.v500.Upgrader410To500;


/**
 * Check if provided backup requires update to satisfy current SMART configuration
 * and performs this update if required.  
 * 
 * @author elitvin
 * @author egouge
 * @since 3.0.0
 */
public class UpgradeEngine {

	private static final String EXTENSION_ID = "org.wcs.smart.dbUpgrage"; //$NON-NLS-1$
	
	public enum UpgradeFromVersion {
		V112("1.1.2", "2.0.0", Upgrader112To200.class), //$NON-NLS-1$ //$NON-NLS-2$
		V200("2.0.0", "3.0.0", Upgrader200To300.class), //$NON-NLS-1$ //$NON-NLS-2$
		V300("3.0.0", "3.0.2", Upgrader300To302.class), //$NON-NLS-1$ //$NON-NLS-2$
		V302("3.0.2", "3.1.0", Upgrader302To310.class), //$NON-NLS-1$ //$NON-NLS-2$
		V310("3.1.0", "3.2.0", Upgrader310To320.class), //$NON-NLS-1$ //$NON-NLS-2$
		V320("3.2.0", "3.2.1", Upgrader320To321.class), //$NON-NLS-1$ //$NON-NLS-2$
		V330("3.2.1", "3.3.0", Upgrader321To330.class), //$NON-NLS-1$ //$NON-NLS-2$
		V331("3.3.0", "3.3.1", Upgrader330To331.class), //$NON-NLS-1$ //$NON-NLS-2$
		V400("3.3.1", "4.0.0", Upgrader331To400.class), //$NON-NLS-1$ //$NON-NLS-2$
		V401("4.0.0", "4.0.1", Upgrader400To401.class), //$NON-NLS-1$ //$NON-NLS-2$
		V500("4.1.0", "5.0.0", Upgrader410To500.class); //$NON-NLS-1$ //$NON-NLS-2$
		
		public String fromVersion;
		public String toVersion;
		public Class<? extends IDatabaseUpgrader> upgradeEngine;
		
		
		private UpgradeFromVersion(String fromVersion, String toVersion, Class<? extends IDatabaseUpgrader> engine){
			this.fromVersion = fromVersion;
			this.toVersion = toVersion;
			this.upgradeEngine = engine;
		}
	}
	
	private Set<IDatabaseUpgrader> upgradersRun = new HashSet<IDatabaseUpgrader>();
	
	public UpgradeEngine(){
		
	}
	
	/**
	 * 
	 * @param monitor progress monitor
	 * @param currentVersions map from plugin id to database version for the existing database
	 * @throws Exception
	 */
	public void upgradeSystem(IProgressMonitor monitor, Map<String, String> currentVersions) throws Exception {
		monitor.beginTask(Messages.UpgradeEngine_UpgradeTask, 5);
		
		monitor.subTask(Messages.UpgradeEngine_subprogress1);
		final boolean[] isOk = {false};
		String lnewDbVersion = null;
		String lexpectedDbVersion = null;
		
		Session s = HibernateManager.openSession();
		try {
			lnewDbVersion = getSmartVersion(s);
			lexpectedDbVersion = SmartProperties.getInstance().getProperty(SmartProperties.DB_VERSION_KEY);
		} finally {
			s.close();
		}
		final String newDbVersion = lnewDbVersion;
		final String expectedDbVersion = lexpectedDbVersion;

		upgradersRun.clear();
		
		/* --- validate the core version; upgrade as required --- */
		if (!expectedDbVersion.equals(newDbVersion)) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					isOk[0] = MessageDialog.openQuestion(
							Display.getDefault().getActiveShell(),
							Messages.UpgradeEngine_Confirm_Title,
							MessageFormat.format(Messages.UpgradeEngine_Confirm_Message, newDbVersion, expectedDbVersion));
				}
			});
			if (!isOk[0]) {
				throw new Exception(Messages.UpgradeEngine_IncompatibleVersion);
			}
		
			UpgradeFromVersion fromVersion = null;
			for (UpgradeFromVersion v : UpgradeFromVersion.values()){
				if (v.fromVersion.equals(newDbVersion)){
					fromVersion = v;
				}
			}
			
			if (fromVersion == null) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								Messages.UpgradeEngine_Error_Title,
								MessageFormat.format(Messages.UpgradeEngine_Error_Message, newDbVersion));
					}
				});
				return;
			}
			
			//find the index of the current from version; then
			//run all upgrades from that index to upgrade to the 
			//current version
			//assumes we are upgrading to the latest version
			int startIndex = 0;
			for (int i = 0; i < UpgradeFromVersion.values().length; i++){
				if (fromVersion == UpgradeFromVersion.values()[i]){
					startIndex = i ;
					break;
				}
			}
			monitor.worked(1);
			
			monitor.subTask(Messages.UpgradeEngine_subprogress2);
			for (int i = startIndex; i < UpgradeFromVersion.values().length; i ++){
				UpgradeFromVersion v = UpgradeFromVersion.values()[i];
				IDatabaseUpgrader upgrader = v.upgradeEngine.newInstance();
				upgrader.upgrade(new SubProgressMonitor(monitor, 0));
				upgradersRun.add(upgrader);
				
				List<IDatabaseUpgrader> additionalItems = getCoreExtensions(v.fromVersion, v.toVersion);
				for (IDatabaseUpgrader item : additionalItems){
					item.upgrade(new SubProgressMonitor(monitor, 0));
				}
			}
			monitor.worked(1);
		}
		
		/* --- validate & update plugins ---*/
		boolean requiresUpgrades = false;
		monitor.subTask(Messages.UpgradeEngine_subprogress3);
		if (currentVersions != null) {
			Map<String, String> backupVersions;
			s = HibernateManager.openSession();
			try {
				backupVersions = getVersions(s);
			} finally {
				s.close();
			}
			String problems = ""; //$NON-NLS-1$
			for (String curPlugin : currentVersions.keySet()) {
				if (backupVersions.containsKey(curPlugin)) {
					String curV = currentVersions.get(curPlugin);
					String backV = backupVersions.get(curPlugin); 
					if (!curV.equals(backV)) {
						problems += MessageFormat.format(Messages.UpgradeEngine_Plugin_WrongVersion, curPlugin, backV, curV) + "\n"; //$NON-NLS-1$
					}
				} else {
					problems += MessageFormat.format(Messages.UpgradeEngine_Plugin_Missing, curPlugin) + "\n"; //$NON-NLS-1$
				}
			}
			if (!problems.isEmpty()) {
				requiresUpgrades = true;
				final String msg = problems;
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openWarning(
								Display.getDefault().getActiveShell(),
								Messages.UpgradeEngine_Confirm_Title,
								MessageFormat.format(Messages.UpgradeEngine_Plugin_UpgradeMessage, msg));
					}
				});
				
			}
		}
		monitor.worked(1);
		
		if (requiresUpgrades){
			//uninstall all change log tracking
			monitor.subTask(Messages.UpgradeEngine_subprogress5);
			s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				ChangeLogInstaller.INSTANCE.uninstallChangeLogTracking(s);
				s.getTransaction().commit();
			}finally{
				s.close();
			}
	
			//run all installers/upgraders
			List<IDatabaseUpgrader> extensions = getExtensions();
			for (IDatabaseUpgrader upgrader : extensions) {
				//execute install/upgrade
				upgrader.upgrade(new SubProgressMonitor(monitor, 0));
			}
			
			//install change log tracking (if necessary)
			monitor.subTask(Messages.UpgradeEngine_subprogress6);
			s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				ChangeLogInstaller.INSTANCE.installChangeLogTracking(s);
				s.getTransaction().commit();
			}finally{
				s.close();
			}
		}
		monitor.worked(1);
		
		monitor.done();
	}

	/**
	 * Runs any post-processing scripts.  These are to be run
	 * after the upgrade has occurred and the plugin and plugin version validation
	 * has completed.
	 * These should not in any way modify the plugin or plugin version table.
	 * @param monitor
	 */
	public void postProcess(IProgressMonitor monitor){
		/* --- post process  --- */
		//this is done here to ensure all plugin tables that are required
		//to check delete are installed
		for (IDatabaseUpgrader up : upgradersRun){
			if (up instanceof Upgrader310To320){
				((Upgrader310To320) up).postProcess(new SubProgressMonitor(monitor, 0));
			}
		}
	}

	private String getSmartVersion(Session s) {
		Map<String, String> versions = getVersions(s);
		if (versions != null) {
			return versions.get(SmartPlugIn.PLUGIN_ID);
		}
		//NOTE: before 3.0.0 db-version table contained only single column with one value
		String version = (String) s.createSQLQuery("SELECT version FROM " + SmartDB.PLUGIN_VERSION_TBL).uniqueResult(); //$NON-NLS-1$
		return version;
	}

	

	/**
	 * @return list of {@link IDatabaseUpgrader} extension points
	 */
	private List<IDatabaseUpgrader> getExtensions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IDatabaseUpgrader> items = new ArrayList<IDatabaseUpgrader>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("dbUpgrader")){ //$NON-NLS-1$
					items.add((IDatabaseUpgrader)e.createExecutableExtension("upgrader")); //$NON-NLS-1$
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}

	private List<IDatabaseUpgrader> getCoreExtensions(String fromVersion, String toVersion) {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IDatabaseUpgrader> items = new ArrayList<IDatabaseUpgrader>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("coreUpgrader")){ //$NON-NLS-1$
					String efromVersion = e.getAttribute("fromVersion"); //$NON-NLS-1$
					String etoVersion = e.getAttribute("toVersion"); //$NON-NLS-1$
					if (efromVersion.equals(fromVersion) && etoVersion.equals(toVersion)){
						items.add((IDatabaseUpgrader)e.createExecutableExtension("upgrader")); //$NON-NLS-1$
					}
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
	
	/**
	 * Runs an file containing a set of sql commands.  
	 * Note: input stream is closed when complete 
	 * 
	 * @param databaseConnection current database connection
	 * @param updateScript inputstream representing the queries to run
	 */
	public static void runScript(Connection databaseConnection, InputStream in) throws Exception{
		try{
			ij.runScript(databaseConnection, in, StandardCharsets.UTF_8.name(), System.out, StandardCharsets.UTF_8.name());
		}finally{
			in.close();
		}
	}
	
	/**
	 * Gets all map of the pluginid to pluginversion stored in the 
	 * plugin versions database table.
	 * 
	 * @param s
	 * @return
	 */
	public static Map<String, String> getVersions(Session s) {
		Map<String, String> versions = new HashMap<String, String>();
		try {
			List<?> tmpversions = s.createSQLQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			for (Object x : tmpversions){
				String pluginid = (String) ((Object[])x)[0];
				String version = (String) ((Object[])x)[1];
				versions.put(pluginid, version);
			}
		} catch (Exception e) {
			//most likely it is because of old version which doesn't contain plugin_id column
			return null;
		}
		return versions;
	}
}
