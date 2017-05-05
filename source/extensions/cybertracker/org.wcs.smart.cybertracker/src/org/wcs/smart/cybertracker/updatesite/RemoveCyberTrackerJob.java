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
package org.wcs.smart.cybertracker.updatesite;

import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Job removes all CyberTracker related tabled from the database and files from filestore
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemoveCyberTrackerJob extends Job {

	public RemoveCyberTrackerJob() {
		super(Messages.RemoveCyberTrackerTablesJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		String[] tables = new String[] {
			"CM_CT_PROPERTIES_PROFILE",  //$NON-NLS-1$
			"CT_PROPERTIES_OPTION",  //$NON-NLS-1$
			"CT_PROPERTIES_PROFILE_OPTION",  //$NON-NLS-1$
			"CT_PROPERTIES_PROFILE" }; //$NON-NLS-1$
		
		final Session session = HibernateManager.openSession();
		final List<ConservationArea> caList = HibernateManager.getConservationAreas(session);
		session.beginTransaction();
		try {
			//delete labels
			if (DerbyHibernateExtensions.tableExists(session, "CT_PROPERTIES_PROFILE")){ //$NON-NLS-1$
				session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.CT_PROPERTIES_PROFILE)").executeUpdate(); //$NON-NLS-1$
			}
			//delete tables
			for (String table : tables){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
				}
			}		
			//clean filestore
			for (ConservationArea ca : caList) {
				FileUtils.deleteDirectory(PdaUtil.getDowloadFolder(ca));
			}
			HibernateManager.setPlugInVersion(CyberTrackerPlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();
			
		} catch (Exception e) {
			SmartPlugIn.log(Messages.RemoveCyberTrackerTablesJob_Error, e);
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
		}
		return Status.OK_STATUS;
	}
	
}
