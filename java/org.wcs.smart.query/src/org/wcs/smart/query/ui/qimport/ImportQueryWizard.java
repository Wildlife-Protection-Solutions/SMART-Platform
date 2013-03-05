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
package org.wcs.smart.query.ui.qimport;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.InputDialog;
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
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.hibernate.IQueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.qimport.QueryImporter;
import org.wcs.smart.util.SmartUtils;


/**
 * Wizard to import query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportQueryWizard extends Wizard implements IPageChangingListener{

	private ImportQueryFilePage page1;
	private ImportQueryFolderPage page2;
	private boolean hasError = false;
	
	/**
	 * Creates a new wizard.
	 *
	 */
	public ImportQueryWizard() {
		setWindowTitle(Messages.ImportQueryWizard_Title);
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		page1 = new ImportQueryFilePage();
		super.addPage(page1);
		
		page2 = new ImportQueryFolderPage();
		super.addPage(page2);
	}

	
	/**
	 * Runs the import process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		hasError = false;
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
				
					File f = page1.getFile();
					QueryFolder qf = page2.getFolder();
					
					QueryImporter importer = new QueryImporter();
					try{
						Query query = importer.importQuery(f);
						
						List<String> warnings = importer.getWarnings();
						if (warnings.size() > 0){
							StringBuilder sb = new StringBuilder();
							for (String str: warnings){
								sb.append(str);
								sb.append(SmartUtils.LINE_SEPARATOR);
								sb.append(SmartUtils.LINE_SEPARATOR);
							}
							
							ConfirmInputDialog dialog = new ConfirmInputDialog(
									getContainer().getShell(),
									Messages.ImportQueryWizard_Confirm_DialogTitle,
									Messages.ImportQueryWizard_Confirm_DialogMessage,
									sb.toString(), null);
							if (dialog.open() != ConfirmInputDialog.OK){
								return;
							}	
						}
						
						
						if (!qf.isRootFolder()){
							query.setFolder(qf);
							query.setIsShared(qf.getEmployee() == null);
						}else if (qf.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY)){
							query.setIsShared(true);
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
						QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_ADDED, query);
						
						//open query in editor
						QueryInput qi = new QueryInput(query);
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(qi, query.getType().getEditorId());
					}catch (Exception ex){
						QueryPlugIn.displayLog(Messages.ImportQueryWizard_CouldNotImportError + ex.getLocalizedMessage(), ex);
					}
					
				}
			});
		} catch (Exception e) {
			QueryPlugIn.displayLog(Messages.ImportQueryWizard_ImportFailed + e.getLocalizedMessage(), e);
		}
		return !hasError;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
		}else{
			page2.setPageComplete(false);
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