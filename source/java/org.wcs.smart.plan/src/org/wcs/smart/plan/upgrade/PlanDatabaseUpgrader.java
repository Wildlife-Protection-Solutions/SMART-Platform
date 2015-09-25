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
package org.wcs.smart.plan.upgrade;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.updatesite.OnInstallAction;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Plan upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class PlanDatabaseUpgrader implements IDatabaseUpgrader {

	/* (non-Javadoc)
	 * @see org.wcs.smart.upgrade.IDatabaseUpgrader#upgrade(org.hibernate.Session, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void upgrade(IProgressMonitor monitor) {

		String currentPluginVersion = null;
		
		Session s = HibernateManager.openSession();
		try{
			Map<String, String> versions = UpgradeEngine.getVersions(s);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			currentPluginVersion = versions.get(SmartPlanPlugIn.PLUGIN_ID);
		}finally{
			s.close();
		}
		
		if (currentPluginVersion == null) {
			//Entity doesn't present in this configuration
			//we need to perform install database support for the plug-in
			monitor.subTask(Messages.PlanDatabaseUpgrader_UpgradeTask);
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
				final String msg = MessageFormat.format(Messages.PlanDatabaseUpgrader_upgradeError, new Object[]{currentPluginVersion, SmartPlanPlugIn.DB_VERSION}) + " \n\n" + t.getMessage(); //$NON-NLS-1$
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						SmartPlanPlugIn.displayLog(msg, t);
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
	 * @param session is active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(SmartPlanPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session){
		@SuppressWarnings("nls")
		String[] sql = new String[]{
			"ALTER TABLE SMART.PATROL_PLAN DROP CONSTRAINT PATROL_PLAN_PATROL_UUID_FK",
			"ALTER TABLE SMART.PATROL_PLAN DROP CONSTRAINT PATROL_PLAN_PLAN_UUID_FK",
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_CA_UUID_FK",
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_CREATOR_UUID_FK",
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_PARENT_UUID_FK",
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_STATION_UUID_FK",
			"ALTER TABLE SMART.PLAN_TARGET_POINT DROP CONSTRAINT PLAN_TARGET_POINT_PLAN_TARGET_UUID_FK",
			"ALTER TABLE SMART.PLAN DROP CONSTRAINT PLAN_TEAM_UUID_FK",
			"ALTER TABLE SMART.PLAN_TARGET DROP CONSTRAINT TARGET_PLAN_UUID_FK",
			"ALTER TABLE SMART.PATROL_PLAN ADD CONSTRAINT PATROL_PLAN_PATROL_UUID_FK FOREIGN KEY (PATROL_UUID) REFERENCES SMART.PATROL(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
			"ALTER TABLE SMART.PATROL_PLAN ADD CONSTRAINT PATROL_PLAN_PLAN_UUID_FK FOREIGN KEY (PLAN_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_CA_UUID_FK FOREIGN KEY (CA_UUID) REFERENCES SMART.CONSERVATION_AREA(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_STATION_UUID_FK FOREIGN KEY (STATION_UUID) REFERENCES SMART.STATION(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", 
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_TEAM_UUID_FK FOREIGN KEY (TEAM_UUID) REFERENCES SMART.TEAM(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_PARENT_UUID_FK FOREIGN KEY (PARENT_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.PLAN ADD CONSTRAINT PLAN_CREATOR_UUID_FK FOREIGN KEY (CREATOR_UUID) REFERENCES SMART.EMPLOYEE(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.PLAN_TARGET ADD CONSTRAINT TARGET_PLAN_UUID_FK FOREIGN KEY (PLAN_UUID) REFERENCES SMART.PLAN(UUID)  ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE",
			"ALTER TABLE SMART.PLAN_TARGET_POINT ADD CONSTRAINT PLAN_TARGET_POINT_PLAN_TARGET_UUID_FK FOREIGN KEY (PLAN_TARGET_UUID) REFERENCES SMART.PLAN_TARGET(UUID)  ON DELETE CASCADE ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE" 
		};
		for (String s : sql){
			session.createSQLQuery(s).executeUpdate();
		}
		HibernateManager.setPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, SmartPlanPlugIn.DB_VERSION_2, session);
	}

}
