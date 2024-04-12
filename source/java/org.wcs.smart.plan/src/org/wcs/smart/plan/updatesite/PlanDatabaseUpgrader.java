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

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Plan upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class PlanDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public String getPluginId() {
		return SmartPlanPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(SmartPlanPlugIn.getDefault().getBundle());
	}
	
	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(SmartPlanPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(SmartPlanPlugIn.PLUGIN_ID).equals(SmartPlanPlugIn.DB_VERSION);
	}
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		try(Session session = HibernateManager.openSession()) {
			try{
				session.beginTransaction();
				
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(SmartPlanPlugIn.PLUGIN_ID), session);
				
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
	 * @param session is active transaction
	 */
	public void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1ToV2(session);
		}else if (currentVersion.equals(SmartPlanPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private void upgradeV1ToV2(Session session){

		String[] sql = new String[]{
			"ALTER TABLE SMART.PATROL_PLAN DROP CONSTRAINT PATROL_PLAN_PATROL_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PATROL_PLAN DROP CONSTRAINT PATROL_PLAN_PLAN_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_CA_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_CREATOR_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_PARENT_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_STATION_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN_TARGET_POINT DROP CONSTRAINT PLAN_TARGET_POINT_PLAN_TARGET_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_TEAM_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN_TARGET DROP CONSTRAINT TARGET_PLAN_UUID_FK", //$NON-NLS-1$
			"ALTER TABLE SMART.PATROL_PLAN ADD CONSTRAINT PATROL_PLAN_PATROL_UUID_FK FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
			"ALTER TABLE SMART.PATROL_PLAN ADD CONSTRAINT PATROL_PLAN_PLAN_UUID_FK FOREIGN KEY (PLAN_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_STATION_UUID_FK FOREIGN KEY (STATION_UUID) REFERENCES SMART.STATION(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",  //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_TEAM_UUID_FK FOREIGN KEY (TEAM_UUID) REFERENCES SMART.TEAM(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_PARENT_UUID_FK FOREIGN KEY (PARENT_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN_TARGET ADD CONSTRAINT TARGET_PLAN_UUID_FK FOREIGN KEY (PLAN_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE SMART.PLAN_TARGET_POINT ADD CONSTRAINT PLAN_TARGET_POINT_PLAN_TARGET_UUID_FK FOREIGN KEY (PLAN_TARGET_UUID) REFERENCES SMART.PLAN_TARGET(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"GRANT ALL PRIVILEGES ON smart.patrol_plan to analyst", //$NON-NLS-1$
		};
		for (String s : sql){
			session.createNativeMutationQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, SmartPlanPlugIn.DB_VERSION_2, session);
	}


	
	
	private void createTables(Session session){
		
		String[] sql = new String[] {
				"CREATE TABLE smart.plan (uuid CHAR(16) FOR BIT DATA NOT NULL, id VARCHAR(32) NOT NULL, start_date DATE NOT NULL, end_date DATE, type VARCHAR(32) NOT NULL, description VARCHAR(256), ca_uuid CHAR(16) FOR BIT DATA NOT NULL, station_uuid CHAR(16) FOR BIT DATA, team_uuid CHAR(16) FOR BIT DATA, active_employees INTEGER, unavailable_employees INTEGER, PARENT_UUID CHAR(16) for bit data, creator_uuid CHAR(16) FOR BIT DATA, comment LONG VARCHAR, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.plan ADD CONSTRAINT plan_ca_uuid_fk FOREIGN KEY (CA_UUID) REFERENCES smart.conservation_area(UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.plan ADD CONSTRAINT plan_station_uuid_fk FOREIGN KEY (STATION_UUID) REFERENCES smart.station (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.plan ADD CONSTRAINT plan_team_uuid_fk FOREIGN KEY (TEAM_UUID) REFERENCES smart.team (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.plan ADD CONSTRAINT plan_parent_uuid_fk FOREIGN KEY (PARENT_UUID) REFERENCES smart.plan (UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"ALTER TABLE smart.plan ADD CONSTRAINT plan_creator_uuid_fk FOREIGN KEY (creator_uuid) REFERENCES smart.employee (uuid) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan to manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.plan to analyst", //$NON-NLS-1$
				
				
				"CREATE TABLE smart.plan_target (uuid CHAR(16) FOR BIT DATA, name VARCHAR(32) NOT NULL, description VARCHAR(256), value double, op VARCHAR(10), type VARCHAR(32), plan_uuid CHAR(16) FOR BIT DATA NOT NULL, category varchar(16) NOT NULL, completed boolean NOT NULL Default false, success_distance INTEGER, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.plan_target ADD CONSTRAINT target_plan_uuid_fk FOREIGN KEY (PLAN_UUID) REFERENCES smart.plan (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan_target to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan_target to manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.plan_target to analyst", //$NON-NLS-1$
		
				"CREATE TABLE smart.patrol_plan (patrol_uuid char(16) for bit data, plan_uuid char(16) for bit data, PRIMARY KEY (patrol_uuid, plan_uuid))", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_plan ADD CONSTRAINT patrol_plan_patrol_uuid_fk FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"ALTER TABLE smart.patrol_plan ADD CONSTRAINT patrol_plan_plan_uuid_fk FOREIGN KEY (PLAN_UUID) REFERENCES SMART.PLAN (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.patrol_plan to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.patrol_plan to manager", //$NON-NLS-1$
				"GRANT SELECT ON smart.patrol_plan to analyst", //$NON-NLS-1$
				
				"CREATE TABLE smart.plan_target_point (UUID CHAR(16) for bit data NOT NULL, PLAN_TARGET_UUID CHAR(16) for bit data  NOT NULL, X DOUBLE NOT NULL, Y DOUBLE NOT NULL, PRIMARY KEY (UUID))", //$NON-NLS-1$
				"ALTER TABLE smart.plan_target_point ADD CONSTRAINT plan_target_point_plan_target_uuid_fk FOREIGN KEY (PLAN_TARGET_UUID) REFERENCES smart.plan_target(UUID) ON UPDATE RESTRICT ON DELETE CASCADE", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan_target_point to data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan_target_point to manager", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.plan_target_point to analyst" //$NON-NLS-1$
		};

		for (String s : sql){
			session.createNativeMutationQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, SmartPlanPlugIn.DB_VERSION_1, session);
	}
	
	
}
