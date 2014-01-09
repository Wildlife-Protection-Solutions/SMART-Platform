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
package org.wcs.smart.intelligence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB.DbUser;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.IntelligenceSource;

/**
 * Updates database to support user defined intelligence source types.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligenceStartupJob extends Job {

	public IntelligenceStartupJob() {
		super(Messages.IntelligenceStartupJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		checkIntelligenceSource();
		return Status.OK_STATUS;
	}

	private void checkIntelligenceSource() {
		final boolean tables[] = {false}; //intelligence_source
		Session session = HibernateManager.openSession();
		//check if required table exists
		try {
			session.beginTransaction();
			if (!DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE")) { //$NON-NLS-1$
				//there is no intelligence tables in database
				//this is likely if plug-in is being installed
				//this means that no changes are required
				return;
			}
			tables[0] = DerbyHibernateExtensions.tableExists(session, "INTELLIGENCE_SOURCE"); //$NON-NLS-1$
			if (tables[0])
				return; //required table exists
		} catch (Exception e) {
			IntelligencePlugIn.displayLog("Failed to obtain information about intelligence_source tables.", e); //$NON-NLS-1$
			return;
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
					String createSql = "CREATE TABLE smart.intelligence_source ("+ //$NON-NLS-1$
							"uuid CHAR(16) for bit data NOT NULL, "+ //$NON-NLS-1$
							"ca_uuid CHAR(16) for bit data  NOT NULL, "+ //$NON-NLS-1$
							"KEYID varchar(128), "+ //$NON-NLS-1$
							"IS_ACTIVE BOOLEAN NOT NULL, "+ //$NON-NLS-1$
							"PRIMARY KEY (UUID))"; //$NON-NLS-1$

					String alterSourceSql = "ALTER TABLE smart.intelligence_source "+ //$NON-NLS-1$
							"ADD CONSTRAINT intelligence_source_ca_uuid_fk FOREIGN KEY (CA_UUID) "+ //$NON-NLS-1$
							"REFERENCES smart.conservation_area(UUID) "+ //$NON-NLS-1$
							"ON UPDATE RESTRICT "+ //$NON-NLS-1$
							"ON DELETE RESTRICT"; //$NON-NLS-1$

					String alterIntelColSql = "ALTER TABLE smart.intelligence ADD COLUMN source_uuid CHAR(16) FOR BIT DATA"; //$NON-NLS-1$

					c.createStatement().execute(createSql);
					c.createStatement().execute(alterSourceSql);
					c.createStatement().execute(alterIntelColSql);
					
					c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to data_entry"); //$NON-NLS-1$
					c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to manager"); //$NON-NLS-1$
					c.createStatement().execute("GRANT SELECT ON smart.intelligence_source to analyst"); //$NON-NLS-1$
				}
			});
			session.flush();
			
			List<ConservationArea> areas = HibernateManager.getConservationAreas(session);
			for (ConservationArea ca : areas) {
				createSource(session, ca, "Patrol", "patrol"); //$NON-NLS-1$ //$NON-NLS-2$
				createSource(session, ca, "Public", "public"); //$NON-NLS-1$ //$NON-NLS-2$
				createSource(session, ca, "Informant", "informant"); //$NON-NLS-1$ //$NON-NLS-2$
				createSource(session, ca, "CET", "cet"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			session.getTransaction().commit();

			session.beginTransaction();
			session.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					String alterIntelColFkSql = "ALTER TABLE smart.intelligence "+ //$NON-NLS-1$
							"ADD CONSTRAINT intelligence_source_uuid_fk FOREIGN KEY (source_uuid) "+ //$NON-NLS-1$
							"REFERENCES smart.intelligence_source (uuid) "+ //$NON-NLS-1$
							"ON UPDATE RESTRICT "+ //$NON-NLS-1$
							"ON DELETE RESTRICT"; //$NON-NLS-1$

					String dropOldSourceColSql = "alter table smart.INTELLIGENCE drop column SOURCE"; //$NON-NLS-1$
					
					c.createStatement().execute(alterIntelColFkSql);
					c.createStatement().execute(dropOldSourceColSql);
				}
			});
			session.getTransaction().commit();
		} catch (Exception ex) {
			IntelligencePlugIn.log("Failed to create inteligence source database records.", ex); //$NON-NLS-1$
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

	
	private IntelligenceSource createSource(Session s, final ConservationArea ca, String name, final String keyId) {
		final IntelligenceSource source = new IntelligenceSource();
		source.setConservationArea(ca);
		source.setKeyId(keyId);
		source.updateName(ca.getDefaultLanguage(), name);
		source.setIsActive(true);
		
		s.save(source);
		
		s.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				PreparedStatement pst = c.prepareStatement("UPDATE smart.intelligence SET source_uuid = ? where ca_uuid = ? and source = ?"); //$NON-NLS-1$
				pst.setBytes(1, source.getUuid());
				pst.setBytes(2, ca.getUuid());
				pst.setString(3, keyId.toUpperCase());
				pst.executeUpdate();
			}
		});
		return source;
	}
}
