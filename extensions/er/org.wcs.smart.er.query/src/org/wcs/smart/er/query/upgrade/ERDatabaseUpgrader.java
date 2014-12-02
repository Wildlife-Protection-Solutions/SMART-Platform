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
package org.wcs.smart.er.query.upgrade;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.updatesite.OnInstallAction;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Ecological Records Query upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ERDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(Session s, IProgressMonitor monitor) {
		Map<String, String> versions = UpgradeEngine.getVersions(s);
		if (versions == null) {
			//we don't know what is happening with database
			//it is some kind of error or wrong database version
			return;
		}
		if (versions.get(ERQueryPlugIn.PLUGIN_ID) == null) {
			//Ecological Records Query isn't present in this configuration
			//we need to perform install database to support the plug-in
			monitor.subTask(Messages.ERDatabaseUpgrader_Info);
			OnInstallAction install = new OnInstallAction();
			install.execute(null);
		}
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(ERQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session){
		String[] sql = new String[]{
				"alter table smart.survey_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_mission_track_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_mission_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.survey_gridded_query add column style long varchar"}; //$NON-NLS-1$
		
		session.beginTransaction();
		try{
			for (String s : sql){
				ERQueryPlugIn.log(s, null);
				session.createSQLQuery(s).executeUpdate();
			}
		
			HibernateManager.setPlugInVersion(ERQueryPlugIn.PLUGIN_ID, ERQueryPlugIn.DB_VERSION_2, session);
			session.getTransaction().commit();
		}finally{
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}
	}

}
