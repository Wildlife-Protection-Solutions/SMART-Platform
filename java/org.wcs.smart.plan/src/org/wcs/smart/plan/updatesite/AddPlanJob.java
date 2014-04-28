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
package org.wcs.smart.plan.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.DisplayAccess;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;

public class AddPlanJob extends Job {

	public AddPlanJob() {
		super(Messages.AddPlanJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//required if run during restore to ensure Display.syncexec calls don't block
		DisplayAccess.accessDisplayDuringStartup();
				
		final PlanTablesMarkers mark = new PlanTablesMarkers();
		Session session = HibernateManager.openSession();
		try{
			String currentVersion = HibernateManager.getPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, session);
			if (currentVersion != null && currentVersion.equals(SmartPlanPlugIn.DB_VERSION)){
				//db version matches current version; we are ok
				return Status.OK_STATUS;
			}
			
			//check is required table exists
			try {
				session.beginTransaction();
				mark.plan = DerbyHibernateExtensions.tableExists(session, "PLAN"); //$NON-NLS-1$
				mark.plan_target = DerbyHibernateExtensions.tableExists(session, "PLAN_TARGET"); //$NON-NLS-1$
				mark.plan_target_point = DerbyHibernateExtensions.tableExists(session, "PLAN_TARGET_POINT"); //$NON-NLS-1$
				mark.patrol_plan = DerbyHibernateExtensions.tableExists(session, "PATROL_PLAN"); //$NON-NLS-1$
			} catch (final Exception e) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						SmartPlanPlugIn.displayLog(Messages.AddPlanJob_Error, e);
					}
				});
				return new Status(IStatus.ERROR, SmartPlanPlugIn.PLUGIN_ID, 1, Messages.AddPlanJob_Error, e); 
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		}
		}finally{
			session.close();
		}

		// need to login as admin user to create tables
		//this must be run as Admin User
		//AND only admin users should be able to install plugins in the first place.
		//so we shouldn't need to do this
		//HibernateManager.setUserName(DbUser.ADMIN.getUserName(), DbUser.ADMIN.getPassword());
		session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			if (!mark.plan) {
				createPlanTable(session);
			}
			if (!mark.plan_target) {
				createPlanTargetTable(session);
			}
			if (!mark.plan_target_point) {
				createPlanTargetPointTable(session);
			}
			if (!mark.patrol_plan) {
				createPatrolPlanTable(session);
			}			
			HibernateManager.setPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, SmartPlanPlugIn.DB_VERSION, session);
			session.getTransaction().commit();
		} catch (final Exception ex) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlanPlugIn.displayLog(Messages.AddPlanJob_Error, ex);
				}
			});
			return new Status(IStatus.ERROR, SmartPlanPlugIn.PLUGIN_ID, 1, Messages.AddPlanJob_Error, ex); 
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			if (session.isOpen()) {
				session.close();
			}
		}

		return Status.OK_STATUS;
	}

	private void createPlanTable(Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.plan (" + //$NON-NLS-1$
						"uuid CHAR(16) FOR BIT DATA NOT NULL, " + //$NON-NLS-1$
						"id VARCHAR(32) NOT NULL, " + //$NON-NLS-1$
						"start_date DATE NOT NULL, " + //$NON-NLS-1$
						"end_date DATE, " + //$NON-NLS-1$
						"type VARCHAR(32) NOT NULL, " + //$NON-NLS-1$
						"description VARCHAR(256), " + //$NON-NLS-1$
						"ca_uuid CHAR(16) FOR BIT DATA NOT NULL, " + //$NON-NLS-1$
						"station_uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"team_uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"active_employees INTEGER, " + //$NON-NLS-1$
						"unavailable_employees INTEGER, " + //$NON-NLS-1$
						"PARENT_UUID CHAR(16) for bit data, " + //$NON-NLS-1$
						"creator_uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"comment LONG VARCHAR, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterCaSql = "ALTER TABLE smart.plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				String alterStationSql = "ALTER TABLE smart.plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_station_uuid_fk FOREIGN KEY (STATION_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.station (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$
				
				String alterTeamSql = "ALTER TABLE smart.plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_team_uuid_fk FOREIGN KEY (TEAM_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.team (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				String alterParentSql = "ALTER TABLE smart.plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_parent_uuid_fk FOREIGN KEY (PARENT_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.plan (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				String alterCreatorSql = "ALTER TABLE smart.plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_creator_uuid_fk FOREIGN KEY (creator_uuid) "+ //$NON-NLS-1$
						"REFERENCES smart.employee (uuid) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$
				
				c.createStatement().execute(createSql);
				c.createStatement().execute(alterCaSql);
				c.createStatement().execute(alterStationSql);
				c.createStatement().execute(alterTeamSql);
				c.createStatement().execute(alterParentSql);
				c.createStatement().execute(alterCreatorSql);
				
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.plan to analyst"); //$NON-NLS-1$
			}
		});
	}

	private void createPlanTargetTable(Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.plan_target (" + //$NON-NLS-1$
						"uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"name VARCHAR(32) NOT NULL, " + //$NON-NLS-1$
						"description VARCHAR(256), " + //$NON-NLS-1$
						"value double, " + //$NON-NLS-1$
						"op VARCHAR(10), " + //$NON-NLS-1$
						"type VARCHAR(32), " + //$NON-NLS-1$
						"plan_uuid CHAR(16) FOR BIT DATA NOT NULL, " + //$NON-NLS-1$
						"category varchar(16) NOT NULL, " + //$NON-NLS-1$
						"completed boolean NOT NULL Default false, " + //$NON-NLS-1$
						"success_distance INTEGER, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterPlanSql = "ALTER TABLE smart.plan_target "+ //$NON-NLS-1$
						"ADD CONSTRAINT target_plan_uuid_fk FOREIGN KEY (PLAN_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.plan (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				c.createStatement().execute(createSql);
				c.createStatement().execute(alterPlanSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan_target to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan_target to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.plan_target to analyst"); //$NON-NLS-1$
			}
		});
	}

	private void createPatrolPlanTable(Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.patrol_plan (" + //$NON-NLS-1$
						"patrol_uuid char(16) for bit data, " + //$NON-NLS-1$
						"plan_uuid char(16) for bit data, " + //$NON-NLS-1$
						"PRIMARY KEY (patrol_uuid, plan_uuid))"; //$NON-NLS-1$

				String alterPatrolSql = "ALTER TABLE smart.patrol_plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT patrol_plan_patrol_uuid_fk FOREIGN KEY (PATROL_UUID) "+ //$NON-NLS-1$
						"REFERENCES SMART.PATROL (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				String alterPlanSql = "ALTER TABLE smart.patrol_plan "+ //$NON-NLS-1$
						"ADD CONSTRAINT patrol_plan_plan_uuid_fk FOREIGN KEY (PLAN_UUID) "+ //$NON-NLS-1$
						"REFERENCES SMART.PLAN (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$
				
				c.createStatement().execute(createSql);
				c.createStatement().execute(alterPatrolSql);
				c.createStatement().execute(alterPlanSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.patrol_plan to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.patrol_plan to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.patrol_plan to analyst"); //$NON-NLS-1$
			}
		});
		
	}

	private void createPlanTargetPointTable(Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.plan_target_point (" + //$NON-NLS-1$
						"UUID CHAR(16) for bit data NOT NULL, " + //$NON-NLS-1$
						"PLAN_TARGET_UUID CHAR(16) for bit data  NOT NULL, " + //$NON-NLS-1$
						"X DOUBLE NOT NULL, " + //$NON-NLS-1$
						"Y DOUBLE NOT NULL, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterPlanTargetSql = "ALTER TABLE smart.plan_target_point "+ //$NON-NLS-1$
						"ADD CONSTRAINT plan_target_point_plan_target_uuid_fk FOREIGN KEY (PLAN_TARGET_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.plan_target(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				c.createStatement().execute(createSql);
				c.createStatement().execute(alterPlanTargetSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan_target_point to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan_target_point to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.plan_target_point to analyst"); //$NON-NLS-1$
			}
		});
	}
	
	private class PlanTablesMarkers {
		public boolean plan = false;
		public boolean plan_target = false;
		public boolean plan_target_point = false;
		public boolean patrol_plan = false;
		
//		public boolean allSet() {
//			return plan && plan_target && plan_target_point && patrol_plan;
//		}
	}
}
