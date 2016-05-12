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
import java.text.MessageFormat;
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
import org.eclipse.birt.report.model.api.LibraryHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.report.ReportEventManager;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.library.SmartBirtLibrary;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Report importer.
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ImportReportEngine {

	private static final String OVERWRITEDIALOG_CREATENEW = Messages.ImportReportEngine_CreateNewButton;
	private static final String OVERWRITEDIALOG_OVERWRITE = Messages.ImportReportEngine_OverwriterButton;
	private static final String OVERWRITEWARNING_DIALOG_MESSAGE = Messages.ImportReportEngine_Error_ReportExists;
	private static final String OVERWRITEWARNING_DIALOG_TITLE = Messages.ImportReportEngine_Overwrite_DialotTitle;
	private static final String IMPORTOP_DIALOG_TITLE = Messages.ImportReportEngine_ImportReport_DialogTitle;
	private static final String QUERY_ALREADYEXISTS_MSG = Messages.ImportReportEngine_Error_QueryAlreadyExists;
	
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
	 * @param folder the destination folder  (RootReportFolder or ReportFolder)
	 * @param ca the conservation to import the report into
	 * @return <code>true</code> if successful, <code>false</code> if cancelled
	 * @throws Exception if error occurs
	 */
	public boolean importReport(File file, Object folder, ConservationArea importCa) throws Exception{
		this.display = Display.getDefault();
		
		//unzip report deifnition file
		if (!SmartUtils.isZip(file)){
			throw new Exception (MessageFormat.format(
				Messages.ImportReportEngine_Error_InvalidFile, new Object[]{file.getAbsolutePath()}));
		}
		File tmpDir = unzip(file);
		try{
		
		//find the and read the .rpt file
		File reportPropFile = null;
		File[] f = tmpDir.listFiles();
		for (int i = 0; i < f.length; i ++){
			if (f[i].getName().endsWith(".rpt")){ //$NON-NLS-1$
				reportPropFile = f[i];
			}
		}
		if (reportPropFile == null){
			throw new Exception(Messages.ImportReportEngine_Error_NoPropertiesFile);
		}
		Report newReport = readReportInfo(reportPropFile, importCa);
		Employee assignedEmployee = ImportQueryUtil.findEmployee(importCa);
		
		session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			Report importReport = validateReport(newReport, importCa, assignedEmployee);
			if (importReport == null){
				//cancel pressed
				return false;
			}
			boolean isNew = false;
			if (importReport.getUuid() == null){
				//need to import new report - ask for location
				isNew = true;
				
				if (folder instanceof RootReportFolder){
					importReport.setShared(((RootReportFolder)folder).isShared());
					importReport.setFolder(null);
				}else if (folder instanceof ReportFolder){
					importReport.setFolder( (ReportFolder)folder);
					importReport.setShared( ((ReportFolder)folder).getEmployee() == null );
				}
				
				importReport.setId(ReportManager.generateReportId(importCa, session));
				if (importCa.getIsCcaa() && importReport.getShared()){
					importReport.setOwner(SmartDB.getSharedEmployee(session));
				}else{
					importReport.setOwner(assignedEmployee);
				}
			}else{
				//existing query
				//remove report->query link so when validating
				//queries we get correct results
				String hsql = "delete from ReportQuery where id.report= :report"; //$NON-NLS-1$
				Query q = session.createQuery(hsql);
				q.setParameter("report", importReport); //$NON-NLS-1$
				q.executeUpdate();
			}
			
			File reportXmlFile = new File(tmpDir, newReport.getFilename());
			
			//process queries
			if (!processQueries(reportXmlFile, tmpDir, importReport.getShared(), importReport.getOwner(), importCa)){
				return false ;
			}

			//save report to database
			session.saveOrUpdate(importReport);
			
			ReportDesignHandle rdh = SessionHandleAdapter.getInstance().getSessionHandle().openDesign(reportXmlFile.getAbsolutePath());
			
			//remove existing library & make sure it points to the library associated with this ca
			LibraryHandle library = rdh.getLibrary(SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
			rdh.dropLibraryAndBreakExtends(library);
			
			//add default library
			rdh.includeLibrary(SmartBirtLibrary.getInstance().getLibraryFileString(), SmartBirtLibrary.DEFAULT_LIBRARY_NAMESPACE);
			
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
			
			try {
				// fire query events
				display.syncExec(new Runnable() {
					@Override
					public void run() {
						for (org.wcs.smart.query.model.Query q : queriesAdded) {
							QueryEventManager.getInstance().fireQueryAdded(q);
						}
						for (org.wcs.smart.query.model.Query q : queriesModified) {
							QueryEventManager.getInstance().fireQueryDefinitionModified(q);
						}
					}
				});
				// fire new/update event
				if (isNew) {
					ReportEventManager.getInstance().fireReportAdded(
							importReport);
				} else {
					ReportEventManager.getInstance().fireReportUpdated(
							importReport);
				}
			} catch (Throwable t) {
				throw new Exception(
						Messages.ImportReportEngine_EventImportError);
			}
			return true;
		}catch (Exception ex){
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
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
		File f = ReportPlugIn.getDefault().getReportFile(r);
		if (r.getUuid() != null){
			//delete existing definition file if it exists
			f.delete();
		}
		SmartUtils.copyFile(source, f);
	}
	
	/**
	 * Reads the report information file which include the report name
	 * and file name.
	 * 
	 * @param f the properties file
	 * @return newly created report object
	 * @throws Exception
	 */
	private Report readReportInfo(File f, ConservationArea newCa) throws Exception{
		Properties prop = new Properties();
		try(InputStream inStream = new FileInputStream(f)){
			prop.load(inStream);
		}		
		Report r = new Report();
		r.setConservationArea(newCa);
		
		for (Object o : prop.keySet()){
			if (o.equals("filename")){ //$NON-NLS-1$
				r.setFilename((String)prop.get(o));
			}else if (((String)o).startsWith("name_")){ //$NON-NLS-1$
				String key = (String)o;
				int index = key.indexOf('_');
				String code = key.substring(index+1);
				
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					List<?> items = s.createCriteria(Language.class).add(Restrictions.eq("ca", r.getConservationArea())).add(Restrictions.eq("code", code)).list(); //$NON-NLS-1$ //$NON-NLS-2$
					for (Object item : items){
						r.updateName((Language)item, prop.getProperty(key));
					}
					r.setName(r.findName(SmartDB.getCurrentLanguage()));
					if (r.getName() == null){
						r.setName(r.findName(newCa.getDefaultLanguage()));
					}
				}finally{
					s.getTransaction().rollback();
					s.close();
				}
			}
		}
		
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
	private Report validateReport(final Report report, ConservationArea newCa, Employee newEmployee){
		
		String hql = "Select r from Report as  r join r.names as l where l.value = :name and ((r.owner = :owner and r.shared = 'false') or r.shared = 'true') and r.conservationArea = :ca"; //$NON-NLS-1$
		Query query = session.createQuery(hql);
		
		query.setParameter("name", report.getName()); //$NON-NLS-1$
		query.setParameter("owner", newEmployee); //$NON-NLS-1$
		query.setParameter("ca", newCa); //$NON-NLS-1$
		
		List<?> reports = query.list();
		if (reports.size() == 0){
			//keep the same report
			return report;
		}else if (reports.size() == 1){
			Report existing = (Report) reports.get(0);
			
			if (existing.getOwner().equals(newEmployee) && !existing.getShared()){
				
				final int[] overwrite = new int[]{-1};
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
								OVERWRITEWARNING_DIALOG_TITLE,
								null,
								MessageFormat.format(
								OVERWRITEWARNING_DIALOG_MESSAGE, new Object[]{report.getName()}),
								MessageDialog.QUESTION, 
								new String[]{IDialogConstants.CANCEL_LABEL, OVERWRITEDIALOG_OVERWRITE, OVERWRITEDIALOG_CREATENEW}, 1);
						overwrite[0] = md.open();
						
					}});
				if (overwrite[0] == 0){
					return null;
				}else if (overwrite[0] == 1){
					return existing;
				}
				
			}else{
				//shared
				if (ReportManager.canModifyCaReports()){
					
					final int[] overwrite = new int[]{-1};
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog md = new MessageDialog(Display.getDefault().getActiveShell(),
									OVERWRITEWARNING_DIALOG_TITLE,
									null,
									MessageFormat.format(OVERWRITEWARNING_DIALOG_MESSAGE, new Object[]{report.getName()}),
									MessageDialog.QUESTION, 
									new String[]{IDialogConstants.CANCEL_LABEL, OVERWRITEDIALOG_OVERWRITE, OVERWRITEDIALOG_CREATENEW}, 1);
							overwrite[0] = md.open();						
						}});
					if (overwrite[0] == 0){
						return null;
					}else if (overwrite[0] == 1){
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
					cont[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
							IMPORTOP_DIALOG_TITLE, 
							MessageFormat.format(
									Messages.ImportReportEngine_Error_DuplicateName, new Object[]{report.getName()}));
					
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
		File tempDir = null;
		try(ZipFile zout = new ZipFile(zipFile)) {
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
					try(InputStream is = zout.getInputStream(entry)){
						FileUtils.copyInputStreamToFile(is, fout);
					}
				}
			}	
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
			File queryDir, boolean sharedReport,
			Employee reportEmployee,
			ConservationArea importCa) throws Exception{
		
		SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();

		ReportDesignHandle rdh = session.openDesign(reportFile.getAbsolutePath());
		
		List<?> datasets = rdh.getDataSets().getContents();
		for (Iterator<?> iterator = datasets.iterator(); iterator.hasNext();) {
			DataSetHandle dataset = (DataSetHandle) iterator.next();
			if (dataset instanceof OdaDataSetHandle){
				OdaDataSetHandle handle = (OdaDataSetHandle)dataset;
				if (handle.getExtensionID().equals(ReportManager.SMART_DATASET_TYPE)){
					//smart dataset
					if (!processQuery(handle.getQueryText().split(":")[1],  //$NON-NLS-1$
							queryDir, handle, sharedReport, reportEmployee, importCa)){
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
			boolean sharedReport,
			Employee reportEmployee, 
			final ConservationArea importCa) throws Exception{
		
		final File queryFile = new File(queryDir, queryUuid + ".query"); //$NON-NLS-1$
		if (!queryFile.exists()){
			throw new Exception(MessageFormat.format(
					Messages.ImportReportEngine_Error_QueryNotFound, new Object[]{queryUuid}));
		}
		
		importedQuery = null;
		final List<String> queryWarnings = new ArrayList<String>();
		
		
		final QueryImportEngine qi = new QueryImportEngine();
		Job j = new Job(MessageFormat.format(Messages.ImportReportEngine_ImportQueryJobName, new Object[]{queryUuid})){
			//run the query importer in a different thread so it 
			//will use its own db connection
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try{
					importedQuery = qi.importQuery(queryFile, importCa);
					queryWarnings.addAll( qi.getWarnings());
					return Status.OK_STATUS;
				}catch (Exception ex){
					return new Status(IStatus.INFO, ReportPlugIn.PLUGIN_ID, IStatus.INFO, ex.getLocalizedMessage(), ex);
				}
			}};
		j.setSystem(true);
		j.schedule();
		j.join();
		if (j.getResult().getCode() == IStatus.INFO){
			throw (Exception)j.getResult().getException();
		}
		
		
		org.wcs.smart.query.model.Query smartQuery = findQuery(queryUuid, importedQuery, sharedReport, reportEmployee, importCa);
		if (smartQuery == null){
			//cancel selected
			return false;
		}
		
		if (smartQuery.getUuid() == null){
			//new query 
			smartQuery.setId(QueryHibernateManager.getInstance().generateQueryId(session));
						
			//smartQuery not found so we need to insert new query
			//display warning created during import process
			if (queryWarnings != null && queryWarnings.size() > 0){
				final StringBuilder warnings = new StringBuilder();
				
				for(String warn: queryWarnings){
					warnings.append(warn + "\n"); //$NON-NLS-1$
				}
				
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.ImportReportEngine_ImportQuery_WarningDialog_Title,MessageFormat.format(Messages.ImportReportEngine_ImportQuery_WarningDialog_Message, new Object[]{importedQuery.getName(), warnings.toString()}));
					}
				});

			}
			
			if (sharedReport){
				smartQuery.setIsShared(true);
			}else{
				smartQuery.setIsShared(false);
			}

			//save to db
			session.saveOrUpdate(smartQuery);
			queriesAdded.add(smartQuery);

		}else{
			//overwrite or use original
			session.saveOrUpdate(smartQuery);
			queriesModified.add(smartQuery);
		}
		
		//update report definition to point to correct query
		String newQueryText = smartQuery.getTypeKey() + ":" + UuidUtils.uuidToString(smartQuery.getUuid());  //$NON-NLS-1$
		oldToNewQueries.put(handle.getQueryText(), newQueryText);
		handle.setQueryText( newQueryText );
		return true;
	}
	
	private List<org.wcs.smart.query.model.Query> queriesAdded = new ArrayList<org.wcs.smart.query.model.Query>();
	private List<org.wcs.smart.query.model.Query> queriesModified = new ArrayList<org.wcs.smart.query.model.Query>();
	
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
			boolean sharedReport,
			final Employee reportEmployee,
			ConservationArea importCa) throws Exception{
		
		final List<org.wcs.smart.query.model.Query> queries = new ArrayList<org.wcs.smart.query.model.Query>();

		IQueryType type = QueryTypeManager.INSTANCE.findQueryType(importedQuery.getTypeKey());
		//search by uuid
		org.wcs.smart.query.model.Query uuidQuery = QueryHibernateManager.getInstance().findQuery(session, UuidUtils.stringToUuid(queryUuid), type);
		if (uuidQuery != null && uuidQuery.getConservationArea().equals(importCa)){
			queries.add(uuidQuery);
		}else{
			//search by name
			queries.addAll( QueryHibernateManager.getInstance().findQuery(session, 
						importedQuery.getName(), type, importCa, reportEmployee) );
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
						QueryImportMessageDialog importDia = new QueryImportMessageDialog(Display.getDefault().getActiveShell(), importedQuery.getName(), canOverwrite(importedQuery, reportEmployee));
						int ret = importDia.open();
						if (ret == QueryImportMessageDialog.CANCEL_INDEX){
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
					smartQuery.copyQuery(importedQuery);
					final org.wcs.smart.query.model.Query thisQuery = smartQuery;
					final boolean[] ok = new boolean[]{false};
					session.evict(smartQuery);					
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							ok[0] = QueryEventManager.getInstance().fireBeforeSave(thisQuery, session);
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
					if (ret == QueryImportMessageDialog.CANCEL_INDEX){
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
				if (!canOverwrite(smartQuery, reportEmployee)){
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(display.getActiveShell(), 
									Messages.ImportReportEngine_Error_DialogTitle, 
									Messages.ImportReportEngine_Error_CannotOverwirteQuery);
							
						}
					});
					return null;
				}
				
				smartQuery.copyQuery(importedQuery);
				session.evict(smartQuery);
				final boolean[] ok = new boolean[]{false};
				final org.wcs.smart.query.model.Query thisQuery = smartQuery;
				display.syncExec(new Runnable(){
					@Override
					public void run() {
						ok[0] = QueryEventManager.getInstance().fireBeforeSave(thisQuery, session);
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
	private boolean canOverwrite(org.wcs.smart.query.model.Query query, Employee employee){
		
		boolean admin =  ReportManager.canModifyCaReports();
		boolean isShared = query.getIsShared();
		boolean amOwner = query.getOwner().equals(employee);
		
		return (admin && isShared) || (amOwner && !isShared); 
	}
	
	class QueryImportMessageDialog extends MessageDialog{

		public static final int OK_INDEX = 0;
		public static final int CANCEL_INDEX = 1;
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
			
			super(parentShell, IMPORTOP_DIALOG_TITLE, null, 
					MessageFormat.format(QUERY_ALREADYEXISTS_MSG , new Object[]{queryName}),
					MessageDialog.QUESTION, new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, OK_INDEX );
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
			btnOp1.setText(Messages.ImportReportEngine_OpImportNewQuery);			
			
			
			final Button btnOp2 = new Button(comp, SWT.RADIO);
			btnOp2.setText(Messages.ImportReportEngine_OpOverwirteExisting);
			
			btnOp2.setVisible(canOverwrite);
			
			final Button btnOp3 = new Button(comp, SWT.RADIO);
			btnOp3.setText(Messages.ImportReportEngine_OpUpdateReport);
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

		public static final int OK_INDEX = 0;
		public static final int CANCEL_INDEX = 1;
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
			
			super(parentShell, IMPORTOP_DIALOG_TITLE, null, 
					MessageFormat.format(QUERY_ALREADYEXISTS_MSG , new Object[]{queryName}),
					MessageDialog.QUESTION, new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, OK_INDEX );
			
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
			lblQuery.setText(Messages.ImportReportEngine_Query_Label);
			
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
							sb.append(IQueryHibernateManager.CONSERVATION_AREA_QUERIES_NAME);
						}else{
							sb.append(IQueryHibernateManager.MY_QUERIES_NAME);
						}
						
						return ((org.wcs.smart.query.model.Query) element).getName() + " [" + ((org.wcs.smart.query.model.Query)element).getId() + "] - " + sb.toString(); //$NON-NLS-1$ //$NON-NLS-2$
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
			btnOp1.setText(Messages.ImportReportEngine_OpImportNewQuery_2);			
			btnOp1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			final Button btnOp2 = new Button(comp, SWT.RADIO);
			btnOp2.setText(Messages.ImportReportEngine_OpOverwirteExisting_2);
			btnOp2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			
			final Button btnOp3 = new Button(comp, SWT.RADIO);
			btnOp3.setText(Messages.ImportReportEngine_OpUpdateReport_2);
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



