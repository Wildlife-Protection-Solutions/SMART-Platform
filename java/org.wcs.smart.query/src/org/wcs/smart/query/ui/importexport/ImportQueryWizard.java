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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.filter.SmartProgressMonitor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.importexport.QueryImportEngine;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.OpenQueryHandler;
import org.wcs.smart.util.SharedUtils;


/**
 * Wizard to import query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportQueryWizard extends Wizard implements IPageChangingListener{

	private ImportQuerySourcePage page0;
	private ImportQueryFilePage page1;
	private ImportQueryFolderPage page2;
	private ImportQueryCaPage page3;
	private ImportQueryCaListPage page4;
	
	private boolean hasError = false;
	private boolean importFile = true;
	/**
	 * Creates a new wizard.
	 *
	 */
	public ImportQueryWizard() {
		setWindowTitle(Messages.ImportQueryWizard_Title1);
		
		super.setNeedsProgressMonitor(true);
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		page0 = new ImportQuerySourcePage();
		super.addPage(page0);
		
		page1 = new ImportQueryFilePage();
		super.addPage(page1);
		
		page2 = new ImportQueryFolderPage();
		super.addPage(page2);
		
		page3 = new ImportQueryCaPage();
		super.addPage(page3);
		
		page4 = new ImportQueryCaListPage();
		super.addPage(page4);
	}

    public boolean canFinish() {
    	return getContainer().getCurrentPage() == page2 && getContainer().getCurrentPage().isPageComplete();
    }
    
	/**
	 * Runs the import process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		hasError = false;
		try {
			if (importFile){
				importFiles(page1.getFiles());
			}else{
				importQueries();
			}
		} catch (Exception e) {
			QueryPlugIn.displayLog(Messages.ImportQueryWizard_ImportFailed + e.getLocalizedMessage(), e);
		}
		return !hasError;
	}

	private void importQueries() throws Exception{
		getContainer().run(false, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
		
				List<QueryEditorInput> queries = page4.getQueries();
				
				monitor.beginTask(Messages.ImportQueryWizard_ImportingTaskName, queries.size());
				int importCnt = 0;
				
				Query firstQuery = null;
				QueryFolder qf = page2.getFolder();
				
				for (QueryEditorInput qi : queries){
					monitor.worked(1);
					monitor.subTask(MessageFormat.format(Messages.ImportQueryWizard_ImportingProgress, new Object[]{qi.getName()}));
					
					Session s = HibernateManager.openSession();
					Query query = null;
					IQueryExporter def = null;
					try{
						query = (Query) s.load(qi.getType().getHibernateClass(), qi.getUuid());
		
						List<IQueryExporter> exporter = QueryExportEngine.getQueryExports(query);
						for (IQueryExporter export : exporter){
							if (export.getId().startsWith(IQueryExporter.QUERY_DEFINTION_EXPORTER_ID)){
								def = export;
								break;
							}
						}
					}finally{
						s.close();
					}
					if (def == null){
						MessageDialog.openError(getShell(), Messages.ImportQueryWizard_ErrorDialogTitle1, MessageFormat.format(Messages.ImportQueryWizard_ImportError, new Object[]{qi.getName() + " [" + qi.getId() + "]"})); //$NON-NLS-1$ //$NON-NLS-2$
						continue;
					}

					File outputFile = null;
					try{
						outputFile = File.createTempFile(qi.getId(), ".xml"); //$NON-NLS-1$
						HashMap<String, Object> params = new HashMap<String, Object>();
						def.export(query, null, outputFile, params, new SmartProgressMonitor(monitor));
						
						Query q = importQuery(outputFile, qf);
						if (q != null){
							if (firstQuery == null){
								firstQuery = q;
							}
							importCnt++;
						
							QueryEventManager.getInstance().fireQueryAdded(q);
						}
					}catch (Throwable ex){
						MessageDialog.openError(getShell(), 
								Messages.ImportQueryWizard_ErrorDialogTitle, 
								MessageFormat.format(Messages.ImportQueryWizard_ImportError1 + "\n\n" + ex.getLocalizedMessage(), new Object[]{qi.getName() + " [" + qi.getId() + "]"}));    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						QueryPlugIn.log(ex.getMessage(), ex);
					}finally{
						if (outputFile != null){
							outputFile.delete();
						}
					}
				}
				
				//open query in editor if only importing a single query
				if (queries.size() == 1 && firstQuery != null){
					QueryEditorInput qi = new QueryEditorInput(firstQuery);
					(new OpenQueryHandler()).openQuery(qi);
				}else{
					MessageDialog.openInformation(getShell(), Messages.ImportQueryWizard_ImportCompleteTitle, MessageFormat.format(Messages.ImportQueryWizard_ImportCompleteMessage, new Object[]{importCnt, queries.size()}));
				}
			}
		});
		
		
	}
			
	private void importFiles(final List<File> files) throws Exception {
		
		getContainer().run(false, false, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
			
				monitor.beginTask(Messages.ImportQueryWizard_ImportingTaskName, files.size());
				
				QueryFolder qf = page2.getFolder();
				int importCnt = 0;
				
				Query firstQuery = null;
				
				for (File f : files){
					monitor.subTask(MessageFormat.format(Messages.ImportQueryWizard_ImportingProgress, new Object[]{f.getName()}));
					monitor.worked(1);
					try{
						Query q = importQuery(f, qf);
						if (q != null){
							if (firstQuery == null){
								firstQuery = q;
							}
							importCnt++;
						
							QueryEventManager.getInstance().fireQueryAdded(q);
						}
					}catch (Exception ex){
						QueryPlugIn.displayLog(MessageFormat.format(Messages.ImportQueryWizard_ErrorImportingFile, new Object[]{f.getAbsolutePath()}) + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
					}	
				
				}
				//open query in editor if only importing a single query
				if (files.size() == 1 && firstQuery != null){
					QueryEditorInput qi = new QueryEditorInput(firstQuery);
					(new OpenQueryHandler()).openQuery(qi);
				}else{
					MessageDialog.openInformation(getShell(), Messages.ImportQueryWizard_ImportCompleteTitle, MessageFormat.format(Messages.ImportQueryWizard_ImportCompleteMessage, new Object[]{importCnt, files.size()}));
				}
			}
			
		});
	}
	
	private Query importQuery(File file, QueryFolder qf) throws Exception{
		QueryImportEngine importer = new QueryImportEngine();
		Query query = importer.importQuery(file);
		
		List<String> warnings = importer.getWarnings();
		if (warnings.size() > 0){
			StringBuilder sb = new StringBuilder();
			for (String str: warnings){
				sb.append(str);
				sb.append(SharedUtils.LINE_SEPARATOR);
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
		
			ConfirmInputDialog dialog = new ConfirmInputDialog(
					getContainer().getShell(),
					Messages.ImportQueryWizard_Confirm_DialogTitle,
					Messages.ImportQueryWizard_Confirm_DialogMessage,
					sb.toString(), null);
			if (dialog.open() != ConfirmInputDialog.OK){
				//skip this query
				return null;
			}	
		}
	
	
		if (!qf.isRootFolder()){
			query.setFolder(qf);
			query.setIsShared(qf.getEmployee() == null);
		}else if (qf.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY)){
			query.setIsShared(true);
		}
	
		//set the owner
		if (query.getIsShared() && SmartDB.isMultipleAnalysis()){
			//shared queries in the cross-ca analysis do not have a user
			query.setOwner(SmartDB.getSharedEmployee());
		}else{
			query.setOwner(SmartDB.getCurrentEmployee());
		}
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			//generate id
			query.setId(QueryHibernateManager.getInstance().generateQueryId(session));
			session.save(query);
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			throw ex;
		}finally{
			session.close();
		}				
		
		return query;
	}
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
		}else if (event.getTargetPage() == page1){
			importFile = true;
		}else if (event.getTargetPage() == page4){
			page4.initValues();
			importFile = false;
		}
	}

}

class ConfirmInputDialog extends InputDialog{

	/**
	 * @param parentShell
	 * @param dialogTitle
	 * @param dialogMessage
	 * @param initialValue
	 * @param validator
	 */
	public ConfirmInputDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String initialValue,
			IInputValidator validator) {
		super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
		
		
	}
	
	@Override
	public int getInputTextStyle(){
		return SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP ;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Control res = super.createDialogArea(parent);
		((GridData)this.getText().getLayoutData()).heightHint = 200;
		((GridData)this.getText().getLayoutData()).widthHint = 500;
		this.getText().setEditable(false);
		this.getText().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		return res;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		this.getText().clearSelection();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}