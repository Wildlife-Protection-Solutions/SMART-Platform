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
package org.wcs.smart.intelligence.updatesite;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;

public class AddIntelligenceJob extends Job {

	public AddIntelligenceJob() {
		super(Messages.AddIntelligenceJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		final IntelligenceTablesMarkers mark = new IntelligenceTablesMarkers();
		Session session = HibernateManager.openSession();
		//check is required table exists
		try {
			session.beginTransaction();
			mark.intelligence = DerbyHibernateExtensions.tableExists(session, "intelligence"); //$NON-NLS-1$
			mark.intelligence_source = DerbyHibernateExtensions.tableExists(session, "intelligence_source"); //$NON-NLS-1$
			mark.intelligence_point = DerbyHibernateExtensions.tableExists(session, "intelligence_point"); //$NON-NLS-1$
			mark.intelligence_attachment = DerbyHibernateExtensions.tableExists(session, "intelligence_attachment"); //$NON-NLS-1$
			mark.patrol_intelligence = DerbyHibernateExtensions.tableExists(session, "patrol_intelligence"); //$NON-NLS-1$
			
			if (mark.allSet())
				return Status.OK_STATUS; //required table exists
		} catch (Exception e) {
			SmartPlugIn.displayLog(null, Messages.AddIntelligenceJob_Error, e);
			return new Status(IStatus.ERROR, IntelligencePlugIn.PLUGIN_ID, 1, "", null); //$NON-NLS-1$
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
			if (!mark.intelligence_source) {
				createIntelligenceSourceTable(session);
			}
			if (!mark.intelligence) {
				createIntelligenceTable(session);
			}
			if (!mark.intelligence_point) {
				createIntelligencePointTable(session);
			}
			if (!mark.intelligence_attachment) {
				createIntelligenceAttachementTable(session);
			}
			if (!mark.patrol_intelligence) {
				createPatrolIntelligenceTable(session);
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(null, Messages.AddIntelligenceJob_Error, ex);
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

	private void createIntelligenceTable(Session session) {
		session.beginTransaction();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.intelligence (" + //$NON-NLS-1$
						"UUID CHAR(16) for bit data NOT NULL, " + //$NON-NLS-1$
						"CA_UUID CHAR(16) for bit data  NOT NULL, " + //$NON-NLS-1$
						"RECEIVED_DATE DATE NOT NULL, " + //$NON-NLS-1$
						"PATROL_UUID CHAR(16) for bit data, " + //$NON-NLS-1$
						"FROM_DATE DATE NOT NULL, " + //$NON-NLS-1$
						"TO_DATE DATE, " + //$NON-NLS-1$
						"DESCRIPTION LONG VARCHAR, " + //$NON-NLS-1$
						"source_uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"creator_uuid CHAR(16) FOR BIT DATA, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterCaSql = "ALTER TABLE smart.intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				String alterPatrolSql = "ALTER TABLE smart.intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_patrol_uuid_fk FOREIGN KEY (PATROL_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.patrol(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$
				
				String alterSourceSql = "ALTER TABLE smart.intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_source_uuid_fk FOREIGN KEY (source_uuid) "+ //$NON-NLS-1$
						"REFERENCES smart.intelligence_source (uuid) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				String alterCreatorSql = "ALTER TABLE smart.intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_creator_uuid_fk FOREIGN KEY (creator_uuid) "+ //$NON-NLS-1$
						"REFERENCES smart.employee (uuid) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$
				
				c.createStatement().execute(createSql);
				c.createStatement().execute(alterCaSql);
				c.createStatement().execute(alterPatrolSql);
				c.createStatement().execute(alterSourceSql);
				c.createStatement().execute(alterCreatorSql);
				
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.intelligence to analyst"); //$NON-NLS-1$
			}
		});
		session.getTransaction().commit();
	}

	private void createIntelligenceSourceTable(Session session) {
		session.beginTransaction();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.intelligence_source ("+ //$NON-NLS-1$
						"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
						"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
						"KEYID varchar(128), "+ //$NON-NLS-1$
						"IS_ACTIVE BOOLEAN NOT NULL, "+ //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterCaSql = "ALTER TABLE smart.intelligence_source "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_source_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE RESTRICT"; //$NON-NLS-1$

				c.createStatement().execute(createSql);
				c.createStatement().execute(alterCaSql);

				c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to analyst"); //$NON-NLS-1$
			}
		});
		session.getTransaction().commit();
	}

	private void createIntelligencePointTable(Session session) {
		session.beginTransaction();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.intelligence_point (" + //$NON-NLS-1$
						"UUID CHAR(16) for bit data NOT NULL, " + //$NON-NLS-1$
						"INTELLIGENCE_UUID CHAR(16) for bit data  NOT NULL, " + //$NON-NLS-1$
						"X DOUBLE NOT NULL, " + //$NON-NLS-1$
						"Y DOUBLE NOT NULL, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterIntelligenceSql = "ALTER TABLE smart.intelligence_point "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_point_intelligence_uuid_fk FOREIGN KEY (INTELLIGENCE_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.intelligence (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				c.createStatement().execute(createSql);
				c.createStatement().execute(alterIntelligenceSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence_point to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence_point to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.intelligence_point to analyst"); //$NON-NLS-1$
			}
		});
		session.getTransaction().commit();
	}

	private void createIntelligenceAttachementTable(Session session) {
		session.beginTransaction();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.intelligence_attachment (" + //$NON-NLS-1$
						"UUID CHAR(16)  for bit data  NOT NULL, " + //$NON-NLS-1$
						"INTELLIGENCE_UUID CHAR(16)  for bit data  NOT NULL, " + //$NON-NLS-1$
						"FILENAME VARCHAR(1024) NOT NULL, " + //$NON-NLS-1$
						"PRIMARY KEY (UUID))"; //$NON-NLS-1$

				String alterIntelligenceSql = "ALTER TABLE smart.intelligence_attachment "+ //$NON-NLS-1$
						"ADD CONSTRAINT intelligence_attachment_intelligence_uuid_fk FOREIGN KEY (INTELLIGENCE_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.intelligence (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				c.createStatement().execute(createSql);
				c.createStatement().execute(alterIntelligenceSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence_attachment to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.intelligence_attachment to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.intelligence_attachment to analyst"); //$NON-NLS-1$
			}
		});
		session.getTransaction().commit();
	}

