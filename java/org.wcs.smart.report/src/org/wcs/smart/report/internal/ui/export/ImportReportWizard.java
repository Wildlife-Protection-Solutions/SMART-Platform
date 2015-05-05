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
package org.wcs.smart.report.internal.ui.export;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.internal.ReportDefintionExporter;
import org.wcs.smart.report.in.internal.ImportReportEngine;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;


/**
 * Wizard to import query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportReportWizard extends Wizard implements IPageChangingListener{

	private ImportReportSourcePage page0;
	private ImportReportFilePage page1;
	private ImportReportFolderPage page2;
	private ImportReportCaPage page3;
	private ImportReportCaListPage page4;
	
	private boolean hasError = false;
	private boolean importFile = true;
	/**
	 * Creates a new wizard.
	 *
	 */
	public ImportReportWizard() {
		setWindowTitle(Messages.ImportReportWizard_Title);
		
		super.setNeedsProgressMonitor(true);
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		page0 = new ImportReportSourcePage();
		super.addPage(page0);
		
		page1 = new ImportReportFilePage();
		super.addPage(page1);
		
		page2 = new ImportReportFolderPage();
		super.addPage(page2);
		
		page3 = new ImportReportCaPage();
		super.addPage(page3);
		
		page4 = new ImportReportCaListPage();
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
				importReports();
			}
		} catch (Exception e) {
			ReportPlugIn.displayLog(Messages.ImportReportWizard_FailedMessage + "\n\n" + e.getLocalizedMessage(), e); //$NON-NLS-1$
		}
		return !hasError;
	}

	private void importReports() throws Exception{
		final Object inputFolder = page2.getFolder();
		final List<Report> reports = page4.getReports();
		
		getContainer().run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				monitor.beginTask(Messages.ImportReportWizard_TaskName, reports.size()*2);
				
				int importCnt = 0;
				ReportDefintionExporter exporter = new ReportDefintionExporter();
				for (Report report : reports){
					monitor.subTask(MessageFormat.format(Messages.ImportReportWizard_TaskProgress, new Object[]{report.getName() + " [" + report.getId() + "]"})); //$NON-NLS-1$ //$NON-NLS-2$
					
					File outputFile = null;
					try{
						outputFile = File.createTempFile(report.getId(), ".xml"); //$NON-NLS-1$
						exporter.exportReport(outputFile, report, null, new NullProgressMonitor());//new SubProgressMonitor(monitor, 2));
						monitor.worked(1);
						
						if (importReport(outputFile, inputFolder)){
							importCnt++;
						}
						monitor.worked(1);
					}catch (Throwable ex){
						ReportPlugIn.displayLog(MessageFormat.format(Messages.ImportReportWizard_ErrorMsg + "\n\n" + ex.getLocalizedMessage(), new Object[]{report.getName() + " [" + report.getId() + "]"}), ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}finally{
						if (outputFile != null){
							outputFile.delete();
						}
					}
					if (monitor.isCanceled()){
						break;
					}
				}
				
				if (monitor.isCanceled()){
					final int cnt = importCnt;
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(getShell(), Messages.ImportReportWizard_CancelledTitle, MessageFormat.format(Messages.ImportReportWizard_CancelledMessage, new Object[]{cnt, reports.size()}));
						}});					
				}else{
					final int cnt = importCnt;
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(getShell(), Messages.ImportReportWizard_ImportComplete, MessageFormat.format(Messages.ImportReportWizard_completeMsg, new Object[]{cnt, reports.size()}));
						}});
				}
				
			}
		});
		
		
	}
			
	private void importFiles(final List<File> files) throws Exception {
		
		final Object inputFolder = page2.getFolder();
		getContainer().run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
			
				monitor.beginTask(Messages.ImportReportWizard_TaskName2, files.size());
				int importCnt = 0;
				
				for (File f : files){
					monitor.subTask(MessageFormat.format(Messages.ImportReportWizard_TaskProgress2, new Object[]{f.getName()}));
					monitor.worked(1);
					try{
						if (importReport(f, inputFolder)){
							importCnt++;	
						}
					}catch (Exception ex){
						ReportPlugIn.displayLog(MessageFormat.format(Messages.ImportReportWizard_FileError, new Object[]{f.getAbsolutePath()}) + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
					}	
					if (monitor.isCanceled()){
						break;
					}
				}

				if (monitor.isCanceled()){
					final int cnt = importCnt;
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(getShell(), Messages.ImportReportWizard_CancelledTitle, MessageFormat.format(Messages.ImportReportWizard_CancelledMessage, new Object[]{cnt, files.size()}));
						}});					
				}else{
					final int cnt = importCnt;
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(getShell(), Messages.ImportReportWizard_ImportComplete1, MessageFormat.format(Messages.ImportReportWizard_completeMsg1, new Object[]{cnt, files.size()}));
						}});
				}
				
			}
			
		});
	}
	
	private boolean importReport(File file, Object reportFolder) throws Exception{
		ImportReportEngine importer = new ImportReportEngine();
		return importer.importReport(file, reportFolder);
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
		this.getText().setBackground(getText().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
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