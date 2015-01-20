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
package org.wcs.smart.intelligence.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.Work;
import org.hibernate.type.BinaryType;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Upgrade database from empty state to version 3.0
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class IntelligenceDbUpgrader0To30 implements IIntelligenceUpgrader {
	
	private static final String RESULT_VERSION = "3.1"; //$NON-NLS-1$

	@Override
	public boolean upgrade(Session session, IProgressMonitor monitor) {
		//check is required table exists & create
		//currently there is no upgrade required
		final IntelligenceTablesMarkers mark = new IntelligenceTablesMarkers();
		try {
			session.beginTransaction();
			mark.intelligence = DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE"); //$NON-NLS-1$
			mark.intelligence_source = DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE_SOURCE"); //$NON-NLS-1$
			mark.intelligence_point = DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE_POINT"); //$NON-NLS-1$
			mark.intelligence_attachment = DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE_ATTACHMENT"); //$NON-NLS-1$
			mark.patrol_intelligence = DerbyHibernateExtensions.tableExists(session, "PATROL_INTELLIGENCE"); //$NON-NLS-1$
		} catch (final Exception e) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(null, Messages.AddIntelligenceJob_Error, e);
				}
			});
			return false;
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		}
		
		try {
			session.beginTransaction();
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
			HibernateManager.setPlugInVersion(IntelligencePlugIn.PLUGIN_ID, RESULT_VERSION, session);

			session.getTransaction().commit();
			
		} catch (final Exception ex) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(null, Messages.AddIntelligenceJob_Error, ex);
				}
			});
			return false;
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		}
		return true;
	}

	private void createIntelligenceTable(Session session) {
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
	}

	private void createIntelligenceSourceTable(Session session) {
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
		session.flush();

		List<ConservationArea> areas = HibernateManager.getConservationAreas(session);
		UUIDGenerator uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
		Properties prop = new Properties();
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
		prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
		uuidGenerator.configure(new BinaryType(), prop, null);
		for (ConservationArea ca : areas) {
			createSource(session, ca, "Patrol", "patrol", uuidGenerator); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(session, ca, "Public", "public", uuidGenerator); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(session, ca, "Informant", "informant", uuidGenerator); //$NON-NLS-1$ //$NON-NLS-2$
			createSource(session, ca, "CET", "cet", uuidGenerator); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	private void createIntelligencePointTable(Session session) {
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
	}

	private void createIntelligenceAttachementTable(Session session) {
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
	}

	private void createPatrolIntelligenceTable(Session session) {
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
	}

	private void createSource(Session s, final ConservationArea ca, final String name, final String keyId, UUIDGenerator uuidGenerator) {
		final byte[] uuid = (byte[]) uuidGenerator.generate((SessionImplementor) s, name);
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				PreparedStatement pst = c.prepareStatement("INSERT INTO smart.intelligence_source (UUID, CA_UUID, KEYID, IS_ACTIVE) VALUES (?, ?, ?, ?)"); //$NON-NLS-1$
				pst.setBytes(1, uuid);
				pst.setBytes(2, ca.getUuid());
				pst.setString(3, keyId);
				pst.setBoolean(4, true);
				pst.execute();
				
				pst = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)"); //$NON-NLS-1$
				pst.setBytes(1, ca.getDefaultLanguage().getUuid());
				pst.setBytes(2, uuid);
				pst.setString(3, name);
				pst.execute();
			}
		});
	}
	
	private class IntelligenceTablesMarkers {
		public boolean intelligence = false;
		public boolean intelligence_source = false;
		public boolean intelligence_point = false;
		public boolean intelligence_attachment = false;
		public boolean patrol_intelligence = false;
	}
	
}
