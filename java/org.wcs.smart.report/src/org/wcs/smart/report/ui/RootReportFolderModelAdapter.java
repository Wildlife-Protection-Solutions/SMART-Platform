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
package org.wcs.smart.report.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Adapter for RootReportFolder
 * @author egouge
 * @since 1.0.0
 */
public class RootReportFolderModelAdapter implements IDeferredWorkbenchAdapter{
	
	public static final RootReportFolderModelAdapter adapter = new RootReportFolderModelAdapter();
	
	private RootReportFolderModelAdapter (){
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}



	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		return null;
	}



	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		return null;
	}



	/**
	 * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#fetchDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IElementCollector, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void fetchDeferredChildren(Object object,
			IElementCollector collector, IProgressMonitor monitor) {
		
		Session s = HibernateManager.openSession();
		try{
			
			//get kid folders
			RootReportFolder root = ((RootReportFolder)object);
			List<Object> kids = new ArrayList<Object>();
			if (root.isShared()){
				List<?> kidFolders = s.createCriteria(ReportFolder.class).add(Restrictions.isNull("parentFolder")).add(Restrictions.isNull("employee")).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
				kids.addAll(kidFolders);
				List<?> kidQueries = s.createCriteria(Report.class).add(Restrictions.isNull("folder")).add(Restrictions.eq("shared", true)).add(Restrictions.eq("owner", SmartDB.getCurrentEmployee())).list();
				kids.addAll(kidQueries);
			}else{
				List<?> kidFolders = s.createCriteria(ReportFolder.class).add(Restrictions.isNull("parentFolder")).add(Restrictions.eq("employee", SmartDB.getCurrentEmployee())).add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list();
				kids.addAll(kidFolders);
				List<?> kidQueries = s.createCriteria(Report.class).add(Restrictions.isNull("folder")).add(Restrictions.eq("shared", false)).add(Restrictions.eq("owner", SmartDB.getCurrentEmployee())).list();
				kids.addAll(kidQueries);
			}
			collector.add(kids.toArray(), monitor);
			
		}finally{
			s.close();
		}
		
	}

	/**
	 * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#isContainer()
	 */
	@Override
	public boolean isContainer() {
		return true;
	}



	/**
	 * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#getRule(java.lang.Object)
	 */
	@Override
	public ISchedulingRule getRule(Object object) {
		return null;
	}
}
