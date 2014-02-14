package org.wcs.smart.cybertracker.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

public class AddCyberTrackerJob extends Job {

	public AddCyberTrackerJob() {
		super(Messages.AddCyberTrackerJob_InstallCtJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		buildTables();
		return Status.OK_STATUS;
	}
	
	/**
	 * Ensures that required tables are present in database and add them is case they are not present
	 */
	private void buildTables() {
		final boolean tables[] = {false, false}; //properties table
		
		//check is required table exists
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			tables[0] = DerbyHibernateExtensions.tableExists(session, "CYBERTRACKER_PROPERTIES"); //$NON-NLS-1$
			tables[1] = DerbyHibernateExtensions.tableExists(session, "CT_PROPERTIES_OPTION"); //$NON-NLS-1$
		} catch (Exception e) {
			session.getTransaction().rollback();
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to obtain information about CyberTracker plugin tables.", e)); //$NON-NLS-1$
		} finally {
			session.close();
		}

		//this must be run as Admin User
		//AND only admin users should be able to install plugins in the first place.
		//so we shouldn't need to do this (if we bring this back we need to make sure
		//to reconnect back to the correct user)
		//HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			
			if (tables[0]) {
				//old table present, need to drop it
				String dropSql = "DROP TABLE smart.cybertracker_properties"; //$NON-NLS-1$
				session.createSQLQuery(dropSql).executeUpdate();
			}
								
			if (!tables[1]) {
				String createSql = "CREATE TABLE smart.ct_properties_option ("+ //$NON-NLS-1$
						"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
						"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
						"OPTION_ID VARCHAR(32) NOT NULL, "+ //$NON-NLS-1$
						"DOUBLE_VALUE DOUBLE, "+ //$NON-NLS-1$
						"INTEGER_VALUE INTEGER, "+ //$NON-NLS-1$
						"STRING_VALUE VARCHAR(1024), "+ //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterSql = "ALTER TABLE smart.ct_properties_option "+ //$NON-NLS-1$
						"ADD CONSTRAINT ct_properties_option_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				session.createSQLQuery(createSql).executeUpdate();
				session.createSQLQuery(alterSql).executeUpdate();
				
						
				session.createSQLQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to data_entry").executeUpdate(); //$NON-NLS-1$
				session.createSQLQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to manager").executeUpdate(); //$NON-NLS-1$
				session.createSQLQuery("GRANT ALL PRIVILEGES ON smart.ct_properties_option to analyst").executeUpdate(); //$NON-NLS-1$
				session.createSQLQuery("GRANT SELECT ON smart.ct_properties_option to login").executeUpdate(); //$NON-NLS-1$
			}
			HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION, session);
			session.getTransaction().commit();
		} catch (Exception ex) {
			session.getTransaction().rollback();
			CyberTrackerPlugIn.getDefault().getLog().log(new Status(IStatus.ERROR, CyberTrackerPlugIn.PLUGIN_ID, IStatus.OK, "Failed to create CyberTracker plugin tables.", ex)); //$NON-NLS-1$
		} finally {
			session.close();
		}
	}
}
