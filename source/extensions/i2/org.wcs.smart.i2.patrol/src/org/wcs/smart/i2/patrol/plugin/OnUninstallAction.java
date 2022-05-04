/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.i2.patrol.plugin;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.p2.common.updatesite.UninstallProvisioningAction;

/**
 * Action that is called when r plug-in is uninstalled
 * 
 * @author Emily
 */
public class OnUninstallAction extends UninstallProvisioningAction {

	@Override
	protected String getPluginId() {
		return PatrolProfilePlugIn.PLUGIN_ID;
	}
	
	@Override
	protected void performRemove() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				uninstall(session);
				session.getTransaction().commit();
			} catch (Exception e) {
				try{
					session.getTransaction().rollback();
				}catch (Exception ex){
					PatrolProfilePlugIn.log(ex.getMessage(), ex);	
				}
				PatrolProfilePlugIn.displayLog(MessageFormat.format(Messages.RemovePluginJob_Error, e.getMessage()), e);
				return;
			}
		}	
	}
	
	
	private void uninstall(Session session){
		//to drop and the label tables (objects that extend NamedItem or NamedKeyItem)
		String[] TABLES = new String[]{	
			"i_patrol_record_motivation", //$NON-NLS-1$
		};

		//drop the tables
		for (String table : TABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
			}
		}		

		//remove from plugin table
		HibernateManager.setPlugInVersion(PatrolProfilePlugIn.PLUGIN_ID, null, session);
	}
}