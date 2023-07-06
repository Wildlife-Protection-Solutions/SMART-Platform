/*
 * Copyright (C) 2021 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.upgrade;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.changetracking.ChangeLogInstaller;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.v321.Upgrader320To321;
import org.wcs.smart.upgrade.v330.Upgrader321To330;
import org.wcs.smart.upgrade.v330.Upgrader330To331;
import org.wcs.smart.upgrade.v400.Upgrader331To400;
import org.wcs.smart.upgrade.v400.Upgrader400To401;
import org.wcs.smart.upgrade.v410.Upgrader401To410;
import org.wcs.smart.upgrade.v500.Upgrader410To500;
import org.wcs.smart.upgrade.v600.Upgrader500To600;
import org.wcs.smart.upgrade.v600.Upgrader600To601;
import org.wcs.smart.upgrade.v600.Upgrader601To610;
import org.wcs.smart.upgrade.v600.Upgrader610To620;
import org.wcs.smart.upgrade.v600.Upgrader620To630;
import org.wcs.smart.upgrade.v700.Upgrader630To700;
import org.wcs.smart.upgrade.v700.Upgrader700To750;
import org.wcs.smart.upgrade.v700.Upgrader750To751;
import org.wcs.smart.upgrade.v700.Upgrader751To753;
import org.wcs.smart.upgrade.v700.Upgrader753To754;
import org.wcs.smart.upgrade.v700.Upgrader754To757;
import org.wcs.smart.upgrade.v800.Upgrader753To800;


/**
 * Check if provided backup requires update to satisfy current SMART configuration
 * and performs this update if required.  
 * 
 * @author elitvin
 * @author egouge
 * @since 3.0.0
 */
public class UpgradeEngine {

	private static final String EXTENSION_ID = "org.wcs.smart.dbUpgrade"; //$NON-NLS-1$
	
	public enum UpgradeFromVersion {
//		V112("1.1.2", "2.0.0", Upgrader112To200.class), //$NON-NLS-1$ //$NON-NLS-2$
//		V200("2.0.0", "3.0.0", Upgrader200To300.class), //$NON-NLS-1$ //$NON-NLS-2$
//		V300("3.0.0", "3.0.2", Upgrader300To302.class), //$NON-NLS-1$ //$NON-NLS-2$
//		V302("3.0.2", "3.1.0", Upgrader302To310.class), //$NON-NLS-1$ //$NON-NLS-2$
//		V310("3.1.0", "3.2.0", Upgrader310To320.class), //$NON-NLS-1$ //$NON-NLS-2$
		//SEE ticket 2707 - as of 6.2 we only support upgrading from 3.2
		V320("3.2.0", "3.2.1", Upgrader320To321.class), //$NON-NLS-1$ //$NON-NLS-2$
		V330("3.2.1", "3.3.0", Upgrader321To330.class), //$NON-NLS-1$ //$NON-NLS-2$
		V331("3.3.0", "3.3.1", Upgrader330To331.class), //$NON-NLS-1$ //$NON-NLS-2$
		V400("3.3.1", "4.0.0", Upgrader331To400.class), //$NON-NLS-1$ //$NON-NLS-2$
		V401("4.0.0", "4.0.1", Upgrader400To401.class), //$NON-NLS-1$ //$NON-NLS-2$
		V410("4.0.1", "4.1.0", Upgrader401To410.class), //$NON-NLS-1$ //$NON-NLS-2$
		V500("4.1.0", "5.0.0", Upgrader410To500.class), //$NON-NLS-1$ //$NON-NLS-2$
		V600("5.0.0", "6.0.0", Upgrader500To600.class), //$NON-NLS-1$ //$NON-NLS-2$
		V601("6.0.0", "6.0.1", Upgrader600To601.class), //$NON-NLS-1$ //$NON-NLS-2$
		V610("6.0.1", "6.1.0", Upgrader601To610.class), //$NON-NLS-1$ //$NON-NLS-2$
		V620("6.1.0", "6.2.0", Upgrader610To620.class), //$NON-NLS-1$ //$NON-NLS-2$
		V630("6.2.0", "6.3.0", Upgrader620To630.class), //$NON-NLS-1$ //$NON-NLS-2$
		V700("6.3.0", "7.0.0", Upgrader630To700.class), //$NON-NLS-1$ //$NON-NLS-2$
		V750("7.0.0", "7.5.0", Upgrader700To750.class), //$NON-NLS-1$ //$NON-NLS-2$
		V751("7.5.0", "7.5.1", Upgrader750To751.class), //$NON-NLS-1$ //$NON-NLS-2$
		V753("7.5.1", "7.5.3", Upgrader751To753.class), //$NON-NLS-1$ //$NON-NLS-2$
		V754("7.5.3", "7.5.4", Upgrader753To754.class), //$NON-NLS-1$ //$NON-NLS-2$
		V757("7.5.4", "7.5.7", Upgrader754To757.class), //$NON-NLS-1$ //$NON-NLS-2$
		V800("7.5.7", "8.0.0", Upgrader753To800.class); //$NON-NLS-1$ //$NON-NLS-2$
		
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
	 * 
	 * @throws Exception
	 */
	public void upgradeSystem(IProgressMonitor monitor) throws Exception {
		List<IDatabaseUpgrader> upgraders = getExtensions();
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UpgradeEngine_UpgradeTask, 3 + 2 + upgraders.size());
		
