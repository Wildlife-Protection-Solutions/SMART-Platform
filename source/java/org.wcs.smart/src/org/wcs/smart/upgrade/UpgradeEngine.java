package org.wcs.smart.upgrade;

import java.io.InputStream;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.v200.Upgrader112To200;
import org.wcs.smart.upgrade.v300.Upgrader200To300;
import org.wcs.smart.upgrade.v300.Upgrader300To302;
import org.wcs.smart.upgrade.v310.Upgrader302To310;
import org.wcs.smart.upgrade.v320.Upgrader310To320;

/**
 * Check if provided backup requires update to satisfy current SMART configuration
 * and performs this update if required.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class UpgradeEngine {

	private static final String EXTENSION_ID = "org.wcs.smart.dbUpgrage"; //$NON-NLS-1$
	
	private enum UpgradeFromVersion {
		V112("1.1.2", Upgrader112To200.class), //$NON-NLS-1$
		V200("2.0.0", Upgrader200To300.class), //$NON-NLS-1$
		V300("3.0.0", Upgrader300To302.class), //$NON-NLS-1$
		V302("3.0.2", Upgrader302To310.class), //$NON-NLS-1$
		V310("3.1.0", Upgrader310To320.class); //$NON-NLS-1$
		
		public String versionString;
		public Class<? extends IDatabaseUpgrader> upgradeEngine;
		
		private UpgradeFromVersion(String version, Class<? extends IDatabaseUpgrader> engine){
			this.versionString = version;
			this.upgradeEngine = engine;
		}
	}
	
	/**
	 * 
	 * @param monitor progress monitor
	 * @param currentVersions map from plugin id to database version for the existing database
	 * @throws Exception
	 */
	public static void upgrageSystem(IProgressMonitor monitor, Map<String, String> currentVersions) throws Exception {
		monitor.setTaskName(Messages.UpgradeEngine_UpgradeTask);
		
		
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

		Set<IDatabaseUpgrader> upgradersRun = new HashSet<IDatabaseUpgrader>();
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
				if (v.versionString.equals(newDbVersion)){
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
			for (int i = startIndex; i < UpgradeFromVersion.values().length; i ++){
				UpgradeFromVersion v = UpgradeFromVersion.values()[i];
				IDatabaseUpgrader upgrader = v.upgradeEngine.newInstance();
				upgrader.upgrade(monitor);
				upgradersRun.add(upgrader);
			}
				
		}
		
		/* --- validate & update plugins ---*/
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
			
		/* --- additional upgrade options --- */
		List<IDatabaseUpgrader> extensions = getExtensions();
		for (IDatabaseUpgrader upgrader : extensions) {
			upgrader.upgrade(monitor);
		}
		/* --- post process  --- */
		//this is done here to ensure all plugin tables that are required
		//to check delete are installed
		for (IDatabaseUpgrader up : upgradersRun){
			if (up instanceof Upgrader310To320){
				((Upgrader310To320) up).postProcess(monitor);
			}
		}
		
		monitor.subTask(""); //$NON-NLS-1$
	}

	public static String getSmartVersion(Session s) {
		Map<String, String> versions = getVersions(s);
		if (versions != null) {
			return versions.get(SmartPlugIn.PLUGIN_ID);
		}
		//NOTE: before 3.0.0 db-version table contained only single column with one value
		String version = (String) s.createSQLQuery("SELECT version FROM " + SmartDB.PLUGIN_VERSION_TBL).uniqueResult(); //$NON-NLS-1$
		return version;
	}

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
	
	/**
	 * Runs an file containing a set of sql commands.  
	 * Note: input stream is closed when complete 
	 * 
	 * @param databaseConnection current database connection
	 * @param updateScript inputstream representing the queries to run
	 */
	public static void runScript(Connection databaseConnection, InputStream in) throws Exception{
		try{
			ij.runScript(databaseConnection, in, "utf-8", System.out, "utf-8");  //$NON-NLS-1$//$NON-NLS-2$
		}finally{
			in.close();
		}
	}

	/**
	 * @return list of {@link IDatabaseUpgrader} extension points
	 */
	private static List<IDatabaseUpgrader> getExtensions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IDatabaseUpgrader> items = new ArrayList<IDatabaseUpgrader>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((IDatabaseUpgrader)e.createExecutableExtension("upgrader")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}

}
