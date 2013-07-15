package org.wcs.smart.cybertracker;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.osgi.framework.BundleContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;

/**
 * The activator class controls the plug-in life cycle
 */
public class CyberTrackerPlugIn extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.wcs.smart.cybertracker"; //$NON-NLS-1$

	
	//image registry key for cybertracker dialog image
	public static final String CT_WIZARD_BANNER = "org.wcs.smart.cybertracker.wizban"; //$NON-NLS-1$
	
	// The shared instance
	private static CyberTrackerPlugIn plugin;
	
	/**
	 * The constructor
	 */
	public CyberTrackerPlugIn() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		buildTables();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CyberTrackerPlugIn getDefault() {
		return plugin;
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
	     reg.put(CT_WIZARD_BANNER, imageDescriptorFromPlugin(PLUGIN_ID, "images/wizban/cybertracker.png")); //$NON-NLS-1$
	}
	
	public static void displayInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

	public static void displayError(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}
	
	/**
	 * Ensures that required tables are present in database and add them is case they are not present
	 */
	private void buildTables() {
		Job j = new Job(Messages.CyberTrackerPlugIn_InitJob_Title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				//check is required table exists
				try {
					session.beginTransaction();
					String sql = "select count(*) from SYS.SYSTABLES tbl inner join SYS.SYSSCHEMAS sch on tbl.SCHEMAID = sch.SCHEMAID AND sch.SCHEMANAME = 'SMART' WHERE tbl.TABLETYPE = 'T' AND tbl.TABLENAME = 'CYBERTRACKER_PROPERTIES'"; //$NON-NLS-1$
					SQLQuery q = session.createSQLQuery(sql);
					Integer result = (Integer) q.uniqueResult();
					if (result > 0)
						return Status.OK_STATUS; //required table exists
				} catch (Exception e) {
			        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, "Failed to obtain information about CyberTracker plugin tables.", e)); //$NON-NLS-1$
					e.printStackTrace();
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
							String createSql = "CREATE TABLE smart.cybertracker_properties ("+ //$NON-NLS-1$
									"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
									"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
									"storage_time INTEGER, "+ //$NON-NLS-1$
									"large_scroll_bars BOOLEAN, "+ //$NON-NLS-1$
									"auto_next BOOLEAN, "+ //$NON-NLS-1$
									"application_name VARCHAR(256), "+ //$NON-NLS-1$
									"kiosk_mode BOOLEAN, "+ //$NON-NLS-1$
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
									"ON DELETE RESTRICT"; //$NON-NLS-1$

							c.createStatement().execute(createSql);
							c.createStatement().execute(alterSql);
							
							c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to data_entry"); //$NON-NLS-1$
							c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to manager"); //$NON-NLS-1$
							c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.cybertracker_properties to analyst"); //$NON-NLS-1$
						}
					});
					session.getTransaction().commit();
				} catch (Exception ex) {
			        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, "Failed to create CyberTracker plugin tables.", ex)); //$NON-NLS-1$
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
				return Status.OK_STATUS;
			}
		};
		j.setRule(SmartPlugIn.PLUGIN_START_MUTEX);
		j.schedule();
	}
}
