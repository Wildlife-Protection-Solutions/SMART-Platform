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
package org.wcs.smart.smartcollect.updatesite;

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;

/**
 * Action that is called when plug-in is uninstalled
 * 
 * @author Emily
 * @since 7.0.0
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	private static String[] TABLES = new String[]{
			"smartcollect_package", //$NON-NLS-1$
			"smartcollect_waypoint", //$NON-NLS-1$
		};
		
	@Override
	protected String getPluginId() {
		return SmartCollectPlugIn.PLUGIN_ID;
	}
	
	@Override
	protected void performRemove() {
		try(Session session = HibernateManager.openSession()){
			
			session.beginTransaction();
			try {
				//drop all tables
				for (int i = 0; i < TABLES.length; i ++){
					if (DerbyHibernateExtensions.tableExists(session, TABLES[i])){
						session.createNativeMutationQuery("DROP TABLE smart."+ TABLES[i]).executeUpdate(); //$NON-NLS-1$
					}
					
					//delete all waypoints with source of smartcollect
					session.createMutationQuery("DELETE FROM Waypoint WHERE source = :source") //$NON-NLS-1$
						.setParameter("source", SmartCollectWaypointSource.KEY).executeUpdate(); //$NON-NLS-1$
				}
				
				//clear version
				HibernateManager.setPlugInVersion(SmartCollectPlugIn.PLUGIN_ID, null, session);
				session.getTransaction().commit();
			} catch (final Exception e) {
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						SmartPlugIn.displayLog(Messages.RemoveSmartCollectJob_UninstallError + e.getMessage(), e);
					}
				});
				return ;
			} finally {
				if (session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			}
		}
	}

	

}