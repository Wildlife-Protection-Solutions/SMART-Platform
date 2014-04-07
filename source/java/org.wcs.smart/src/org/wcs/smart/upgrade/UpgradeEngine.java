package org.wcs.smart.upgrade;

import java.io.InputStream;
import java.sql.Connection;

//import org.apache.derby.tools.ij;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.upgrade.v300.Upgrader200To300;

/**
 * Check if provided backup requires update to satisfy current SMART configuration
 * and performs this update if required.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class UpgradeEngine {

	private enum UpgradeFromVersion {
		V110,
		V200
	}
	
	public static void upgrageSystem(IProgressMonitor monitor) {
		Session s = HibernateManager.openSession();
		try {
			String version = getVersion(s);
			UpgradeFromVersion fromVersion = null;
			if ("2.0.0".equals(version)) { //$NON-NLS-1$
				fromVersion = UpgradeFromVersion.V200;
			} else if ("1.1.0".equals(version)) { //$NON-NLS-1$
				fromVersion = UpgradeFromVersion.V110;
			}
			
			if (fromVersion == null){
				return; //TODO: need some message
			}
			
			switch (fromVersion) {
			case V110:
//				Upgrader110To200.upgrade(s, monitor);
			case V200:
				Upgrader200To300.upgrade(s, monitor);
			default:
				break;
			}
			
		} finally {
			s.close();
		}
	}

	public static String getVersion(Session s) {
		//NOTE: before 3.0.0 db-version table contained only single column with one value
		String version = (String) s.createSQLQuery("SELECT version FROM " + SmartDB.PLUGIN_VERSION_TBL).uniqueResult(); //$NON-NLS-1$
		return version;
	}

	/**
	 * Runs an file containing a set of sql commands.  
	 * Note: input stream is closed when complete 
	 * 
	 * @param databaseConnection current database connection
	 * @param updateScript inputstream representing the queries to run
	 */
	public static void runScript(Connection databaseConnection, InputStream in) throws Exception{
//		try{
//			ij.runScript(databaseConnection, in, "utf-8", System.out, "utf-8");  //$NON-NLS-1$//$NON-NLS-2$
//		}finally{
//			in.close();
//		}
	}
	
}
