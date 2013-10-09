/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;

/**
 * Performs activities that are required on startup:
 * - ensure that database tables exist (create then if the do not exist)
 * - clean up storage according to specified settings
 * - ensure all storage locations exist
 * - record all required registry keys
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerStartupJob extends Job {

	public CyberTrackerStartupJob() {
		super(Messages.CyberTrackerPlugIn_InitJob_Title);
	}


	@Override
	protected IStatus run(IProgressMonitor monitor) {
		buildTables();
		List<ConservationArea> caList = null;
		List<CyberTrackerProperties> propList = null;
		Session session = HibernateManager.openSession();
		try {
			caList = HibernateManager.getConservationAreas(session);
			propList = CyberTrackerHibernateManager.getAllProperties(session);
		} catch (Exception e) {
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to select CA list and CyberTracker properties.", e)); //$NON-NLS-1$
		} finally {
			session.close();
		}
		checkFolderAndRegistry(caList);
		cleanStorage(caList, propList);
		return Status.OK_STATUS;
	}

	/**
	 * Ensures that required tables are present in database and add them is case they are not present
	 */
	private void buildTables() {
		final boolean tables[] = {false}; //properties table
		Session session = HibernateManager.openSession();
		//check is required table exists
		try {
			session.beginTransaction();
			String sql = "select count(*) from SYS.SYSTABLES tbl inner join SYS.SYSSCHEMAS sch on tbl.SCHEMAID = sch.SCHEMAID AND sch.SCHEMANAME = 'SMART' WHERE tbl.TABLETYPE = 'T' AND tbl.TABLENAME = 'CYBERTRACKER_PROPERTIES'"; //$NON-NLS-1$
			SQLQuery q = session.createSQLQuery(sql);
			Integer result = (Integer) q.uniqueResult();
			tables[0] = result > 0;
			
//			sql = "select count(*) from SYS.SYSTABLES tbl inner join SYS.SYSSCHEMAS sch on tbl.SCHEMAID = sch.SCHEMAID AND sch.SCHEMANAME = 'SMART' WHERE tbl.TABLETYPE = 'T' AND tbl.TABLENAME = 'CT_PATROL_OPTION'"; //$NON-NLS-1$
//			q = session.createSQLQuery(sql);
//			result = (Integer) q.uniqueResult();
//			tables[1] = result > 0;
			
			if (tables[0])
				return; //required table exists
		} catch (Exception e) {
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to obtain information about CyberTracker plugin tables.", e)); //$NON-NLS-1$
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		
		// need to login as admin user to create tables
		HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			session.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					if (!tables[0]) {
						String createSql = "CREATE TABLE smart.cybertracker_properties ("+ //$NON-NLS-1$
								"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
								"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
								"storage_time INTEGER, "+ //$NON-NLS-1$
								"large_scroll_bars BOOLEAN, "+ //$NON-NLS-1$
								"auto_next BOOLEAN, "+ //$NON-NLS-1$
								"application_name VARCHAR(256), "+ //$NON-NLS-1$
								"kiosk_mode BOOLEAN, "+ //$NON-NLS-1$
								"exit_pin INTEGER, "+ //$NON-NLS-1$
								"sighting_accuracy DOUBLE, "+ //$NON-NLS-1$
								"sighting_fix_count INTEGER, "+ //$NON-NLS-1$
								"waypoint_timer INTEGER, "+ //$NON-NLS-1$
								"gps_time_zone INTEGER, "+ //$NON-NLS-1$
								"skip_button_timeout INTEGER, "+ //$NON-NLS-1$
								"PRIMARY KEY (UUID))"; //$NON-NLS-1$

						String alterSql = "ALTER TABLE smart.cybertracker_properties "+ //$NON-NLS-1$
								"ADD CONSTRAINT cybertracker_properties_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
								"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
								"ON UPDATE RESTRICT "+ //$NON-NLS-1$
								"ON DELETE CASCADE"; //$NON-NLS-1$

						c.createStatement().execute(createSql);
						c.createStatement().execute(alterSql);
						
						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to data_entry"); //$NON-NLS-1$
						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to manager"); //$NON-NLS-1$
						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to analyst"); //$NON-NLS-1$
						c.createStatement().execute("GRANT SELECT ON smart.cybertracker_properties to login"); //$NON-NLS-1$
					}

//					if (!tables[1]) {
//						String createSql = "CREATE TABLE smart.ct_patrol_option ("+ //$NON-NLS-1$
//								"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
//								"properties_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
//								"type VARCHAR(10), "+ //$NON-NLS-1$
//								"is_visible BOOLEAN, "+ //$NON-NLS-1$
//								"string_value VARCHAR(8192), "+ //same as patrol.comment //$NON-NLS-1$
//								"boolean_value BOOLEAN, "+ //$NON-NLS-1$
//								"uuid_value CHAR(16) for bit data, "+ //$NON-NLS-1$
//								"PRIMARY KEY (UUID))"; //$NON-NLS-1$
//
//						String alterSql = "ALTER TABLE smart.ct_patrol_option "+ //$NON-NLS-1$
//								"ADD CONSTRAINT cybertracker_ct_patrol_option_properties_uuid_fk FOREIGN KEY (PROPERTIES_UUID) "+ //$NON-NLS-1$
//								"REFERENCES smart.cybertracker_properties(UUID) "+ //$NON-NLS-1$
//								"ON UPDATE RESTRICT "+ //$NON-NLS-1$
//								"ON DELETE CASCADE"; //$NON-NLS-1$
//
//						c.createStatement().execute(createSql);
//						c.createStatement().execute(alterSql);
//						
//						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.ct_patrol_option to data_entry"); //$NON-NLS-1$
//						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.ct_patrol_option to manager"); //$NON-NLS-1$
//						c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.ct_patrol_option to analyst"); //$NON-NLS-1$
//						c.createStatement().execute("GRANT SELECT ON smart.ct_patrol_option to login"); //$NON-NLS-1$
//					}					
				}
			});
			session.getTransaction().commit();
		} catch (Exception ex) {
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to create CyberTracker plugin tables.", ex)); //$NON-NLS-1$
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			if (session.isOpen()) {
				session.close();
			}
			// disconnect from admin user
			HibernateManager.endSessionFactory(true);
		}
	}
	
	private void checkFolderAndRegistry(List<ConservationArea> caList) {
		if (caList == null)
			return;
		for (ConservationArea ca : caList) {
			try {
				PdaUtil.updateRegistryKey(ca);
			} catch (Exception e) {
				CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to create folder or update registry for CA "+ca.getName(), e)); //$NON-NLS-1$
			}
		}
	}

	private void cleanStorage(List<ConservationArea> caList, List<CyberTrackerProperties> propList) {
		if (caList == null || propList == null)
			return;
		Map<byte[], CyberTrackerProperties> propMap = new HashMap<byte[], CyberTrackerProperties>();
		for (CyberTrackerProperties ctp : propList) {
			propMap.put(ctp.getConservationArea().getUuid(), ctp);
		}
		
		for (ConservationArea ca : caList) {
			CyberTrackerProperties ctp = propMap.get(ca.getUuid());
			File storageDir = PdaUtil.getStorageFolder(ca);
			int dayLimit = (ctp != null) ? ctp.getStorageTime() : CyberTrackerProperties.STORAGE_TIME_DEFAULT_VALUE;
			cleanStorage(storageDir, dayLimit);
		}
	}

	private void cleanStorage(File folder, long dayLimit) {
		if (!folder.exists())
			return;
		long current = new Date().getTime();
		long bound = dayLimit * 24 * 60 * 60 * 1000;
		for (File file : folder.listFiles()) {
			if (current - file.lastModified() > bound) {
				FileUtils.deleteQuietly(file);
			}
		}
	}
	
}
