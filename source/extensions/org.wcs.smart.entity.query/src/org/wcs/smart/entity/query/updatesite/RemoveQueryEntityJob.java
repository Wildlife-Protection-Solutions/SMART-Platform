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
package org.wcs.smart.entity.query.updatesite;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all entity plug-in related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveQueryEntityJob extends Job {

	private static String[] SQL = new String[]{
		"DELETE FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.entity_waypoint_query)", //$NON-NLS-1$
		"DELETE FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.entity_summary_query)", //$NON-NLS-1$
		"DELETE FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.entity_gridded_query)", //$NON-NLS-1$
		"DELETE FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.entity_observation_query)" //$NON-NLS-1$
	};
	
	private static String[] TABLES = new String[]{
		"ENTITY_WAYPOINT_QUERY", //$NON-NLS-1$
		"ENTITY_SUMMARY_QUERY", //$NON-NLS-1$
		"ENTITY_GRIDDED_QUERY", //$NON-NLS-1$
		"ENTITY_OBSERVATION_QUERY", //$NON-NLS-1$
	};
	
	public RemoveQueryEntityJob() {
		super(Messages.RemoveQueryEntityJob_JobName);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		return dropTables();
	}

	
	private IStatus dropTables(){
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			for (int i = 0; i < SQL.length; i ++){
				if (SQL[i].equals("")) continue; //$NON-NLS-1$
				
				if (DerbyHibernateExtensions.tableExists(session, TABLES[i])){
					session.createSQLQuery(SQL[i]).executeUpdate();
				}
			}
			
			for (int i = 0; i < TABLES.length; i ++){
				if (DerbyHibernateExtensions.tableExists(session, TABLES[i])){
					session.createSQLQuery("DROP TABLE SMART."+ TABLES[i]).executeUpdate(); //$NON-NLS-1$
				}
			}
			session.getTransaction().commit();
		} catch (final Exception e) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					SmartPlugIn.displayLog(null, Messages.RemoveQueryEntityJob_Error1, e);
				}
			});
			return new Status(IStatus.ERROR, EntityPlugIn.PLUGIN_ID, 1, Messages.RemoveQueryEntityJob_Error2 + e.getLocalizedMessage(), e); 
		} finally {
			if (session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
			session.close();
		}
		return Status.OK_STATUS;
	}
}

