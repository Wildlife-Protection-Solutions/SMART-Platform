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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.hibernate.Session;
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
		
		try(Session s = HibernateManager.openSession()){
			try{
				s.beginTransaction();
				//get kid folders
				RootReportFolder root = (RootReportFolder)object;
				List<Object> kids = new ArrayList<Object>();
				CriteriaBuilder cb = s.getCriteriaBuilder();
				
				if (root.isShared()){
					
					CriteriaQuery<ReportFolder> cfolder = cb.createQuery(ReportFolder.class);
					Root<ReportFolder> folderfrom = cfolder.from(ReportFolder.class);
					cfolder.where(cb.and(
							cb.isNull(folderfrom.get("parentFolder")), //$NON-NLS-1$
							cb.isNull(folderfrom.get("employee")), //$NON-NLS-1$
							cb.equal(folderfrom.get("conservationArea"), root.getConservationArea()) //$NON-NLS-1$
							));				
					List<?> kidFolders = s.createQuery(cfolder).getResultList();
					kids.addAll(kidFolders);
					
					CriteriaQuery<Report> creport = cb.createQuery(Report.class);
					Root<Report> reportfrom = creport.from(Report.class);
					creport.where(cb.and(
							cb.isNull(reportfrom.get("folder")), //$NON-NLS-1$
							cb.equal(reportfrom.get("shared"), true), //$NON-NLS-1$
							cb.equal(reportfrom.get("conservationArea"), root.getConservationArea()) //$NON-NLS-1$
							));									
					List<?> kidQueries = s.createQuery(creport).getResultList();
					kids.addAll(kidQueries);
				}else{
					CriteriaQuery<ReportFolder> cfolder = cb.createQuery(ReportFolder.class);
					Root<ReportFolder> folderfrom = cfolder.from(ReportFolder.class);
					cfolder.where(cb.and(
							cb.isNull(folderfrom.get("parentFolder")), //$NON-NLS-1$
							cb.equal(folderfrom.get("employee"), SmartDB.getConservationAreaConfiguration().getCcaaUser()), //$NON-NLS-1$
							cb.equal(folderfrom.get("conservationArea"), root.getConservationArea()) //$NON-NLS-1$
							));				
					List<?> kidFolders = s.createQuery(cfolder).getResultList();
					kids.addAll(kidFolders);
					
					CriteriaQuery<Report> creport = cb.createQuery(Report.class);
					Root<Report> reportfrom = creport.from(Report.class);
					creport.where(cb.and(
							cb.isNull(reportfrom.get("folder")), //$NON-NLS-1$
							cb.equal(reportfrom.get("shared"), false), //$NON-NLS-1$
							cb.equal(reportfrom.get("owner"), SmartDB.getConservationAreaConfiguration().getCcaaUser()), //$NON-NLS-1$
							cb.equal(reportfrom.get("conservationArea"), root.getConservationArea()) //$NON-NLS-1$
							));									
					List<?> kidQueries = s.createQuery(creport).getResultList();
					kids.addAll(kidQueries);
				}
				
				//ensure ca is loaded for report items
				for (Object  x : kids){
					if (x instanceof Report){
						((Report) x).getConservationArea().getId();
					}
				}
				LazyReportContentProvider.sortItems(kids);
				collector.add(kids.toArray(), monitor);
				s.getTransaction().commit();
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
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
