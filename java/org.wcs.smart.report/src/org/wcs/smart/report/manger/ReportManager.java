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

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.designer.SmartReportPerspective;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.util.SmartUtils;

/**
 * A report manager.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportManager {

	private static IReportEngine reportEngine = null;
	
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
		reportEngine.destroy();
	}
	
	/**
	 * Deletes a report folder from the database.
	 * @param folder the folder to delete
	 * @throws Exception if folder cannot be deleted
	 */
	public static void deleteReportFolder(ReportFolder folder) throws Exception{
		Session session = HibernateManager.openSession();
		try{
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
	public static void deleteReport(Report report) throws Exception{
		
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
				DecimalFormat df = new DecimalFormat("######");
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
			File reportFile = new File(ReportPlugIn.getReportDirectory(), r.getFilename()); 
			reportFile = reportFile.getCanonicalFile();
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			window.getWorkbench().showPerspective(SmartReportPerspective.ID,window);
			if (reportFile != null) {
				ReportEditorInput ri = new ReportEditorInput(reportFile);
				window.getActivePage().openEditor(
								ri,
								IReportEditorContants.DESIGN_EDITOR_ID);
			}
		} catch (Exception ex) {
			ReportPlugIn.displayLog(
					"Report created.  Error opening report: "
							+ ex.getMessage(), ex);
		}
	}
	
	/**
	 * Opens the given report the view perspective and run the report.
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
}
