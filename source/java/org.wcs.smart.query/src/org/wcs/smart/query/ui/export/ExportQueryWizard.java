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
package org.wcs.smart.query.ui.export;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.export.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard for exporting query results.
 * @author Emily
 * @since 1.0.0
 */
public class ExportQueryWizard extends Wizard implements IPageChangingListener{

	private static final String EXPORT_FAILED_MGS = Messages.ExportQueryWizard_ExportFailedError;

	private static final String EXPORT_DIALOGTITLE = Messages.ExportQueryWizard_ExportDialogTitle;

	private Query query;

	private ExportQueryTypePage page1;
	private ExportQueryLocationPage page2;
	private boolean hasError = false;
	
	/**
	 * Creates a new wizard.
	 *
	 * @param data the data to export
	 * @param columns the query columns to export
	 * @param queryName the query name
	 */
	public ExportQueryWizard(Query query) {
		setWindowTitle(Messages.ExportQueryWizard_WindowTitle);
		setDialogSettings(QueryPlugIn.getDefault().getDialogSettings());
		this.query = query;
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		page1 = new ExportQueryTypePage();
		super.addPage(page1);
		
		page2 = new ExportQueryLocationPage();
		super.addPage(page2);
	}


	/**
	 * @return the query exporter for the format selected
	 * on the first query page.
	 */
	public IQueryExporter getQueryExporter(){
		return page1.getQueryExporter();
	}
	
	/**
	 * @return the query name of
	 * the query being exported
	 */
	public String getQueryName(){
		return this.query.getName();
	}
	
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
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						IQueryExporter exporter = getQueryExporter();
						if (exporter == null){
							hasError = true;
							return;
						}
						File outputFile = page2.getFile();
						
						if (!outputFile.getParentFile().exists()){
							boolean create = MessageDialog.openQuestion(getShell(), Messages.ExportQueryWizard_DialogTitle, MessageFormat.format(Messages.ExportQueryWizard_DirectoryDoesNotExist, new Object[]{outputFile.getParent()}));
							if (!create){
								hasError = true;
								return;
							}else{
								if (!SmartUtils.createDirectory(outputFile.getParentFile())){
									hasError = true;
									return;
								}
							}
						}
						
						if (outputFile.exists()){
							if (!MessageDialog.openConfirm(getShell(), 
									Messages.ExportQueryWizard_OverwriteDialogTitle, 
									MessageFormat.format(Messages.ExportQueryWizard_OverwriteDialogMessage, new Object[]{outputFile.toString()}))){
								hasError = true;
								return;
							}
						}
//						exporter.setData(data,  columns, outputFile, query);
						exporter.export(query, outputFile, monitor);
						
						page1.performFinish();
						page2.performFinish();
						
						if (monitor.isCanceled()){
							MessageDialog.openInformation(
									Display.getDefault().getActiveShell(), EXPORT_DIALOGTITLE,
									Messages.ExportQueryWizard_ExportCancelled_DialogMessage);
						}else{
							MessageDialog.openInformation(
								Display.getDefault().getActiveShell(), EXPORT_DIALOGTITLE,
								Messages.ExportQueryWizard_ExportOk_DialogMessage);
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
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getTargetPage() == page2){
			page2.initValues();
			page2.setPageComplete(true);
		}else{
			page2.setPageComplete(false);
		}
	}

}
