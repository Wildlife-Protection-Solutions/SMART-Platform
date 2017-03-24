/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.record.importer;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.record.importer.RecordImportConfig;
import org.wcs.smart.i2.record.importer.RecordImportEngine;

/**
 * Import record from CSV wizard
 * 
 * @author Emily
 *
 */
public class ImportRecordCsvWizard extends Wizard implements IPageChangingListener  {
	
	private FileWizardPage filePage;
	private AttributeMappingWizardPage mappingPage;
	
	private RecordImportConfig config;
	
	@Inject 
	private IEventBroker eventBroker;
	
	/**
	 * Create a new wizard
	 * @param importer csv importer
	 */
	public ImportRecordCsvWizard(){
		super();
		
		setWindowTitle(Messages.ImportRecordWizard_Title);
		super.setNeedsProgressMonitor(true);
		config = new RecordImportConfig();
	}

	@Override
	public boolean performFinish() {
		mappingPage.updateConfiguration(config);
		
		boolean r[] = new boolean[]{false};
		try{
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try{
						Integer numRecords = RecordImportEngine.INSTANCE.importRecords(config, eventBroker, monitor);
						if (numRecords == null){
							r[0] = false;
							if (monitor.isCanceled()){
								Display.getDefault().syncExec(()->{
									MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ImportRecordWizard_ImportTitle, Messages.ImportRecordWizard_CancelledMsg);
								});
							}
						}else{
							r[0] = true;
							Display.getDefault().syncExec(()->{
								MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ImportRecordWizard_ImportTitle, MessageFormat.format(Messages.ImportRecordWizard_SuccessMsg, numRecords));
							});
						}
					}catch (Exception ex){
						Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.ImportRecordWizard_ImportError,  ex.getMessage()), ex);
						r[0] = false;
					}
				}
			});
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.ImportRecordWizard_ImportError2,  ex.getMessage()), ex);
			r[0] = false;
		}
		
		return r[0];
	}

	public RecordImportConfig getImportConfiguration(){
		return this.config;
	}
	
	@Override
	public boolean canFinish(){
		if (!super.canFinish()) return false;
		
		if (getContainer().getCurrentPage() == mappingPage){
			return true;
		}
		return false;
	}
	
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	
    	filePage = new FileWizardPage();
    	mappingPage = new AttributeMappingWizardPage();
    	
    	super.addPage(filePage);
    	super.addPage(mappingPage);
    }
    
	
    
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == filePage){
			config.setFile(filePage.getFile());
			config.setDelimiter(filePage.getDelimiter());
			config.setSkipFileLine(filePage.getSkipFirstLine());
			config.setProjection(filePage.getProjection());
			config.setDateFormatString(filePage.getDateFormatStr());
		}
		
		if (event.getTargetPage() == mappingPage){
			mappingPage.initPage();
		}
	}
}
