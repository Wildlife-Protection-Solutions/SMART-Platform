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
package org.wcs.smart.i2.plugin;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * Plan upgrade operations while upgrade/restore backup.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligenceDatabaseUpgrader implements IDatabaseUpgrader {

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.IntelligenceDatabaseUpgrader_JobName, 1);
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			Map<String, String> versions = UpgradeEngine.getVersions(session);
			if (versions == null) throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
			String currentPluginVersion = versions.get(Intelligence2PlugIn.PLUGIN_ID);
			
			if (currentPluginVersion == null) {
				monitor.subTask(Messages.IntelligenceDatabaseUpgrader_TaskName);
				(new AddIntelligenceJob()).installPlugin(session);
			}else{
				upgrade(currentPluginVersion, session);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}
		monitor.done();
	}
	
	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session is active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
//		if (currentVersion.equals(Intelligence2PlugIn.DB_VERSION_1)){
//			upgradeV1ToV2(session);
//		}
	}
	
	

}
