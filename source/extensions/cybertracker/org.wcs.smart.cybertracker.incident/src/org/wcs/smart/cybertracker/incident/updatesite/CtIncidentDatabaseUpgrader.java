/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.incident.CtIncidentPlugIn;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

/**
 * SMART CT Incident upgrade progress
 * 
 */
public class CtIncidentDatabaseUpgrader implements IDatabaseUpgrader {
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CtIncidentDatabaseUpgrader_Progress);
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try {
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				if (versions == null)
					throw new IllegalStateException("Database versions not found."); //shouldn't happy //$NON-NLS-1$
				String currentPluginVersion = versions.get(CtIncidentPlugIn.PLUGIN_ID);
	
				if (currentPluginVersion == null) {
					(new AddCtIncidentJob()).installPlugin(session);
				} else {
					upgrade(currentPluginVersion, session);
				}
				session.getTransaction().commit();
			} catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}
	
	/**
	 * 
	 * @param currentVersion
	 * @param session in active transaction
	 */
	public static final void upgrade(String currentVersion, Session session){
		
	}
	
	

}
