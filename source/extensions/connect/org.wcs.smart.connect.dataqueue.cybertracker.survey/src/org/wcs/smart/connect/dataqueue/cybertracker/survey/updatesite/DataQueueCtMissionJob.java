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
package org.wcs.smart.connect.dataqueue.cybertracker.survey.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.PlugIn;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all Connect for CyberTracker plug-in related tabled from the database
 * 
 * @author egouge
 * 
 */
public class DataQueueCtMissionJob extends Job {
	
	private static String[] TABLES = new String[]{
		"ct_mission_link", //$NON-NLS-1$
	};
	
	public DataQueueCtMissionJob() {
		super(Messages.DataQueueCtMissionJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		return dropTables();
	}

	
	private IStatus dropTables(){
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();

			//drop all tables
			for (int i = 0; i < TABLES.length; i ++){
				if (DerbyHibernateExtensions.tableExists(session, TABLES[i])){
					session.createSQLQuery("DROP TABLE SMART."+ TABLES[i]).executeUpdate(); //$NON-NLS-1$
				}
			}
			
			//clear version
			HibernateManager.setPlugInVersion(PlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();
		} catch (final Exception e) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(Messages.DataQueueCtMissionJob_ErrorMsg, e);
				}
			});
			return new Status(IStatus.ERROR, PlugIn.PLUGIN_ID, 1, "Error uninstalling Cybertracker Connect DataQueue Processor " + e.getLocalizedMessage(), e);  //$NON-NLS-1$
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		return Status.OK_STATUS;
	}
}
