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
package org.wcs.smart.cybertracker.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.Work;
import org.hibernate.type.BinaryType;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Performs upgrade for CyberTracker from plugin version 3.0 to 4.0
 *
 * @author elitvin
 * @since 4.0.0
 */
public class CtDatabaseUpgrader30To40 {
	
	private UUIDGenerator uuidGenerator;
	
	public void upgrade(Session session){
		uuidGenerator = null;
		@SuppressWarnings("nls")
		String[] sql = new String[]{
			"ALTER TABLE SMART.CT_PROPERTIES_OPTION DROP CONSTRAINT CT_PROPERTIES_OPTION_CA_UUID_FK",
			"ALTER TABLE SMART.CT_PROPERTIES_OPTION ADD CONSTRAINT CT_PROPERTIES_OPTION_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			
			"CREATE TABLE smart.ct_properties_profile (uuid CHAR(16) for bit data NOT NULL, ca_uuid CHAR(16) for bit data NOT NULL, IS_DEFAULT BOOLEAN, PRIMARY KEY (UUID))",
			"ALTER TABLE smart.ct_properties_profile ADD CONSTRAINT CT_PROPERTIES_PROFILE_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile to data_entry",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile to manager",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile to analyst",
			"GRANT SELECT ON smart.ct_properties_profile to login",

			"CREATE TABLE smart.ct_properties_profile_option (uuid CHAR(16) for bit data NOT NULL, profile_uuid CHAR(16) for bit data  NOT NULL, OPTION_ID VARCHAR(32) NOT NULL, DOUBLE_VALUE DOUBLE, INTEGER_VALUE INTEGER, STRING_VALUE VARCHAR(1024), PRIMARY KEY (UUID))",
			"ALTER TABLE smart.ct_properties_profile_option ADD CONSTRAINT CT_PROPERTIES_PROFILE_OPTION_PROFILE_UUID_FK FOREIGN KEY (profile_uuid) REFERENCES smart.ct_properties_profile(UUID) ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile_option to data_entry",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile_option to manager",
			"GRANT ALL PRIVILEGES ON smart.ct_properties_profile_option to analyst",
			"GRANT SELECT ON smart.ct_properties_profile_option to login"
		};
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		populateProfiles(session);
		HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, CyberTrackerPlugIn.DB_VERSION_4_0, session);
	}

	private void populateProfiles(final Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				//NOTE: STORAGE_TIME is a global option, it is unique per CA
				PreparedStatement ps_ca = c.prepareStatement("select DISTINCT CA_UUID from smart.CT_PROPERTIES_OPTION where OPTION_ID <> 'STORAGE_TIME'"); //$NON-NLS-1$
				PreparedStatement ps_lang = c.prepareStatement("select UUID from smart.LANGUAGE where ISDEFAULT = true and CA_UUID = ?"); //$NON-NLS-1$
				PreparedStatement insert_profile_ps = c.prepareStatement("insert into smart.ct_properties_profile (UUID, CA_UUID, IS_DEFAULT) VALUES (?, ?, true)"); //$NON-NLS-1$
				PreparedStatement insert_label_ps = c.prepareStatement("INSERT INTO smart.I18N_LABEL (LANGUAGE_UUID, ELEMENT_UUID, VALUE) VALUES (?, ?, ?)"); //$NON-NLS-1$
				PreparedStatement insert_profile_options_ps = c.prepareStatement("INSERT INTO SMART.CT_PROPERTIES_PROFILE_OPTION (UUID, PROFILE_UUID, OPTION_ID, DOUBLE_VALUE, INTEGER_VALUE, STRING_VALUE) SELECT UUID, ?, OPTION_ID, DOUBLE_VALUE, INTEGER_VALUE, STRING_VALUE FROM SMART.CT_PROPERTIES_OPTION where OPTION_ID <> 'STORAGE_TIME' AND CA_UUID = ?"); //$NON-NLS-1$
				PreparedStatement delete_old_options_ps = c.prepareStatement("DELETE FROM SMART.CT_PROPERTIES_OPTION where OPTION_ID <> 'STORAGE_TIME' AND CA_UUID = ?"); //$NON-NLS-1$
				try (ResultSet ca_rs = ps_ca.executeQuery()) {
					while (ca_rs.next()) {
						byte[] ca_uuid = ca_rs.getBytes(1);
						byte[] p_uuid = getNewUuid(session, ca_uuid);
						
						insert_profile_ps.setBytes(1, p_uuid);
						insert_profile_ps.setBytes(2, ca_uuid);
						insert_profile_ps.execute();

						insert_profile_options_ps.setBytes(1, p_uuid);
						insert_profile_options_ps.setBytes(2, ca_uuid);
						insert_profile_options_ps.execute();
						
						delete_old_options_ps.setBytes(1, ca_uuid);
						delete_old_options_ps.execute();
						
						ps_lang.setBytes(1, ca_uuid);
						try (ResultSet lang_rs = ps_lang.executeQuery()) {
							if (lang_rs.next()) {
								byte[] lang_uuid = lang_rs.getBytes(1);
								
								insert_label_ps.setBytes(1, lang_uuid);
								insert_label_ps.setBytes(2, p_uuid);
								insert_label_ps.setString(3, CyberTrackerHibernateManager.DEFAULT_PROFILE_NAME);
								insert_label_ps.execute();
							}
						}
						
					}
				}
				
			}
		});
	}
	
	private byte[] getNewUuid(Session session, Object object) {
		if (uuidGenerator == null) {
			uuidGenerator = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();
			Properties prop = new Properties();
			prop.put(UUIDGenerator.UUID_GEN_STRATEGY, StandardRandomStrategy.INSTANCE);
			prop.put(UUIDGenerator.UUID_GEN_STRATEGY_CLASS, UUIDGenerationStrategy.class.getName());
			uuidGenerator.configure(new BinaryType(), prop, null);
		}

		byte[] uuid = (byte[]) uuidGenerator.generate((SessionImplementor) session, object);
		return uuid;
	}
	
}
