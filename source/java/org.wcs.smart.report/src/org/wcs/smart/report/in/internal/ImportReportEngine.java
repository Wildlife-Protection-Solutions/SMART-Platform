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
package org.wcs.smart.report.in.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.qimport.QueryImporter;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.CreateReportDialog;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.util.SmartUtils;

/**
 * Report importer.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ImportReportEngine {

	/*
	 * Set of options for importing queries
	 */
	private static enum ImportOption{
		INSERT_NEW, OVERWRITE, USE_EXISTING
	};
	
	private Session session;
	private org.wcs.smart.query.model.Query importedQuery = null;
	private Display display;
	
	/**
	 * Imports the report 
	 * 
	 * @param file the report export to import
	 * @return <code>true</code> if successful, <code>false</code> if cancelled
	 * @throws Exception if error occurs
	 */
	public boolean importReport(File file) throws Exception{
		this.display = Display.getDefault();
		
		//unzip report deifnition file
		if (!SmartUtils.isZip(file)){
			throw new Exception ("Invalid file " + file.getAbsolutePath() + ".  This is not a zip file.");
		}
		File tmpDir = unzip(file);
		try{
		
		//find the and read the .rpt file
		File reportPropFile = null;
		File[] f = tmpDir.listFiles();
		for (int i = 0; i < f.length; i ++){
			if (f[i].getName().endsWith(".rpt")){
				reportPropFile = f[i];
			}
		}
		if (reportPropFile == null){
			throw new Exception("Report properties file not found.");
		}
		Report newReport = readReportInfo(reportPropFile);
		
		
		session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			Report importReport = validateReport(newReport);
			if (importReport == null){
				//cancel pressed
				return false;
			}
			boolean isNew = false;
			if (importReport.getUuid() == null){
				//need to import new report - ask for location
				isNew = true;
				
				final CreateReportDialog[] dialog = new CreateReportDialog[]{null};
				display.syncExec(new Runnable(){

					@Override
					public void run() {
						dialog[0] = new CreateReportDialog(display.getActiveShell(), null, null, false);
						dialog[0].setTitle("Import Report");
						if (dialog[0].open() != Window.OK){
							dialog[0]= null;
						}
						
					}});
				
				if (dialog[0] == null){
					return false;
				}
				Object folder = dialog[0].getReportFolder();
				if (folder instanceof RootReportFolder){
					importReport.setShared(((RootReportFolder)folder).isShared());
					importReport.setFolder(null);
				}else if (folder instanceof ReportFolder){
					importReport.setFolder( (ReportFolder)folder);
					importReport.setShared( ((ReportFolder)folder).getEmployee() == null );
				}
				
				importReport.setId(ReportManager.generateReportId(session));
				importReport.setOwner(SmartDB.getCurrentEmployee());
			}else{
				//existing query
				//remove report->query link so when validating
				//queries we get correct results
				String hsql = "delete from ReportQuery where id.report= :report";
				Query q = session.createQuery(hsql);
				q.setParameter("report", importReport);
				q.executeUpdate();
			}
			
			File reportXmlFile = new File(tmpDir, newReport.getFilename());
			
			//process queries
			if (!processQueries(reportXmlFile, tmpDir, importReport.getShared())){
				return false ;
			}

			//save report to database
			session.saveOrUpdate(importReport);
			
			ReportDesignHandle rdh = SessionHandleAdapter.getInstance().getSessionHandle().openDesign(reportXmlFile.getAbsolutePath());
			//remove existing library & make sure it points to the library associated with this ca
			rdh.dropLibrary(rdh.getLibrary(SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE));
			rdh.includeLibrary(SmartBirtLibrary.getInstance().getLibraryFile().toString(), SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
			//update report/query info			
			ReportManager.updateReportQueries(session, rdh, importReport);
			
			//fire events
			ReportEventManager.getInstance().fireReportImportHandlers(rdh, oldToNewQueries);

			rdh.save();
			rdh.close();
		
			//copy report file
			if (isNew){
				//generate a new filename
				importReport.setFilename(ReportManager.generateFilename(importReport));
			}
			copyToFileStore(reportXmlFile, importReport);
		
			
			session.getTransaction().commit();
			
			//fire new/update event
			if (isNew){
				ReportEventManager.getInstance().fireReportAdded(importReport);
			}else{
				ReportEventManager.getInstance().fireReportUpdated(importReport);
			}
			return true;
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			session.close();
		}
		}finally{
			if (tmpDir.exists()){
				FileUtils.deleteDirectory(tmpDir);
			}
		}
	}
	
	
	/**
	 * Copies the report xml definition file from the
	 * temporary unzip location to the ca filestore.
	 * <p>
	 * If the report xml file already exists in the filestore
	 * then it is deleted and overwriten by the new 
	 * file.
	 * </p>
	 * @param source the temp report definition file (from zip file)
	 * @param r the report
	 * @throws Exception
	 */
	private void copyToFileStore(File source, Report r) throws Exception{
		if (r.getUuid() != null){
			//delete existing definition file if it exists
			r.getFullReportFilename().delete();
		}
		SmartUtils.copyFile(source, r.getFullReportFilename());
	}
	
	/**
	 * Reads the report information file which include the report name
	 * and file name.
	 * 
	 * @param f the properties file
	 * @return newly created report object
	 * @throws Exception
	 */
	private Report readReportInfo(File f) throws Exception{
		Properties prop = new Properties();
		InputStream inStream = new FileInputStream(f);
		prop.load(inStream);
		String reportName = prop.getProperty("name");
		String fileName = prop.getProperty("filename");
		inStream.close();
		
		Report r = new Report();
		r.setConservationArea(SmartDB.getCurrentConservationArea());
		r.setName(reportName);
		r.setFilename(fileName);
		
		return r;
	}
	
	/**
	 * Determines is a report already exists in the database
	 * with the same name.
	 * <p>
	 * If a report exists it prompts the user if they want to overwrite or
	 * save as new.
	 * </p>
	 * 
	 * @param report the report name
	 * @return the report object to save to; if the user wants to overwrite an existing report this will
	 * be the existing report object, otherwise it will be a new report object 
	 */
	private Report validateReport(final Report report){
		
		String hql = " from Report where name = :name and ((owner = :owner and shared = 'false') or shared = 'true') and conservationArea = :ca";
		Query query = session.createQuery(hql);
		
		query.setParameter("name", report.getName());
		query.setParameter("owner", SmartDB.getCurrentEmployee());
		query.setParameter("ca", SmartDB.getCurrentConservationArea());
		
		List<?> reports = query.list();
		if (reports.size() == 0){
			//keep the same report
			return report;
		}else if (reports.size() == 1){
			Report existing = (Report) reports.get(0);
			
			if (existing.getOwner().equals(SmartDB.getCurrentEmployee()) && !existing.getShared()){
				
				final boolean[] overwrite = new boolean[]{false};
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
								"Overwrite",
								null,
								"A report with the name " + report.getName() + " already existings.  Do you wish to overwrite it or create a new report?",
								MessageDialog.QUESTION, 
								new String[]{"Overwrite", "Create New"}, 0);
						if (md.open() == 0){
							//overwrite
							overwrite[0] = true;
						}
						
					}});
				if (overwrite[0]){
					return existing;
				}
				
			}else{
				//shared
				if (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN || 
						SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER ){
					
					final boolean[] overwrite = new boolean[]{false};
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
									"Overwrite",
									null,
									"A report with the name " + report.getName() + " already existings.  Do you wish to overwrite it or create a new report?",
									MessageDialog.QUESTION, 
									new String[]{"Overwrite", "Create New"}, 0);
							if (md.open() == 0){
								//overwrite
								overwrite[0] = true;
							}							
						}});
					if (overwrite[0]){
						return existing;
					}

				}else{
					//import new
				}
			}
		}else if (reports.size() > 1){
			final boolean[] cont = new boolean[]{true};
			display.syncExec(new Runnable(){

				@Override
				public void run() {
					cont[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Import", "Multiple reports already exist with the name " + report.getName() +".  This report will be imported as a new report.  Do you want to continue?");
					
				}});
			if (!cont[0]){
				return null;
			}
		}		
		
		
		return report;
	}
	
	/**
	 * Unzips the content of the zip
	 * file to a temporary directory.
	 * 
	 * @param zipFile the zip file to unzip
	 * @return the location of the files
	 * @throws Exception
	 */
	private static File unzip(File zipFile) throws Exception{
		ZipFile zout = new ZipFile(zipFile);
		
		File tempDir = null;
		try {
			tempDir = File.createTempFile(zipFile.getName(),
					Long.toString(System.nanoTime()));
			tempDir.delete();
			tempDir.mkdir();
		
			Enumeration<? extends ZipEntry> elements = zout.entries();
			while(elements.hasMoreElements()){
				ZipEntry entry = elements.nextElement();
				
				File fout = new File(tempDir.getAbsoluteFile() + File.separator + entry.getName());
				if (entry.isDirectory()){
					FileUtils.forceMkdir(fout);
				}else{
					InputStream is = zout.getInputStream(entry);
					try{
						FileUtils.copyInputStreamToFile(is, fout);
					}finally{
						is.close();
					}
				}
			}
			
		} finally {
			zout.close();
		}
		return tempDir;
	}
	
	/**
	 * Process the queries in the report file 
	 * @param reportFile the report BIRT xml file
	 * @param queryDir the directory containing the exported query files.
	 * @param sharedReport true if the report is shared false if user report.
	 * Shared reports can only use shared queries.
	 * @return <code>true</code> if queries processed ok, <code>false</code> if import should exit
	 * @throws Exception
	 */
	private boolean  processQueries(File reportFile, 
			File queryDir, boolean sharedReport) throws Exception{
		
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(reportFile.getAbsolutePath());
		
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				OdaDataSetHandle handle = (OdaDataSetHandle)dataset;
				if (handle.getExtensionID().equals(ReportManager.SMART_DATASET_TYPE)){
					//smart dataset
					if (!processQuery(handle.getQueryText().split(":")[1], 
							queryDir, handle, sharedReport)){
						return false;
					}
				}
			}
		}
		rdh.save();
		rdh.close();
		return true;
	}
	
	/**
	 * Processes a single query determining if the query already exists in the db
	 * with the same name, definition. 
	 * 
	 * <p>If an exact match query cannot be found then the query is used.</p>
	 * <p>If no match is found the query is imported.</p>
	 * <p>If a name match is found but the query definition is different
	 * the user is prompted if they want to import as a new, overwrite or 
	 * use the existing report.</p>
	 * 
	 * <p>Updates the report handle with the query uuid as required.</p>
	 *   
	 * @param queryUuid
	 * @param queryDir
	 * @param handle
	 * @param sharedReport true if the report is shared false if user report.
	 * Shared reports can only use shared queries.
	 * @return <code>true</code> if okay; <code>false</code> if we should exit the import process
	 * @throws Exception
	 */
	
	private boolean processQuery(String queryUuid, 
			File queryDir, 
			OdaDataSetHandle handle,
			boolean sharedReport) throws Exception{
		
		final File queryFile = new File(queryDir, queryUuid + ".query");
		if (!queryFile.exists()){
			throw new Exception("Query file not found for query " + queryUuid + ".  Report cannot be imported.");
		}
		
		importedQuery = null;
		final List<String> queryWarnings = new ArrayList<String>();
		
		
		final QueryImporter qi = new QueryImporter();
		Job j = new Job("import query " + queryUuid){
			//run the query importer in a different thread so it 
			//will use its own db connection
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try{
					importedQuery = qi.importQuery(queryFile);
					queryWarnings.addAll( qi.getWarnings());
					return Status.OK_STATUS;
				}catch (Exception ex){
					return new Status(IStatus.ERROR, ReportPlugIn.PLUGIN_ID, IStatus.ERROR, ex.getMessage(), ex);
				}
			}};
		j.schedule();
		j.join();
		if (j.getResult().getCode() == IStatus.ERROR){
			throw (Exception)j.getResult().getException();
		}
		
		
		org.wcs.smart.query.model.Query smartQuery = findQuery(queryUuid, importedQuery, sharedReport);
		if (smartQuery == null){
			//cancel selected
			return false;
		}
		
		if (smartQuery.getUuid() == null){
			//new query 
			smartQuery.setId(QueryHibernateManager.generateQueryId(session));
						
			//smartQuery not found so we need to insert new query
			//display warning created during import process
			if (queryWarnings != null && queryWarnings.size() > 0){
				final StringBuilder warnings = new StringBuilder();
				warnings.append("The following warnings were generated while importing the query " + importedQuery.getName() +".\n");
				for(String warn: queryWarnings){
					warnings.append(warn + "\n");
				}
				
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Query Warnings",warnings.toString());
					}
				});

			}
			
			//TODO: - perhaps ask users where they want to import report
			if (sharedReport){
				smartQuery.setIsShared(true);
			}else{
				smartQuery.setIsShared(false);
			}

			//save to db
			session.saveOrUpdate(smartQuery);
			
			final org.wcs.smart.query.model.Query thisQuery = smartQuery;
			display.syncExec(new Runnable(){
				@Override
				public void run() {
					QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_ADDED, thisQuery);
				}
				
			});
			

		}else{
			//overwrite or use original
			session.saveOrUpdate(smartQuery);
			final org.wcs.smart.query.model.Query thisQuery = smartQuery;
			display.syncExec(new Runnable(){
				@Override
				public void run() {
					QueryEventManager.getInstance().fireQueryChangedListeners(thisQuery);
				}
			});
		}
		
		//update report definition to point to correct query
		String newQueryText = smartQuery.getType().name() + ":" + SmartUtils.encodeHex(smartQuery.getUuid()); 
		oldToNewQueries.put(handle.getQueryText(), newQueryText);
		handle.setQueryText( newQueryText );
		
		return true;
	}
	
	private HashMap<String, String> oldToNewQueries = new HashMap<String, String>();
	/**
	 * Finds the database query that matches the imported query
	 * 
	 * @param importedQuery the imported query
	 * @param sharedReport <code>true</code> if the report is shared <code>false</code> if user report.
	 * Shared reports can only use shared queries.
	 * @return null if import should be cancelled
	 * otherwise the query to use in the report.  This query
	 * could be the importedQuery (if no queries found; or
	 * the users decided to import a new query; an un-modified
	 * existing db query if the user decides to use a db query
	 * as exists; or a modified existing db query if the user
	 * decides to update the db query definition with the imported
	 * query definition.
	 * 
	 * @throws Exception
	 */
	private org.wcs.smart.query.model.Query findQuery(
			String queryUuid,
			final org.wcs.smart.query.model.Query importedQuery,
			boolean sharedReport) throws Exception{
		
		final List<org.wcs.smart.query.model.Query> queries = new ArrayList<org.wcs.smart.query.model.Query>();

		//search by uuid
		org.wcs.smart.query.model.Query uuidQuery = QueryHibernateManager.findQuery(session, SmartUtils.decodeHex(queryUuid), importedQuery.getType());
		if (uuidQuery != null){
			queries.add(uuidQuery);
		}else{
			//search by name
			queries.addAll( QueryHibernateManager.findQuery(session, 
						importedQuery.getName(), 
						importedQuery.getType()) );
		}
		
		if (sharedReport){
			//remove any non-shared queries
			for (Iterator<org.wcs.smart.query.model.Query> iterator = queries.iterator(); iterator.hasNext();) {
				org.wcs.smart.query.model.Query query = (org.wcs.smart.query.model.Query) iterator.next();
				if (!query.getIsShared()){
					iterator.remove();
				}
			}
		}
		
		
		if (queries.size() == 0){			
			return importedQuery;
		}else if (queries.size() == 1){
			org.wcs.smart.query.model.Query smartQuery = queries.get(0);
			
			if (!validateQuery(importedQuery, smartQuery)){
				//query is different 
				
				//the query "BLAH" used in the report already exists
				//in this database but has a different query definition.
				//Would you like to:
				// overwrite the existing query with the report query
				// import the report query into the database as a new query  
				// use the query in the database [report may not run]
				
				// can only overwrite IF admin/manager or smartQuery is a user query
				
				final ImportOption[] data = new ImportOption[]{null};
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						QueryImportMessageDialog importDia = new QueryImportMessageDialog(Display.getDefault().getActiveShell(), importedQuery.getName(), canOverwrite(importedQuery));
						int ret = importDia.open();
						if (ret == QueryImportMessageDialog.CANCEL){
							data[0] = null;
						}else{
							data[0] = importDia.getOption();
						}
					}
					
				});
				
				
				ImportOption op = data[0];
				if (op == null){
					return null;
				}
				if (op == ImportOption.USE_EXISTING){
					return smartQuery;
				}else if (op == ImportOption.OVERWRITE){
					smartQuery.copyFrom(importedQuery);
					final org.wcs.smart.query.model.Query thisQuery = smartQuery;
					final boolean[] ok = new boolean[]{false};
					session.evict(smartQuery);
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							ok[0] = QueryEventManager.getInstance().fireBeforeSaveListeners(thisQuery, session);
						}
					});
					if (!ok[0]){
						return null;
					}
					smartQuery = (org.wcs.smart.query.model.Query) session.merge(smartQuery);
					return smartQuery;
				}else if (op == ImportOption.INSERT_NEW){
					return importedQuery;
				}
				
			}
			return queries.get(0);
		}else{
			for (org.wcs.smart.query.model.Query smartQuery : queries){
				if (validateQuery(importedQuery, smartQuery)){
					return smartQuery;
				}
			}
			//could not find a query with the same name and definition
			//ask user
			
			final Object[] data = new Object[]{null, null};
			display.syncExec(new Runnable(){
				@Override
				public void run() {
					MultiQueryImportMessageDialog importDia = 
							new MultiQueryImportMessageDialog(Display.getDefault().getActiveShell(), importedQuery.getName(), queries);
					int ret = importDia.open();
					if (ret == QueryImportMessageDialog.CANCEL){
						data[0] = null;
					}else{
						data[0] = importDia.getOption();
						data[1] = importDia.getSelectedQuery();
					}
				}
				
			});
			
			ImportOption op = (ImportOption) data[0];
			org.wcs.smart.query.model.Query smartQuery = (org.wcs.smart.query.model.Query) data[1];
			if (op == null){
				return null;
			}
			if (op == ImportOption.USE_EXISTING){
				return smartQuery;
			}else if (op == ImportOption.OVERWRITE){
				if (!canOverwrite(smartQuery)){
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(display.getActiveShell(), "Error", "You do not have the required permissions to overwrite this query.");
							
						}
					});
					return null;
				}
				
				smartQuery.copyFrom(importedQuery);
				session.evict(smartQuery);
				final boolean[] ok = new boolean[]{false};
				final org.wcs.smart.query.model.Query thisQuery = smartQuery;
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						ok[0] = QueryEventManager.getInstance().fireBeforeSaveListeners(thisQuery, session);
					}
				});
				if (!ok[0]){
					return null;
				}
				smartQuery = (org.wcs.smart.query.model.Query) session.merge(smartQuery);
				return smartQuery;
			}else if (op == ImportOption.INSERT_NEW){
				return importedQuery;
			}
			return importedQuery;
		}
	}
	
	
	/**
	 * Determines if the two query definitions are the same
	 * @param importQuery
	 * @param smartQuery
	 * @return
	 */
	private boolean validateQuery(org.wcs.smart.query.model.Query importQuery, 
			org.wcs.smart.query.model.Query smartQuery){
		return importQuery.isDefinitionEqual(smartQuery);		
	}

	/**
	 * Determines if the query can be overwritten by the 
	 * current user
	 * @param query
	 * @return
	 */
	private boolean canOverwrite(org.wcs.smart.query.model.Query query){
		
		boolean admin =  SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN || 
					SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER;
		boolean isShared = query.getIsShared();
		boolean amOwner = query.getOwner().equals(SmartDB.getCurrentEmployee());
		
		return (admin && isShared) || (amOwner && !isShared); 
	}
	
	class QueryImportMessageDialog extends MessageDialog{

		public static final int OK = 0;
		public static final int CANCEL = 1;
		private ImportOption op = null;
		private boolean canOverwrite;
		
		/**
		 * @param parentShell
		 * @param dialogTitle
		 * @param dialogTitleImage
		 * @param dialogMessage
		 * @param dialogImageType
		 * @param dialogButtonLabels
		 * @param defaultIndex
		 */
		public QueryImportMessageDialog(Shell parentShell, String queryName, boolean canOverwrite) {
			
			super(parentShell, "Report Import", null, "The query " + queryName + " used in the report already exists in the database but with a different query definition." ,
					MessageDialog.QUESTION, new String[]{"Ok", "Cancel"}, OK );
			this.canOverwrite = canOverwrite;
			
		}
		
		public ImportOption getOption(){
			return this.op;
		}
		@Override
	    protected Control createCustomArea(Composite parent) {
			Composite comp = new Composite(parent, SWT.NONE);
			comp.setLayout(new GridLayout(1, false));
			
			final Button btnOp1 = new Button(comp, SWT.RADIO);
			btnOp1.setText("Import the report query into the database as a new query. [Recommended]");			
			
			
			final Button btnOp2 = new Button(comp, SWT.RADIO);
			btnOp2.setText("Overwrite the existing database query with the report query.");
			
			btnOp2.setVisible(canOverwrite);
			
			final Button btnOp3 = new Button(comp, SWT.RADIO);
			btnOp3.setText("Replace the report query with the database query [this may result in report errors].");
			Listener opSelected = new Listener() {
				
				@Override
				public void handleEvent(Event event) {
					if (btnOp1.getSelection()){
						op = ImportOption.INSERT_NEW;
					}else if (btnOp2.getSelection()){
						op = ImportOption.OVERWRITE;
					}else if (btnOp3.getSelection()){
						op = ImportOption.USE_EXISTING;
					}
					
				}
			};
			btnOp1.addListener(SWT.Selection, opSelected);
			btnOp2.addListener(SWT.Selection, opSelected);
			btnOp3.addListener(SWT.Selection, opSelected);
			
			
			btnOp1.setSelection(true);
			op = ImportOption.INSERT_NEW;
			
			return comp;
	    }
	
	}
	
	
	
	class MultiQueryImportMessageDialog extends MessageDialog{

		public static final int OK = 0;
		public static final int CANCEL = 1;
		private ImportOption op = null;
		private org.wcs.smart.query.model.Query selectedQuery = null;
		
		
		private List<org.wcs.smart.query.model.Query> exisitngQueries;
		
		/**
		 * @param parentShell
		 * @param dialogTitle
		 * @param dialogTitleImage
		 * @param dialogMessage
		 * @param dialogImageType
		 * @param dialogButtonLabels
		 * @param defaultIndex
		 */
		public MultiQueryImportMessageDialog(Shell parentShell, String queryName,
				List<org.wcs.smart.query.model.Query> exisitngQueries) {
			
			super(parentShell, "Report Import", null, 
					"The query " + queryName + " used in the report already exists (mutliple times) in the database but with a different query definition." ,
					MessageDialog.QUESTION, new String[]{"Ok", "Cancel"}, OK );
			
			this.exisitngQueries = exisitngQueries;
			
		}
		public org.wcs.smart.query.model.Query getSelectedQuery(){
			return this.selectedQuery;
		}
		
		public ImportOption getOption(){
			return this.op;
		}
		@Override
	    protected Control createCustomArea(Composite parent) {

			
			Composite comp = new Composite(parent, SWT.NONE);
			comp.setLayout(new GridLayout(2, false));
			
			final Label lblQuery = new Label(comp, SWT.NONE);
			lblQuery.setText("Database Query:");
			
			final ComboViewer cmbQueries = new ComboViewer(comp,  SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbQueries.setContentProvider(ArrayContentProvider.getInstance());
			cmbQueries.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof org.wcs.smart.query.model.Query){
						
						org.wcs.smart.query.model.Query query = (org.wcs.smart.query.model.Query) element;
						StringBuilder sb = new StringBuilder();
						QueryFolder parent = query.getFolder();
						while(parent != null){
							sb.append(parent.getName());
							sb.append(File.separator);
							parent = parent.getParentFolder();
							
						}
						if (query.getIsShared()){
							sb.append(QueryHibernateManager.CONSERVATION_AREA_QUERIES_NAME);
						}else{
							sb.append(QueryHibernateManager.MY_QUERIES_NAME);
						}
						
						return ((org.wcs.smart.query.model.Query) element).getName() + " [" + ((org.wcs.smart.query.model.Query)element).getId() + "] - " + sb.toString();
					}
					return super.getText(element);
				}
			});
			selectedQuery =  exisitngQueries.get(0);
			cmbQueries.setInput(exisitngQueries.toArray());
			cmbQueries.setSelection(new StructuredSelection(selectedQuery));
			cmbQueries.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					selectedQuery = (org.wcs.smart.query.model.Query) ((IStructuredSelection)cmbQueries.getSelection()).getFirstElement();
					
				}
			});
			
			final Button btnOp1 = new Button(comp, SWT.RADIO);
			btnOp1.setText("Import the report query into the database as a new query. [Recommended]");			
			btnOp1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			final Button btnOp2 = new Button(comp, SWT.RADIO);
			btnOp2.setText("Overwrite the database query (selected above) with the report query.");
			btnOp2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			
			final Button btnOp3 = new Button(comp, SWT.RADIO);
			btnOp3.setText("Replace the report query with the database query selected above. [this may result in report errors].");
			btnOp3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			Listener opSelected = new Listener() {
				
				@Override
				public void handleEvent(Event event) {
					if (btnOp1.getSelection()){
						op = ImportOption.INSERT_NEW;
						cmbQueries.getCombo().setEnabled(false);
						lblQuery.setEnabled(false);
					}else if (btnOp2.getSelection()){
						op = ImportOption.OVERWRITE;
						lblQuery.setEnabled(true);
						cmbQueries.getCombo().setEnabled(true);
					}else if (btnOp3.getSelection()){
						op = ImportOption.USE_EXISTING;
						cmbQueries.getCombo().setEnabled(true);
						lblQuery.setEnabled(true);
					}
					
				}
			};
			btnOp1.addListener(SWT.Selection, opSelected);
			btnOp2.addListener(SWT.Selection, opSelected);
			btnOp3.addListener(SWT.Selection, opSelected);
			
			
			btnOp1.setSelection(true);
			op = ImportOption.INSERT_NEW;
			cmbQueries.getCombo().setEnabled(false);
			lblQuery.setEnabled(false);
			
			
			return comp;
	    }
	
	}
}



