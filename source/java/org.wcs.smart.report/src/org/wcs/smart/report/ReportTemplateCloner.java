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
package org.wcs.smart.report;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DesignConfig;
import org.eclipse.birt.report.model.api.DesignEngine;
import org.eclipse.birt.report.model.api.LibraryHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.ReportQuery;
import org.wcs.smart.util.SmartUtils;

/**
 * Report template cloner that copies report information
 * from the template conservation area to the new conservation area.
 * Also copies the report library and folders.  Only shared reports/folders
 * are copies (does not copy user specific folders or reports).
 * 
 * @author Emily
 *
 */
public class ReportTemplateCloner implements
		IConservationAreaTemplateCloner {

	/**
	 * @see org.wcs.smart.ca.IConservationAreaTemplateCloner#cloneTemplateData(org.wcs.smart.ca.ConservationAreaClonerEngine, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine, IProgressMonitor monitor) throws Exception {
		monitor.beginTask(Messages.ReportTemplateCloner_Progress_CopyReport, 3);
		try{
			monitor.subTask(Messages.ReportTemplateCloner_Progress_copyLibrary);
			cloneLibrary(engine);
			
			monitor.worked(1);
			monitor.subTask(Messages.ReportTemplateCloner_Progress_CopyFolder);
			cloneFolders(engine);
			
			monitor.worked(1);
			monitor.subTask(Messages.ReportTemplateCloner_Progress_CopyReportData);
			cloneReports(engine);
			
			monitor.worked(1);
		}finally{
			monitor.done();
		}
	}

	/*
	 * clones the report library and associated files (copies the entire
	 * directory)
	 */
	private void cloneLibrary(ConservationAreaClonerEngine engine) throws IOException{
		File templateDir = new File(engine.getTemplateCa().getFileDataStoreLocation(), ReportPlugIn.REPORT_DIR);
		File templateLibraryDir = new File(templateDir, SmartBirtLibrary.LIBRARY_DIR);
		
		if (templateLibraryDir.exists()){
			File caDir = new File(engine.getNewCa().getFileDataStoreLocation(), ReportPlugIn.REPORT_DIR);
			File caLibraryDir = new File(caDir, SmartBirtLibrary.LIBRARY_DIR);
			FileUtils.copyDirectory(templateLibraryDir, caLibraryDir);
		}
	}
	
	/*
	 * clones shared folders
	 */
	private void cloneFolders(ConservationAreaClonerEngine engine){
		@SuppressWarnings("unchecked")
		List<ReportFolder> queryFolder = 
				 engine.getSession()
					.createCriteria(ReportFolder.class)
					.add(Restrictions.isNull("parentFolder")) //$NON-NLS-1$
					.add(Restrictions.isNull("employee")) //$NON-NLS-1$
					.add(Restrictions.eq("conservationArea", //$NON-NLS-1$
							engine.getTemplateCa())).list();
				
		for (ReportFolder q : queryFolder){
			processReportFolder(q, null, engine);
		}
		engine.getSession().flush();
	}

	/*
	 * clones a folder
	 */
	private ReportFolder processReportFolder(ReportFolder templateFolder, ReportFolder newParent, ConservationAreaClonerEngine engine){
	
		ReportFolder clone = new ReportFolder();
		engine.copyLabels(templateFolder, clone);
		clone.setConservationArea(engine.getNewCa());
		clone.setEmployee(null);
		clone.setParentFolder(newParent);
		clone.setChildren(new ArrayList<ReportFolder>());
		
		engine.getSession().save(clone);
		engine.addConservationItemMapping(templateFolder, clone);
		
		for (ReportFolder kid : templateFolder.getChildren()){
			ReportFolder clonedKid = processReportFolder(kid, clone, engine);
			clone.getChildren().add(clonedKid);
		}
		return clone;
	}
	
	/*
	 * clones Reports and report query links.
	 */
	private void cloneReports(ConservationAreaClonerEngine engine) throws Exception{
		Employee newEmployee = engine.getNewCa().getEmployees().get(0);
		@SuppressWarnings("unchecked")
		List<Report> reports = engine.getSession().createCriteria(Report.class).add(Restrictions.eq("conservationArea", engine.getTemplateCa())).add(Restrictions.eq("shared", true)).list(); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (Report r : reports){
			Report clone = new Report();
			clone.setConservationArea(engine.getNewCa());
			if (r.getFolder() != null){
				clone.setFolder((ReportFolder)engine.getNewConservationItem(r.getFolder()));
			}
			clone.setId(r.getId());
			engine.copyLabels(r, clone);
			clone.setOwner(newEmployee);
			clone.setShared(r.getShared());
			clone.setFilename(r.getFilename());
			
			File src = new File(new File(engine.getTemplateCa().getFileDataStoreLocation(), ReportPlugIn.REPORT_DIR), r.getFilename());
			File dest = new File(new File(engine.getNewCa().getFileDataStoreLocation(), ReportPlugIn.REPORT_DIR), clone.getFilename());
			
			FileUtils.copyFile(src, dest);
	
			boolean save = true;
			try{
				updateReportFile(clone, dest,  engine);	
			}catch (Exception ex){
				save = false;
				ReportPlugIn.log(ex.getMessage(), ex);
				dest.delete();
			}
			
			if (save){
				engine.getSession().save(clone);
				engine.addConservationItemMapping(r, clone);
				
				@SuppressWarnings("unchecked")
				List<ReportQuery> queries = engine.getSession().createCriteria(ReportQuery.class).add(Restrictions.eq("id.report", r)).list(); //$NON-NLS-1$
				for (ReportQuery rq : queries){
					UuidItem item = engine.getNewConservationItem(rq.getQueryUuid());
					if (item != null){
						ReportQuery rqclone = new ReportQuery(clone, item.getUuid());
						engine.getSession().save(rqclone);
					}
				}
			}
		}
		engine.getSession().flush();
	}
	
	private void updateReportFile(Report report, File dest, ConservationAreaClonerEngine engine) throws Exception{
		SessionHandle session = new DesignEngine(new DesignConfig()).newSessionHandle(null);
		ReportDesignHandle rdh = session.openDesign(dest.getAbsolutePath());
		
		//update library reference
		//here we look for the templateuuid in the library file name
		//and replace it with the newuuid
		LibraryHandle library = rdh.getLibrary(SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
		rdh.dropLibraryAndBreakExtends(library);
		String newLibraryLocation = engine.getNewCa().getFileDataStoreLocation() + File.separator + ReportPlugIn.REPORT_DIR + File.separator + SmartBirtLibrary.LIBRARY_DIR + File.separator + SmartBirtLibrary.LIBRARY_FILENAME;
		rdh.includeLibrary(newLibraryLocation, SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
		
		//updates query references
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				if (((OdaDataSetHandle)dataset).getExtensionID().equals(ReportManager.SMART_DATASET_TYPE)){
					String bits[] = ((OdaDataSetHandle) dataset).getQueryText().split(":"); //$NON-NLS-1$
					
					String queryUuid = ((OdaDataSetHandle) dataset).getQueryText().split(":")[1]; //$NON-NLS-1$
					UuidItem newQueryReferences = engine.getNewConservationItem(SmartUtils.decodeHex(queryUuid));
					if (newQueryReferences != null){
						((OdaDataSetHandle) dataset).setQueryText(bits[0] + ":" + SmartUtils.encodeHex(newQueryReferences.getUuid())); //$NON-NLS-1$
					}else{
						//new query reference cannot be found; 
						throw new Exception(MessageFormat.format(Messages.ReportTemplateCloner_CloneError, new Object[]{report.getName()}));
					}
					
				}
			}
		}
		rdh.save();
		rdh.close();
	}
}
