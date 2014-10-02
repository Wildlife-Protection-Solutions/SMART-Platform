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
package org.wcs.smart.er.query.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all Ecological Records plug-in related tabled from the database
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class RemoveERQueryJob extends Job {

	private String[] LABELTABLES = new String[]{
			"SURVEY_OBSERVATION_QUERY", //$NON-NLS-1$
			"SURVEY_GRIDDED_QUERY", //$NON-NLS-1$
			"SURVEY_SUMMARY_QUERY", //$NON-NLS-1$
			"SURVEY_MISSION_QUERY", //$NON-NLS-1$
			"SURVEY_MISSION_TRACK_QUERY", //$NON-NLS-1$
			"SURVEY_WAYPOINT_QUERY" //$NON-NLS-1$

	};
	
	public RemoveERQueryJob() {
		super(Messages.RemoveERQueryJob_UninstallJobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//drop tables
		final Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			for (String table : LABELTABLES){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart." + table + ")").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			for (String table : LABELTABLES){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
				}
			}		
			
			HibernateManager.setPlugInVersion(ERQueryPlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();

		} catch (Exception e) {
			try{
				session.getTransaction().rollback();
			}catch (Exception ex){
				ERQueryPlugIn.log(ex.getMessage(), ex);	
			}
			ERQueryPlugIn.displayLog(Messages.RemoveERQueryJob_UninstallError, e);
			return new Status(Status.ERROR,ERQueryPlugIn.PLUGIN_ID,e.getMessage());
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				ERQueryPlugIn.log(ex.getMessage(), ex);
			}
		}

		return Status.OK_STATUS;
	}

}
