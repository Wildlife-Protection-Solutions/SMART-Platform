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
package org.wcs.smart.report.manger;

import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSourceHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.birt.ui.RCPMultiPageReportEditor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.designer.SmartReportPerspective;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.ReportQuery;
import org.wcs.smart.report.ui.SmartReportEditorInput;
import org.wcs.smart.util.SmartUtils;

/**
 * A report manager.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportManager {
	
	public static final String SMART_DATASOURCE_ID = "org.wcs.smart.data.oda.smart"; //$NON-NLS-1$
	public static final String SMART_DATASET_TYPE = "org.wcs.smart.data.oda.smart.smartQueryDataset"; //$NON-NLS-1$
	public static final String SMART_DATASET_TABLE_TYPE = "org.wcs.smart.data.oda.smart.smartTableDataset"; //$NON-NLS-1$

	
	/**
	 * Deletes a report folder from the database.
	 * @param folder the folder to delete
	 * @throws Exception if folder cannot be deleted
	 */
	public static void deleteReportFolder(ReportFolder folder) throws Exception{
		Session session = HibernateManager.openSession();
		try{
			session.load(folder, folder.getUuid());
			//report folders can only be removed if they don't have any children
			session.beginTransaction();
			Query q = session.createQuery("SELECT count(*) FROM ReportFolder WHERE parentFolder = :parent"); //$NON-NLS-1$
			q.setParameter("parent", folder); //$NON-NLS-1$
			Long cnt = (Long) q.list().get(0);
			if (cnt > 0){
				throw new Exception(Messages.ReportManager_Error_ChildFoldersExist);
			}
			q = session.createQuery("SELECT count(*) FROM Report WHERE folder = :parent"); //$NON-NLS-1$
			q.setParameter("parent", folder); //$NON-NLS-1$
			cnt = (Long) q.list().get(0);
			if (cnt > 0){
				throw new Exception(Messages.ReportManager_Error_ChildReportsExist);
			}
			
			folder.setDeletedParent(folder.getParentFolder());
			if (folder.getParentFolder() != null){
				session.update(folder.getParentFolder());
				folder.getParentFolder().getChildren().remove(folder);
				folder.setParentFolder(null);
			}else{
				session.delete(folder);
			}
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			session.close();
		}
	}
	
	/**
	 * Deletes a report from the database and filestore.
	 * @param report the report to delete
	 * @throws Exception if report cannot be deleted
	 */
	public static void deleteReport(final Report report) throws Exception{
		
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			session.delete(report);
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			session.close();
		}	
		if (!report.getFullReportFilename().exists()){
			throw new Exception(Messages.ReportManager_Deleteok_ReportFileNotFound + report.getFullReportFilename().toString());
		}
		try{
			if (!report.getFullReportFilename().delete()){
				throw new Exception(Messages.ReportManager_Deleteok_ReportFileNotRemoved + report.getFullReportFilename().toString());
			}
		}catch (Exception ex){
			throw new Exception(Messages.ReportManager_Deleteok_ReportFileNotRemovedB + ex.getLocalizedMessage(), ex);
		}	
	}
	
	/**
	 * Computes the next report id.
	 * @param session current hibernate session
	 * @return the next report id
	 * @throws Exception
	 */
	public static String generateReportId(Session session) throws Exception{
		String newId = "000001"; //$NON-NLS-1$
		Query q = session.createQuery("SELECT max(id) FROM Report WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		Object maxid = q.list().get(0);
		if (maxid != null){
			int x = Integer.parseInt(maxid.toString());
			x++;
			if (x > 999999){
				x = 1;
			}
			DecimalFormat df = new DecimalFormat("000000"); //$NON-NLS-1$
			newId = df.format(x);
		}
		return newId;
	}
	
	/**
	 * Generates the report file name from the report name.
	 * @param r the report 
	 * @return the new filename
	 * @throws Exception if cannot determine a filename for the report
	 */
	public synchronized static String generateFilename(Report r) throws Exception{
		File dir = ReportPlugIn.getReportDirectory(r.getConservationArea());

		String fname = r.getId().replaceAll("[^\\p{Ll}\\p{Lu}\\p{Lt}\\p{Nd}]", ""); //$NON-NLS-1$ //$NON-NLS-2$  letters and digits
		String suffix =  ".rptdesign"; //$NON-NLS-1$
		
		int cnt = 0;
		File f = new File(dir, fname+suffix);
		while(f.exists()){
			cnt ++;
			if (cnt > 1000){
				throw new Exception(Messages.ReportManager_GeneratingFileError);
			}
			f = new File(dir, fname + "_" + cnt + suffix); //$NON-NLS-1$
		}
		return f.getName();
	}
	
	/**
	 * Opens the given report in the edit perspective
	 * @param r report to edit
	 */
	public static void editReport(Report r){
		try {
			
			if (r != null) {
				ReportPlugIn.initReports();
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				window.getWorkbench().showPerspective(SmartReportPerspective.ID,window);
				
				ReportEditorInput ri = new SmartReportEditorInput(r);
				
				IEditorPart part = window.getActivePage().findEditor(ri);
				if (part == null){
					//not yet open so lets refresh before we open
					refreshReport(r);	
				}
				IEditorPart editor = window.getActivePage().openEditor(
								ri,
								IReportEditorContants.DESIGN_EDITOR_ID);
				if (editor instanceof RCPMultiPageReportEditor){
					((RCPMultiPageReportEditor) editor).refreshMarkers(editor.getEditorInput());
				}
				
			}
		} catch (Exception ex) {
			ReportPlugIn.displayLog(
					Messages.ReportManager_ReportOk_OpenEerror
							+ ex.getLocalizedMessage(), ex);
		}
	}
	
	
	public static void refreshReport(Report report) throws Exception{
		//create report file with default library
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(report.getFullReportFilename().getAbsolutePath());
		
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset.getDataSource() != null && ((OdaDataSourceHandle)dataset.getDataSource()).getExtensionID().equals(SMART_DATASOURCE_ID)){
				//refresh the columns in the query
				try{
					DataSetUIUtil.updateColumnCache(dataset);
				}catch (Exception ex){
					//eat me - could not update column cache
				}
			
				//for now we are not updating any references to the query columns
				
			}
			
		}
		rdh.save();
		rdh.close();
	}
	
	/**
	 * Opens the given report the view perspective and runs the report.
	 * @param report
	 */
	public static void viewReport(final Report report){
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				try {
					IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ReportView.ID,SmartUtils.encodeHex(report.getUuid()),IWorkbenchPage.VIEW_ACTIVATE );
					((ReportView)view).setReport(report);
				} catch (PartInitException e) {
					ReportPlugIn.displayLog(MessageFormat.format(Messages.ReportManager_Error_OpeningReport, new Object[]{report.getName()}) + e.getLocalizedMessage(), e);
				}				
			}});
	}
	
	/**
	 * Opens the given report the view perspective and run the report using
	 * the parameters provided.
	 * @param report
	 */
	public static void viewReport(final Report report, final HashMap<String, Object> reportParameters){
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				try {
					IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ReportView.ID,SmartUtils.encodeHex(report.getUuid()),IWorkbenchPage.VIEW_ACTIVATE );
					((ReportView)view).setReport(report, reportParameters);
				} catch (PartInitException e) {
					ReportPlugIn.displayLog(MessageFormat.format(Messages.ReportManager_Error_OpeningReportA, new Object[]{report.getName()}) + e.getLocalizedMessage(), e);
				}				
			}});
	}
	
	
	/**
	 * Updates the database report query table for the given
	 * report.  Determines all the smart queries used in the 
	 * report and updates the smart.report_query table.
	 * 
	 * @param s open database session with active transaction
	 * @param rdh report design handler
	 * @param r report associated with design handler
	 * @throws Exception
	 */
	public static void updateReportQueries(Session s, ReportDesignHandle rdh, Report r) throws Exception{
		List<?> datasets = rdh.getDataSets().getContents();
		
		Set<ReportQuery> reportQueries = new HashSet<ReportQuery>();		
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				OdaDataSetHandle h = (OdaDataSetHandle)dataset;
				if (h.getExtensionID().equals(SMART_DATASET_TYPE)){
					reportQueries.add(new ReportQuery(r, SmartUtils.decodeHex(h.getQueryText().split(":")[1]))); //$NON-NLS-1$
				}
			}
		}
		Query q = s.createQuery("delete ReportQuery where id.report=:report"); //$NON-NLS-1$
		q.setParameter("report", r); //$NON-NLS-1$
		q.executeUpdate();
		
		for (ReportQuery rq: reportQueries){
			s.saveOrUpdate(rq);
		}
	}

}
