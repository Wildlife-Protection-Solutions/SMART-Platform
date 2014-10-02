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
package org.wcs.smart.plan.updatesite;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.DerbyHibernateExtensions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;

/**
 * Job removes all plan related tabled from the database
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class RemovePlanJob extends Job {

	private static final String[] TABLES = new String[]{
		"patrol_plan",  //$NON-NLS-1$
		"plan_target_point",  //$NON-NLS-1$
		"plan_target",  //$NON-NLS-1$
		"plan"}; //$NON-NLS-1$
	
	public RemovePlanJob() {
		super(Messages.RemovePlanJob_Title);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		List<ConservationArea> cas = null;
		
		final Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			cas = HibernateManager.getConservationAreas(session);
			if (DerbyHibernateExtensions.tableExists(session, "plan")){ //$NON-NLS-1$
				session.createSQLQuery("delete FROM smart.I18N_LABEL where ELEMENT_UUID in (select uuid from smart.plan)").executeUpdate(); //$NON-NLS-1$
			}
			for (String table : TABLES){
				if (DerbyHibernateExtensions.tableExists(session, table)){
					session.createSQLQuery("DROP TABLE SMART." + table).executeUpdate(); //$NON-NLS-1$
				}
			}		
			HibernateManager.setPlugInVersion(SmartPlanPlugIn.PLUGIN_ID, null, session);
			session.getTransaction().commit();

		} catch (Exception e) {
			try{
				session.getTransaction().rollback();
			}catch (Exception ex){
				SmartPlanPlugIn.log(ex.getMessage(), ex);	
			}
			SmartPlanPlugIn.displayLog(Messages.RemovePlanJob_Error, e);
			return new Status(Status.ERROR,SmartPlanPlugIn.PLUGIN_ID,e.getMessage());
		} finally {
			try {
				session.close();
			} catch (Exception ex) {
				SmartPlanPlugIn.log(ex.getMessage(), ex);
			}
		}
		if (cas != null){
			//delete filestores
			for (ConservationArea ca : cas){
				try {
					File folder = new File(ca.getFileDataStoreLocation() + File.separator + SmartPlanPlugIn.PLAN_DIR);
					FileUtils.deleteDirectory(folder);
				} catch (IOException ex) {
					SmartPlanPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
		return Status.OK_STATUS;
	}

}
