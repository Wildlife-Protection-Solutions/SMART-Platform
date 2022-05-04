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
package org.wcs.smart.entity.query.updatesite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.upgrade.v400.Upgrader331To400;

/**
 * Entity query upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * @since 3.1.0
 */
public class EntityQueryDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return EntityQueryPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(EntityQueryPlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(EntityQueryPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(EntityQueryPlugIn.PLUGIN_ID).equals(EntityQueryPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.EntityQueryDatabaseUpgrader_UpgradeTask);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(EntityQueryPlugIn.PLUGIN_ID), session);
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session in active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
			upgradeV3ToV4(session);
		}else if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
			upgradeV3ToV4(session);
		}else if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_2)){
			upgradeV2ToV3(session);
			upgradeV3ToV4(session);
		}else if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_3)){
			upgradeV3ToV4(session);
		}
	}
	
	private void upgradeV1ToV2(Session session){
		String[] sql = new String[]{
				"alter table smart.entity_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_gridded_query add column style long varchar"}; //$NON-NLS-1$
		
		
		for (String s : sql){
			EntityQueryPlugIn.log(s, null);
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_2, session);
	}
	
	private void upgradeV2ToV3(final Session session){
		String[] sql = new String[]{
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITY_OBSERVATION_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITY_OBSERVATION_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITY_WAYPOINT_QUERY_CA_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITY_WAYPOINT_QUERY_FOLDER_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITYOBSERVATION_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITYWAYPOINT_QUERY_CREATOR_UUID_FK", //$NON-NLS-1$
				
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITYOBSERVATION_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITYWAYPOINT_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE"	 //$NON-NLS-1$
				}; 
		
		for (String s : sql){
			EntityQueryPlugIn.log(s, null);
			session.createNativeQuery(s).executeUpdate();
		}
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				upgradeCcaaQueriesAndReports(session,connection);
			}});
		
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_3, session);
	}
	
	private void upgradeCcaaQueriesAndReports(Session s, Connection c) throws SQLException{
		// --- Queries  ---
		//we only worry about the query tables we know about; plugins are responsible for their query tables
		String[] queryTables = new String[]{
				"smart.ENTITY_SUMMARY_QUERY", //$NON-NLS-1$
				"smart.ENTITY_WAYPOINT_QUERY", //$NON-NLS-1$
				"smart.ENTITY_OBSERVATION_QUERY", //$NON-NLS-1$
				"smart.ENTITY_GRIDDED_QUERY" //$NON-NLS-1$
		};
		Upgrader331To400.updateCCAAQueryTables(s, c, queryTables);
		
	}
	
	private void upgradeV3ToV4(Session session){
		String[] sql = new String[]{
	            // #1366: Deactivate columns without values in queries
				"ALTER TABLE smart.entity_observation_query ADD COLUMN show_data_columns_only BOOLEAN", //$NON-NLS-1$ //EntityObservationQuery
		};
		
		for (String s : sql){
			EntityQueryPlugIn.log(s, null);
			session.createNativeQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_4, session);
	}
	
	
	private void createTables(Session session){
		
		String[] sql = new String[]{
				"CREATE TABLE smart.entity_waypoint_query(	UUID CHAR(16) FOR BIT DATA NOT NULL, CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL, QUERY_FILTER VARCHAR(32672), CA_FILTER VARCHAR(32672), CA_UUID CHAR(16) FOR BIT DATA NOT NULL, FOLDER_UUID CHAR(16) FOR BIT DATA, COLUMN_FILTER VARCHAR(32672), SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE smart.entity_summary_query(UUID CHAR(16) for bit data NOT NULL,CREATOR_UUID CHAR(16) for bit data NOT NULL,CA_FILTER VARCHAR(32672),QUERY_DEF VARCHAR(32672),FOLDER_UUID CHAR(16) for bit data,SHARED BOOLEAN NOT NULL,CA_UUID CHAR(16) for bit data NOT NULL,ID VARCHAR(6) NOT NULL,PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE smart.entity_gridded_query(UUID CHAR(16) for bit data NOT NULL,CREATOR_UUID CHAR(16) for bit data NOT NULL,QUERY_FILTER VARCHAR(32672),CA_FILTER VARCHAR(32672),QUERY_DEF VARCHAR(32672),FOLDER_UUID CHAR(16) for bit data,SHARED BOOLEAN NOT NULL,CA_UUID CHAR(16) for bit data NOT NULL,ID VARCHAR(6) NOT NULL,CRS_DEFINITION VARCHAR(32672) NOT NULL,PRIMARY KEY (UUID))", //$NON-NLS-1$
				"CREATE TABLE smart.entity_observation_query (UUID CHAR(16) FOR BIT DATA NOT NULL,CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL,QUERY_FILTER VARCHAR(32672),CA_FILTER VARCHAR(32672),CA_UUID CHAR(16) FOR BIT DATA NOT NULL,FOLDER_UUID CHAR(16) FOR BIT DATA,COLUMN_FILTER VARCHAR(32672),SHARED BOOLEAN DEFAULT false NOT NULL,ID VARCHAR(6) NOT NULL,PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.entity_waypoint_query ADD constraint entitywaypoint_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_waypoint_query ADD constraint entity_waypoint_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_waypoint_query ADD constraint entity_waypoint_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_observation_query ADD constraint entityobservation_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_observation_query ADD constraint entity_observation_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_observation_query ADD constraint entity_observation_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_summary_query ADD constraint entity_summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID) REFERENCES smart.employee (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.entity_gridded_query ADD constraint entity_gridded_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID) REFERENCES smart.query_folder (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_observation_query to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_waypoint_query to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_summary_query to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_gridded_query to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_observation_query to analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_waypoint_query to analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_summary_query to analyst", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.entity_gridded_query to analyst" //$NON-NLS-1$
			};
		
		//check is required table exists		
		for (int i = 0; i < sql.length; i ++){
			EntityQueryPlugIn.log(sql[i], null);
			session.createNativeQuery(sql[i]).executeUpdate();
		}
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_1, session);
	}
}
