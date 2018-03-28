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
package org.wcs.smart.event.plugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all event related tabled from the database
 * 
 * @author Emily
 */
public class RemoveEventJob extends Job {

	public RemoveEventJob() {
		super(Messages.RemoveEventJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				uninstall(session);
				session.getTransaction().commit();
			} catch (Exception e) {
				try{
					session.getTransaction().rollback();
				}catch (Exception ex){
					EventPlugIn.log(ex.getMessage(), ex);	
				}
				EventPlugIn.displayLog(Messages.RemoveEventJob_UninstallError + e.getMessage(), e);
				return new Status(Status.ERROR, EventPlugIn.PLUGIN_ID, e.getMessage());
			}
		}	
		return Status.OK_STATUS;
	}

	private void uninstall(Session session){
		
		String[] TABLES = new String[]{
				"smart.e_event_action", //$NON-NLS-1$
				"smart.e_action_parameter_value", //$NON-NLS-1$
				"smart.e_action", //$NON-NLS-1$
				"smart.e_event_filter" //$NON-NLS-1$
		};
		
		String[] LABELTABLES = new String[]{
		};
		
		//drop tables
		for (String table : LABELTABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
			
		for (String table : TABLES){
			if (DerbyHibernateExtensions.tableExists(session, table)){
				session.createNativeQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
			}
		}		

		//remove from plugin table
		HibernateManager.setPlugInVersion(EventPlugIn.PLUGIN_ID, null, session);

	}
}
