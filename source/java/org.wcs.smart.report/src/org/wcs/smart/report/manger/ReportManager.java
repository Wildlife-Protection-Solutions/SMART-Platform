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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.designer.RCPMultiPageReportEditor;
import org.wcs.smart.report.internal.ui.designer.SmartReportEditorInput;
import org.wcs.smart.report.internal.ui.designer.SmartReportPerspective;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.ReportQuery;
import org.wcs.smart.util.SmartUtils;

/**
 * A report manager.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportManager {

	private static IReportEngine reportEngine = null;
	public static final String SMART_DATASOURCE_ID = "org.wcs.smart.data.oda.smart";
	public static final String SMART_DATASET_TYPE = "org.wcs.smart.data.oda.smart.smartQueryDataset";
	public static final String SMART_DATASET_TABLE_TYPE = "org.wcs.smart.data.oda.smart.smartTableDataset";

	
	/**
	 * 
	 * @return the BIRT report engine
	 */
	public static IReportEngine getReportEngine(){
		if (reportEngine != null){
			return reportEngine;
		}
		IReportEngineFactory factory = (IReportEngineFactory)Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
		reportEngine = factory.createReportEngine(new EngineConfig());
		return reportEngine;
	}
	
	/**
	 * Shuts down the BIRT report engine
	 */
	public static void endReportEngine(){
		if (reportEngine != null){
			reportEngine.destroy();
			reportEngine = null;
		}
	}
	
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
			Query q = session.createQuery("SELECT count(*) FROM ReportFolder WHERE parentFolder = :parent");
			q.setParameter("parent", folder);
			Long cnt = (Long) q.list().get(0);
			if (cnt > 0){
				throw new Exception("Folder cannot be deleted until all children folders are deleted.");
			}
			q = session.createQuery("SELECT count(*) FROM Report WHERE folder = :parent");
			q.setParameter("parent", folder);
			cnt = (Long) q.list().get(0);
			if (cnt > 0){
				throw new Exception("Folder cannot be deleted until all children reports are deleted.");
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
//
//		//close any editor and view open that reference this report
//		Display.getDefault().syncExec(new Runnable(){
//
//			@Override
//			public void run() {
//				try{
//					SmartUtils.forceClose(IReportEditorContants.DESIGN_EDITOR_ID, new ReportEditorInput(new File(report.getFilename())));
//				}catch (Exception ex){
//					ReportPlugIn.log("Error closing editor on report delete.", ex);
//				}
//				try{
//					SmartUtils.forceCloseView(ReportView.ID, SmartUtils.encodeHex(report.getUuid()));
//				}catch (Exception ex){
//					ReportPlugIn.log("Error closing view on report delete.", ex);
//				}
//			}});
//

		
		
		if (!report.getFullReportFilename().exists()){
			throw new Exception("Report deleted from database but report file could not be found to remove.\n\n" + report.getFullReportFilename().toString());
		}
		try{
			if (!report.getFullReportFilename().delete()){
				throw new Exception("Report deleted from database but report file could not be deleted.  This file should be removed manually.\n\n" + report.getFullReportFilename().toString());
			}
		}catch (Exception ex){
			throw new Exception("Report deleted from database but report file could not be deleted.  This file should be removed manually.\n\n" + ex.getMessage(), ex);
		}	
	}
	
	/**
	 * Computes the next report id.
	 * @return the next report id
	 * @throws Exception
	 */
	public static String generateReportId() throws Exception{
		String newId = "000001";
		Session session = HibernateManager.openSession();
		try{
			session.beginTransaction();
			Query q = session.createQuery("SELECT max(id) FROM Report");
			Object maxid = q.list().get(0);
			if (maxid != null){
				int x = Integer.parseInt(maxid.toString());
				x++;
				if (x > 999999){
					x = 1;
				}
				DecimalFormat df = new DecimalFormat("000000");
				newId = df.format(x);
				
			}
			session.getTransaction().commit();
		}finally{
			session.close();
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
		File dir = ReportPlugIn.getReportDirectory();
		
		String fname = r.getName().replaceAll("[^a-zA-Z0-9]", "");
		fname += "_" + r.getId();
		String suffix =  ".rptdesign";
		
		int cnt = 0;
		File f = new File(dir, fname+suffix);
		while(f.exists()){
			cnt ++;
			if (cnt > 1000){
				throw new Exception("Could not generate a report file.");
			}
			f = new File(dir, fname + "_" + cnt + suffix);
		}
		if (!f.createNewFile()){
			throw new Exception("Could not generate a report file.");
		}
		return f.getName();
	}
	
	/**
	 * Opens the given report in the edit perspective
	 * @param r report to edit
	 */
	public static void editReport(Report r){
		try {
			refreshReport(r);
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			window.getWorkbench().showPerspective(SmartReportPerspective.ID,window);
			if (r != null) {
				ReportEditorInput ri = new SmartReportEditorInput(r);
				IEditorPart editor = window.getActivePage().openEditor(
								ri,
								IReportEditorContants.DESIGN_EDITOR_ID);
				if (editor instanceof RCPMultiPageReportEditor){
					//TODO: test this
					((RCPMultiPageReportEditor) editor).refreshMarkers(editor.getEditorInput());
				}
				
			}
		} catch (Exception ex) {
			ReportPlugIn.displayLog(
					"Report created.  Error opening report: "
							+ ex.getMessage(), ex);
		}
	}
	
	
	public static void refreshReport(Report report) throws Exception{
		//create report file with default library
		File reportFile = new File(ReportPlugIn.getReportDirectory(), report.getFilename());

		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(reportFile.getAbsolutePath());
		
		List<?> datasets = rdh.getAllDataSets();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (((OdaDataSourceHandle)dataset.getDataSource()).getExtensionID().equals(SMART_DATASOURCE_ID)){
				//refresh the columns in the query
				DataSetUIUtil.updateColumnCacheAfterCleanRs(dataset);
			
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
					ReportPlugIn.displayLog("Could not open report " + report.getName() + ".\n\n" + e.getMessage(), e);
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
					ReportPlugIn.displayLog("Could not open report " + report.getName() + ".\n\n" + e.getMessage(), e);
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
		List<?> datasets = rdh.getAllDataSets();
		List<ReportQuery> reportQueries = new ArrayList<ReportQuery>();
		
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				OdaDataSetHandle h = (OdaDataSetHandle)dataset;
				if (h.getExtensionID().equals(SMART_DATASET_TYPE)){
					reportQueries.add(new ReportQuery(r, SmartUtils.decodeHex(h.getQueryText())));
				}
			}
		}
		Query q = s.createQuery("delete ReportQuery where id.report=:report");
		q.setParameter("report", r);
		q.executeUpdate();
		
		for (ReportQuery rq: reportQueries){
			s.saveOrUpdate(rq);
		}
	}

}