	private void createPatrolIntelligenceTable(Session session) {
		session.beginTransaction();
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				String createSql = "CREATE TABLE smart.patrol_intelligence (" + //$NON-NLS-1$
						"PATROL_UUID CHAR(16)  for bit data  NOT NULL, " + //$NON-NLS-1$
						"INTELLIGENCE_UUID CHAR(16)  for bit data  NOT NULL, " + //$NON-NLS-1$
						"PRIMARY KEY (PATROL_UUID, INTELLIGENCE_UUID))"; //$NON-NLS-1$

				String alterPatrolSql = "ALTER TABLE smart.patrol_intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT patrol_intelligence_patrol_uuid_fk FOREIGN KEY (PATROL_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.patrol (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$

				String alterIntelligenceSql = "ALTER TABLE smart.patrol_intelligence "+ //$NON-NLS-1$
						"ADD CONSTRAINT patrol_intelligence_intelligence_uuid_fk FOREIGN KEY (INTELLIGENCE_UUID) "+ //$NON-NLS-1$
						"REFERENCES smart.intelligence (UUID) "+ //$NON-NLS-1$
						"ON UPDATE RESTRICT "+ //$NON-NLS-1$
						"ON DELETE CASCADE"; //$NON-NLS-1$
				
				c.createStatement().execute(createSql);
				c.createStatement().execute(alterPatrolSql);
				c.createStatement().execute(alterIntelligenceSql);

				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.patrol_intelligence to data_entry"); //$NON-NLS-1$
				c.createStatement().execute("GRANT ALL PRIVILEGES ON smart.patrol_intelligence to manager"); //$NON-NLS-1$
				c.createStatement().execute("GRANT SELECT ON smart.patrol_intelligence to analyst"); //$NON-NLS-1$
			}
		});
		session.getTransaction().commit();
	}

	private class IntelligenceTablesMarkers {
		public boolean intelligence = false;
		public boolean intelligence_source = false;
		public boolean intelligence_point = false;
		public boolean intelligence_attachment = false;
		public boolean patrol_intelligence = false;

		
		public boolean allSet() {
			return intelligence && intelligence_source && intelligence_point && intelligence_attachment && patrol_intelligence;
		}
	}
}