		upgradersRun.clear();

		//read current versions
		progress.subTask(Messages.UpgradeEngine_LoadingTaskName);
		Map<String, String> currentVersions = null;
		try(Session s = HibernateManager.openSession()){
			currentVersions = getVersions(s);
		}
		progress.worked(1);
		
		//upgrade
		progress.subTask(Messages.UpgradeEngine_UpgradingTaskName);
		List<IDatabaseUpgrader> run = new ArrayList<>();
		for (IDatabaseUpgrader upgrader : upgraders) {
			SubMonitor p = progress.split(1);
			if (upgrader.isUpdateToDate(currentVersions)) continue;
			
			//remove any existing change log tracking
			//this is done so any alter table statements are not
			//prevented from running because of the triggers
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				ChangeLogInstaller.INSTANCE.uninstallChangeLogTracking(s, upgrader.getPluginId());
				s.getTransaction().commit();
			}
			
			//execute install/upgrade
			upgrader.upgrade(p);
			run.add(upgrader);		
		}
		
		//postprocess
		postProcess(progress.split(1), run);

		//add back all change log tracking if necessary
		progress.subTask(Messages.UpgradeEngine_ChangeLogTaskName);
		SubMonitor monitor1 = progress.split(2);
		monitor1.beginTask(Messages.UpgradeEngine_ChangeLogTaskName, run.size());
		for (IDatabaseUpgrader upgrader : run) {
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				monitor1.split(1);
				monitor1.subTask(Messages.UpgradeEngine_ChangeLogTaskName + ": " + upgrader.getPluginId()); //$NON-NLS-1$
				ChangeLogInstaller.INSTANCE.installChangeLogTracking(s, upgrader.getPluginId());
				s.getTransaction().commit();
			}
		}
		
		//compress tables
		progress.subTask(Messages.UpgradeEngine_CompressingTaskName);
		try(Session s = HibernateManager.openSession()){
			HibernateManager.compressTables(s, progress.split(1));
		}
		
		progress.done();
	}

	/**
	 * Runs any post-processing scripts.  These are to be run
	 * after the upgrade has occurred and the plugin and plugin version validation
	 * has completed.
	 * These should not in any way modify the plugin or plugin version table.
	 * @param monitor
	 * @throws Exception 
	 * @throws OperationCanceledException 
	 */
	private void postProcess(IProgressMonitor monitor, List<IDatabaseUpgrader> upgradersRun) throws OperationCanceledException, Exception{
		/* --- post process  --- */
		//this is done here to ensure all plugin tables that are required
		//to check delete are installed
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.setWorkRemaining(upgradersRun.size());
		for (IDatabaseUpgrader up : upgradersRun){
			up.postProcess(progress.split(1));
		}
	}

	/**
	 * 
	 * @return list of {@link IDatabaseUpgrader} extension points sorted by requires flag
	 * @throws Exception 
	 */
	public static List<IDatabaseUpgrader> getExtensions() throws Exception {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IDatabaseUpgrader> items = new ArrayList<IDatabaseUpgrader>();
		List<String> names = new ArrayList<>();
		
		List<Object[]> toprocess = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		
		IDatabaseUpgrader core = null;
		try {			
			for (IConfigurationElement e : config) {
				if (e.getName().equals("dbUpgrader")){ //$NON-NLS-1$
					String requires = e.getAttribute("requires"); //$NON-NLS-1$
					String name = e.getContributor().getName();
					IDatabaseUpgrader up = (IDatabaseUpgrader)e.createExecutableExtension("upgrader"); //$NON-NLS-1$
					if (requires == null) {
						items.add(up); 
						names.add(name);
					}else {
						toprocess.add(new Object[] {requires, name, up});
					}
					if (up.getClass().equals(CoreDatabaseUpgrader.class)) {
						core = up;
					}
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}

		int cnt = 0;
		while(!toprocess.isEmpty()) {
			if (cnt > toprocess.size()) {
				throw new Exception("Circular dependency associated with database upgraders."); //$NON-NLS-1$
			}
			Object[] item = toprocess.remove(0);
			if (names.contains(item[0])) {
				items.add((IDatabaseUpgrader)item[2]);
				names.add((String)item[1]);
				cnt = 0;
			}else {
				toprocess.add(item);
				cnt++;
			}
		}
		
		//make sure core is the first one to run
		items.remove(core);
		items.add(0, core);
		return items;
	}

	public static List<IDatabaseUpgrader> getCoreExtensions(String fromVersion, String toVersion) {
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
	
	public static HashMap<String, String[]> getPluginMappings() {
		if (Platform.getExtensionRegistry() == null) return new HashMap<>();
		HashMap<String,String[]> pluginMappings = new HashMap<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals("pluginMapping")){ //$NON-NLS-1$
					String oldVersion = e.getAttribute("old_version_id"); //$NON-NLS-1$
					String oldPluginId = e.getAttribute("old_plugin_id"); //$NON-NLS-1$
					String newPluginId = e.getAttribute("new_plugin_id"); //$NON-NLS-1$
					pluginMappings.put(oldPluginId, new String[] {oldVersion, newPluginId});
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return pluginMappings;
	}
	
	/**
	 * Gets all map of the PlugIn ID to Plugin Version stored in the 
	 * plugin versions database table. Throws an exception if versions cannot be determined
	 * 
	 * @param s
	 * @return
	 */
	public static Map<String, String> getVersions(Session s) throws Exception{
		Map<String, String> versions = HibernateManager.getPlugInVersions(s);
		
		if (versions == null) {
			//NOTE: before 3.0.0 db-version table contained only single column with one value
			List<String> version = s.createNativeQuery("SELECT version FROM " + SmartDB.PLUGIN_VERSION_TBL, String.class).list(); //$NON-NLS-1$
			if (version.size() == 1 && !version.get(0).isBlank()) {
				versions = new HashMap<>();
				versions.put(SmartPlugIn.PLUGIN_ID, version.get(0));
			}
		}
		if (versions == null) throw new Exception(Messages.UpgradeEngine_VersionError);
		
		return versions;
		
	}
	
	/**
	 * Opens a database sessions, gets the current versions and validates them against
	 * the current version in the database. 
	 * 
	 * 
	 * @return A string that is null if versions match, or else contains a description of the plugins
	 * that don't match. 
	 * 
	 * @throws Exception if can't read versions from database
	 */
	public static String validateVersions() throws Exception{
		
		try(Session session = HibernateManager.openSession()){
			
			Map<String, String> currentversions = UpgradeEngine.getVersions(session);
			List<IDatabaseUpgrader> extensions = UpgradeEngine.getExtensions();
			
			List<IDatabaseUpgrader> torun = new ArrayList<>();
			for (IDatabaseUpgrader upgrader : extensions) {
				if (!upgrader.isUpdateToDate(currentversions)) {
					torun.add(upgrader);
				}
			}
			if (!torun.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				sb.append("The following plugins are out of date:"); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
				for (IDatabaseUpgrader up : torun) {
					sb.append(up.getPluginName());
					sb.append("\n"); //$NON-NLS-1$
				}
				return sb.toString();				
			}
			return null;
		}
			
	}
}
