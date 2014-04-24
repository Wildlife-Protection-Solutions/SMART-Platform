package org.wcs.smart.upgrade;

import java.io.InputStream;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.derby.tools.ij;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.v200.Upgrader112To200;
import org.wcs.smart.upgrade.v300.Upgrader200To300;

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
		V112,
		V200,
		V300
	}
	
	public static void upgrageSystem(IProgressMonitor monitor, Map<String, String> currentVersions) throws Exception {
		monitor.setTaskName(Messages.UpgradeEngine_UpgradeTask);
		Session s = HibernateManager.openSession();
		try {
			final String version = getSmartVersion(s);
			final boolean[] isOk = {false};
			if (!SmartPlugIn.PLUGIN_VERSION.equals(version)) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						isOk[0] = MessageDialog.openQuestion(
								Display.getDefault().getActiveShell(),
								Messages.UpgradeEngine_Confirm_Title,
								MessageFormat.format(Messages.UpgradeEngine_Confirm_Message, version, SmartPlugIn.PLUGIN_VERSION));
					}
				});
				if (!isOk[0]) {
					throw new Exception(Messages.UpgradeEngine_IncompatibleVersion);
				}
				
				UpgradeFromVersion fromVersion = null;
				if ("3.0.0".equals(version)) { //$NON-NLS-1$
					fromVersion = UpgradeFromVersion.V300;
				} else if ("2.0.0".equals(version)) { //$NON-NLS-1$
					fromVersion = UpgradeFromVersion.V200;
				} else if ("1.1.2".equals(version)) { //$NON-NLS-1$
					fromVersion = UpgradeFromVersion.V112;
				}
				
				if (fromVersion == null) {
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openError(
									Display.getDefault().getActiveShell(),
									Messages.UpgradeEngine_Error_Title,
									MessageFormat.format(Messages.UpgradeEngine_Error_Message, version));
						}
					});
					return;
				}
				
				switch (fromVersion) {
				case V112:
					Upgrader112To200.upgrade(s, monitor);
				case V200:
					Upgrader200To300.upgrade(s, monitor);
				default:
					break;
				}
			}
			
			if (currentVersions != null) {
				Map<String, String> backupVersions = getVersions(s);
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
			
			List<IDatabaseUpgrader> extensions = getExtensions();
			for (IDatabaseUpgrader upgrader : extensions) {
				upgrader.upgrade(s, monitor);
			}
			
		} finally {
			s.close();
		}
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
			//most likely it is because of old version which doesn't contain pligin_id column
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
