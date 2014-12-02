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
	public void upgrade(Session s, IProgressMonitor monitor) {
		Map<String, String> versions = UpgradeEngine.getVersions(s);
		if (versions == null) {
			//we don't know what is happening with database
			//it is some kind of error or wrong database version
			return;
		}
		final String currentVersion = versions.get(EntityQueryPlugIn.PLUGIN_ID);
		if (currentVersion == null) {
			//Entity doesn't present in this configuration
			//we need to perform install database support for the plug-in
			
			//this will install and upgrade to current version
			monitor.subTask(Messages.EntityQueryDatabaseUpgrader_UpgradeTask);
			OnInstallAction install = new OnInstallAction();
			install.execute(null);
		}else{
			try{
				upgrade(currentVersion, s);
			}catch (final Throwable t){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						EntityQueryPlugIn.displayLog(MessageFormat.format(Messages.EntityQueryDatabaseUpgrader_QueryUpdateError, new Object[]{currentVersion, EntityQueryPlugIn.DB_VERSION_2}) + " \n\n" + t.getMessage(), t); //$NON-NLS-1$
					}
				});
			}
		}
		
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session
	 */
	public static final void upgrade(String currentVersion, Session session){
		if (currentVersion.equals(EntityQueryPlugIn.DB_VERSION_1)){
			upgradeV1ToV2(session);
		}
	}
	
	private static void upgradeV1ToV2(Session session){
		String[] sql = new String[]{
				"alter table smart.entity_observation_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_waypoint_query add column style long varchar", //$NON-NLS-1$
				"alter table smart.entity_gridded_query add column style long varchar"}; //$NON-NLS-1$
		
		session.beginTransaction();
		try{
			for (String s : sql){
				EntityQueryPlugIn.log(s, null);
				session.createSQLQuery(s).executeUpdate();
			}
		
			HibernateManager.setPlugInVersion(EntityQueryPlugIn.PLUGIN_ID, EntityQueryPlugIn.DB_VERSION_2, session);
			session.getTransaction().commit();
		}finally{
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
		}
	}

}
