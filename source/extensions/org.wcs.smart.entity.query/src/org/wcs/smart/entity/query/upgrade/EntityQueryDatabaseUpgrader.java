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
package org.wcs.smart.entity.query.upgrade;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.updatesite.OnInstallAction;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Entity query upgrade operations while upgrade/restore backup.
 * 
 * @author egouge
 * @since 3.1.0
 */
public class EntityQueryDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) {
		String currentPluginVersion = null;
		
		Session s = HibernateManager.openSession();
		try{
			Map<String, String> versions = UpgradeEngine.getVersions(s);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			currentPluginVersion = versions.get(EntityQueryPlugIn.PLUGIN_ID);
		}finally{
			s.close();
		}
		
		if (currentPluginVersion == null) {
			//Entity doesn't present in this configuration
			//we need to perform install database support for the plug-in
			monitor.subTask(Messages.EntityQueryDatabaseUpgrader_UpgradeTask);
			OnInstallAction install = new OnInstallAction();
			install.execute(null);
		
		}else{
			s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				upgrade(currentPluginVersion, s);
				s.getTransaction().commit();
			}catch (final Throwable t){
				if (s.getTransaction().isActive()) s.getTransaction().rollback();
				final String msg = MessageFormat.format(Messages.EntityQueryDatabaseUpgrader_QueryUpdateError, new Object[]{currentPluginVersion, EntityQueryPlugIn.DB_VERSION}) + " \n\n" + t.getMessage(); //$NON-NLS-1$
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						EntityQueryPlugIn.displayLog(msg, t);
					}
				});
			}finally{
				s.close();
			}
		}
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
			upgradeV2ToV3(session);
		}else if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_2)){
			upgradeV2ToV3(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session){
		String[] sql = new String[]{
				"alter table smart.entity_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_gridded_query add column style long varchar"}; //$NON-NLS-1$
		
		
		for (String s : sql){
			EntityQueryPlugIn.log(s, null);
			session.createSQLQuery(s).executeUpdate();
		}
		
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_2, session);
	}
	
	private static void upgradeV2ToV3(Session session){
		@SuppressWarnings("nls")
		String[] sql = new String[]{
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_CREATOR_UUID_FK",
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY DROP CONSTRAINT ENTITY_GRIDDED_QUERY_FOLDER_UUID_FK",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITY_OBSERVATION_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITY_OBSERVATION_QUERY_FOLDER_UUID_FK",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_CREATOR_UUID_FK",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY DROP CONSTRAINT ENTITY_SUMMARY_QUERY_FOLDER_UUID_FK",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITY_WAYPOINT_QUERY_CA_UUID_FK",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITY_WAYPOINT_QUERY_FOLDER_UUID_FK",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY DROP CONSTRAINT ENTITYOBSERVATION_QUERY_CREATOR_UUID_FK",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY DROP CONSTRAINT ENTITYWAYPOINT_QUERY_CREATOR_UUID_FK",
				
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_GRIDDED_QUERY ADD CONSTRAINT ENTITY_GRIDDED_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITYOBSERVATION_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_OBSERVATION_QUERY ADD CONSTRAINT ENTITY_OBSERVATION_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_SUMMARY_QUERY ADD CONSTRAINT ENTITY_SUMMARY_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITYWAYPOINT_QUERY_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_FOLDER_UUID_FK FOREIGN KEY (FOLDER_UUID) REFERENCES SMART.QUERY_FOLDER(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
				"ALTER TABLE SMART.ENTITY_WAYPOINT_QUERY ADD CONSTRAINT ENTITY_WAYPOINT_QUERY_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE"	
				}; 
		
		for (String s : sql){
			EntityQueryPlugIn.log(s, null);
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_3, session);
	}
}
