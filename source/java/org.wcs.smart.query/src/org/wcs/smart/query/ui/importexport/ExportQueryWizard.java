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
package org.wcs.smart.query.ui.importexport;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.importexport.ICsvQueryExporter;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard for exporting query results.
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryWizard extends Wizard implements IPageChangingListener{
	
	public static final String LAST_DIR_KEY = "LAST_EXPORT_DIR"; //$NON-NLS-1$
	
	private static final String EXPORT_FAILED_MGS = Messages.ExportQueryWizard_ExportFailedError;

	private static final String EXPORT_DIALOGTITLE = Messages.ExportQueryWizard_ExportDialogTitle;

	private Query query;

	private ExportQueryTypePage page1;
	private ExportQueryLocationPage page2;
	private ExportQueryListPage page3;
	private ExportQueryDefLocationPage page4;
	
	private boolean hasError = false;
	private List<QueryEditorInput> initSelection = null;
	
	private ExportQueryWizard(Query query, List<QueryEditorInput> initSelection) {
		this.query = query;
		if (this.query != null){
			setWindowTitle(Messages.ExportQueryWizard_Title1);	
		}else{
			this.initSelection = initSelection;
			setWindowTitle(Messages.ExportQueryWizard_Title2);
		}
		
		setDialogSettings(QueryPlugIn.getDefault().getDialogSettings());
		
		super.setNeedsProgressMonitor(true);
	}
	
	/**
	 * Creates a new wizard that will allow users to export
	 * a set of query definitions.
	 * 
	 * @param initSelection
	 */
	public ExportQueryWizard(List<QueryEditorInput> initSelection) {
		this(null, initSelection);
		
	}
	
	/**
	 * Creates a new wizard that will allow users to export
	 * query results from the given query.
	 *
	 * @param query the query to export
	 */
	public ExportQueryWizard(Query query) {
		this(query, null);
	}

    /*
     * (non-Javadoc) Method declared on IWizard.
     */
    public boolean canFinish() {
    	if (getContainer().getCurrentPage() == page2 && page2.isPageComplete()){
    		return true;
    	}else if (getContainer().getCurrentPage() == page4 && page4.isPageComplete()){
    		return true;
    	}
    	return false;
    }
	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		if (this.query != null){
			page1 = new ExportQueryTypePage();
			super.addPage(page1);
		
			page2 = new ExportQueryLocationPage();
			super.addPage(page2);
		}
		
		page3 = new ExportQueryListPage();
		page4 = new ExportQueryDefLocationPage();
		super.addPage(page3);
		super.addPage(page4);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		if (this.query == null){
			page3.initValues(initSelection);
		}
	}

	/**
	 * @return the query exporter for the format selected
	 * on the first query page.
	 */
	public IQueryExporter getQueryExporter(){
		return page1.getQueryExporter();
	}
	
	/**
	 * The query to export or null if no specific query is active
	 * @return
	 */
	public Query getQuery(){
		return this.query;
	}
	
	/**
	 * Runs the export process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		hasError = false;
		
		if (page1 != null){
			page1.performFinish();
		}
		
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						if (getContainer().getCurrentPage() == page2){
							IQueryExporter exporter = getQueryExporter();
							if (exporter == null){
								hasError = true;
								return;
							}
							
							hasError = !exportSingleFile(exporter, monitor);
						}else if (getContainer().getCurrentPage() == page4){
							exportMultiDefs(monitor);
						}
					} catch (Exception e) {
						QueryPlugIn.displayLog(
								EXPORT_FAILED_MGS + e.getLocalizedMessage(), e);
						hasError = true;
					}
				}
			});
		} catch (Exception e) {
			QueryPlugIn.displayLog(EXPORT_FAILED_MGS + e.getLocalizedMessage(), e);
		}
		return !hasError;
	}
	
	/**
	 * Exports a single query to the selected format/file.
	 */
	private boolean exportSingleFile(IQueryExporter exporter, IProgressMonitor monitor) throws Exception{
		File outputFile = page2.getFile();
		getDialogSettings().put(LAST_DIR_KEY, outputFile.getParent().toString());
		
		if (!outputFile.getParentFile().exists()){
			boolean create = MessageDialog.openQuestion(getShell(), Messages.ExportQueryWizard_DialogTitle, MessageFormat.format(Messages.ExportQueryWizard_DirectoryDoesNotExist, new Object[]{outputFile.getParent()}));
			if (!create){
				return false;
			}else{
				if (!SmartUtils.createDirectory(outputFile.getParentFile())){
					return false;
				}
			}
		}
		
		if (outputFile.exists()){
			if (!MessageDialog.openConfirm(getShell(), 
					Messages.ExportQueryWizard_OverwriteDialogTitle, 
					MessageFormat.format(Messages.ExportQueryWizard_OverwriteDialogMessage, new Object[]{outputFile.toString()}))){
				hasError = true;
				return false;
			}
		}
		
		HashMap<String, Object> ops = null;
		try{
			ops = page2.getOptions();
			if (ops != null){
				SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_DELIMITER_KEY, String.valueOf((Character)ops.get(ICsvQueryExporter.DELIMITER_KEY)));
			}
		}catch(Exception ex){
			MessageDialog.openError(getShell(), Messages.ExportQueryWizard_ExportDialogTitle, ex.getMessage());
			hasError = true;
			return false;
		}
		
		exporter.export(getQuery(), getQuery().getCachedResults(), outputFile, ops, monitor);

		if (monitor.isCanceled()){
			MessageDialog.openInformation(
					Display.getDefault().getActiveShell(), EXPORT_DIALOGTITLE,
					Messages.ExportQueryWizard_ExportCancelled_DialogMessage);
		}else{
			MessageDialog.openInformation(
				Display.getDefault().getActiveShell(), EXPORT_DIALOGTITLE,
				Messages.ExportQueryWizard_ExportOk_DialogMessage);
		}
		return true;
	}
	
	/**
	 * Exports a single query to the selected format/file.
	 */
	private void exportMultiDefs(IProgressMonitor monitor) {
		File outputLocation = page4.getExportLocation();
		List<ConservationArea> cas = page4.getConservationAreasToExport();
		
		HashMap<String, Object> ops = null;
		try{
			if (page2 != null){
				ops = page2.getOptions();
				if (ops != null){
					SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_DELIMITER_KEY, String.valueOf((Character)ops.get(ICsvQueryExporter.DELIMITER_KEY)));
				}
			}
		}catch(Exception ex){
			MessageDialog.openError(getShell(), Messages.ExportQueryWizard_ExportDialogTitle, ex.getMessage());
			hasError = true;
			return ;
		}
		
		if (outputLocation != null){
			getDialogSettings().put(LAST_DIR_KEY, outputLocation.toString());
			
			if (!outputLocation.exists()){
				boolean create = MessageDialog.openQuestion(getShell(), Messages.ExportQueryWizard_DialogTitle, MessageFormat.format(Messages.ExportQueryWizard_DirectoryDoesNotExist, new Object[]{outputLocation.toString()}));
				if (!create){
					hasError = true;
					return;
				}else{
					if (!SmartUtils.createDirectory(outputLocation)){
						hasError = true;
						return;
					}
				}
			}
		}else{
			//exporting to ca; create a temp directory to work with
			try{
				outputLocation = Files.createTempDirectory("queryexports").toFile(); //$NON-NLS-1$
			}catch (Exception ex){
				QueryPlugIn.displayLog(ex.getMessage(), ex);
				hasError = true;
				return;
			}
		}
		
		monitor.beginTask(Messages.ExportQueryWizard_ExportProgress, page3.getQueries().size()  * (cas == null ? 1 : (cas.size() + 1)));
		
		HashMap<Query, File> exportedQueries = exportQueriesToFile(outputLocation, page3.getQueries(), ops, monitor);
		
		if (cas == null){
			if (monitor.isCanceled()){
				openInfo(MessageFormat.format(Messages.ExportQueryWizard_ExportCancelled, new Object[]{exportedQueries.size(), page3.getQueries().size()}));
			}else{
				openInfo(MessageFormat.format(Messages.ExportQueryWizard_ExportCompleted, new Object[]{exportedQueries.size(), page3.getQueries().size()}));
			}
		}else{
			if (monitor.isCanceled()){
				openInfo(MessageFormat.format(Messages.ExportQueryWizard_ExportCancelled, new Object[]{0, page3.getQueries().size()}));
				hasError = true;
				return;
			}
			
			//import files into each conservation area
			Object[] results = importQueries(cas, exportedQueries, monitor);
			int cnt = (int) results[0];
			List<String> error = (List<String>) results[1];
			try{
				FileUtils.deleteDirectory(outputLocation);
			}catch(Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
			}
			if (monitor.isCanceled()){
				openInfo(MessageFormat.format(Messages.ExportQueryWizard_Cancelled, new Object[]{cnt, exportedQueries.size() * cas.size()}));
			}else{
				if (error.size() == 0){
					openInfo(MessageFormat.format(Messages.ExportQueryWizard_Complete, new Object[]{cnt, exportedQueries.size() * cas.size()}));
				}else{
					WarningDialog wd = new WarningDialog(getContainer().getShell(), 
							EXPORT_DIALOGTITLE, 
							MessageFormat.format(Messages.ExportQueryWizard_CompleteWError, new Object[]{cnt, exportedQueries.size() * cas.size()}), 
							error);
					wd.open();
				}
			}
		}
	}
	private void openInfo(String message){
		MessageDialog.openInformation(
				getContainer().getShell(), EXPORT_DIALOGTITLE,
				message);
	}
	
	private Object[] importQueries(List<ConservationArea> cas, HashMap<Query, File> queriesToImport, IProgressMonitor monitor){
		List<String> errors = new ArrayList<String>();
		List<String> overview = new ArrayList<String>();
		int ok = 0;
		for (ConservationArea ca : cas){
			//figure out what folder to import into
			QueryFolder root = new QueryFolder();
			root.setRootFolder(true);
			Employee e = ImportQueryUtil.findEmployee(ca);
			if (e.getSmartUserLevel() == SmartUserLevel.ADMIN || 
					e.getSmartUserLevel() == SmartUserLevel.MANAGER ){
				root.setUuid(IQueryHibernateManager.CA_QUERY_KEY);	
			}else if (e.getSmartUserLevel() == SmartUserLevel.ANALYST){
				//store in my queries folder
				root.setUuid(IQueryHibernateManager.USER_QUERY_KEY);
			}else if (e.getSmartUserLevel() == SmartUserLevel.DATA_ENTRY){
				//data entry queries do not have access to import queries
				errors.add(MessageFormat.format(Messages.ExportQueryWizard_UserError, ca.getNameLabel(), SmartLabelProvider.getFullLabel(e)));
			}
			
			monitor.subTask(MessageFormat.format(Messages.ExportQueryWizard_ImportProgress, ca.getNameLabel()));
			int lcnt = 0;
			for (Entry<Query, File> key: queriesToImport.entrySet()){
				try {
					ImportQueryUtil.importQuery(key.getValue(), root, ca, getContainer().getShell());
					ok++;
					lcnt ++;
				} catch (Exception e1) {
					QueryPlugIn.log(e1.getMessage(), e1);
					errors.add(MessageFormat.format(Messages.ExportQueryWizard_ImportError, key.getKey().getName(), ca.getNameLabel(), e1.getMessage()));
				}
				monitor.worked(1);
				if (monitor.isCanceled()){ 
					return new Object[]{ok, errors}; 
				}
			}			
			overview.add(MessageFormat.format(Messages.ExportQueryWizard_ImportStatus, ca.getNameLabel(), lcnt, queriesToImport.size()));
		}
		errors.add(0, "\n"); //$NON-NLS-1$
		for (String x : overview){
			errors.add(0, x);
		}
		
		return new Object[]{ok, errors};
	}
	
	
	private HashMap<Query, File> exportQueriesToFile(File outputLocation, List<Object> queries, HashMap<String, Object> ops, IProgressMonitor monitor){
		boolean overwriteall = false;
		HashMap<Query, File> exportedFiles = new HashMap<Query, File>();
		for (Object qi : queries){
			monitor.worked(1);
			
			Query query = null;
			if (qi instanceof Query){
				query = (Query) qi;
				monitor.subTask(MessageFormat.format(Messages.ExportQueryWizard_ExportProgress2, new Object[]{query.getName()}));
			}else if (qi instanceof QueryEditorInput){
				monitor.subTask(MessageFormat.format(Messages.ExportQueryWizard_ExportProgress2, new Object[]{((QueryEditorInput)qi).getName()}));
				Session session = HibernateManager.openSession();
				try{
					query = QueryHibernateManager.getInstance().findQuery(session, ((QueryEditorInput)qi).getUuid(), ((QueryEditorInput)qi).getType());
				}finally{
					session.close();
				}
				if (query == null){
					MessageDialog.openError(getShell(), Messages.ExportQueryWizard_QueryNotFound, MessageFormat.format(Messages.ExportQueryWizard_QueryNotFoundMsg, new Object[]{((QueryEditorInput)qi).getName() + " [" + ((QueryEditorInput)qi).getId() + "]"})); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
			}
	
			//find query definition exporter
			IQueryExporter lexporter = null;
			for (IQueryExporter exporter : QueryExportEngine.getQueryExports(query)){
				if (exporter.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
					lexporter = exporter;
					break;
				}
			}
			//to definition exporter found for query type
			if (lexporter == null){
				MessageDialog.openError(getShell(), Messages.ExportQueryWizard_ExporterNotFound, MessageFormat.format(Messages.ExportQueryWizard_ExporterNotFoundMsg, new Object[]{query.getName() + " [" + query.getId() + "]"})); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			
			File outputFile = new File(outputLocation, URLUtils.cleanFilename(query.getName()) +"_" + query.getId() + "." + lexporter.getDefaultExtension()); //$NON-NLS-1$ //$NON-NLS-2$
			if (!overwriteall && outputFile.exists()){
				MessageDialog md = new MessageDialog(getShell(), Messages.ExportQueryWizard_OverwriteDialogTitle,
						MessageDialog.getImage(Dialog.DLG_IMG_MESSAGE_INFO),
						MessageFormat.format(Messages.ExportQueryWizard_OverwriteDialogMessage, new Object[]{outputFile.toString()}), 
						MessageDialog.INFORMATION,
						new String[]{IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.YES_LABEL, IDialogConstants.SKIP_LABEL},
						0);
				int ret = md.open();
				if (ret == 2){
					continue;
				}else if (ret == 0){
					overwriteall = true;
				}
			}

			try{
				lexporter.export(query, query.getCachedResults(), outputFile, ops, monitor);
				exportedFiles.put(query, outputFile);
			}catch (Throwable ex){
				MessageDialog.openError(getShell(), 
						Messages.ExportQueryWizard_ExportFailed, 
						MessageFormat.format(Messages.ExportQueryWizard_ExportFailedMsg + "\n\n" + ex.getLocalizedMessage(), new Object[]{query.getName() + " [" + query.getId() + "]"}));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ 
				QueryPlugIn.log(ex.getMessage(), ex);
			}
		
			if (monitor.isCanceled()){
				break;
			}
		}
		return exportedFiles;
	}
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
			page2.setPageComplete(true);
		}else if (event.getTargetPage() == page3){
			if (event.getCurrentPage() != page4){
				page3.initValues(initSelection);
				page3.setPageComplete(true);
			}
		}
	}

}
