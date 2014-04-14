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
package org.wcs.smart.entity.ui.importwizard;

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
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityCsvImporter;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Wizard for importing entities from a csv file.
 * 
 * @author Emily
 *
 */
public class ImportEntitiesWizard extends Wizard implements IPageChangingListener {


	private FileWizardPage filePage;
	private AttributeMappingWizardPage mapPage;
	private EntityCsvImporter importer;
	private String error;
	
	/**
	 * Create a new wizard
	 * @param importer csv importer
	 */
	public ImportEntitiesWizard(EntityCsvImporter importer){
		super();
		
		this.importer = importer;
		setWindowTitle(Messages.ImportEntitiesWizard_WizardTitle);
		super.setNeedsProgressMonitor(true);
	}
	

	@Override
	public boolean performFinish() {
		SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_DELIMITER_KEY, String.valueOf(importer.getConfiguration().getDelimiter()));
		//update the configuration
		mapPage.updateConfiguration();
		
		final boolean[] error = new boolean[]{false};
		
		//import entities
		try {
			
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ImportEntitiesWizard_ProgressMsg, 110);
					Session s = HibernateManager.openSession();
					try{
						if (!importer.importEntities(s, monitor)){
							error[0] = true;
						}
					}catch (Exception ex){
						EntityPlugIn.displayLog(Messages.ImportEntitiesWizard_ErrorMesg + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						error[0] = true;
					}finally{
						s.close();
						monitor.done();
					}
					
					
				}
			});
		} catch (Exception ex) {
			EntityPlugIn.log("Could no import entities." + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		if (error[0]){
			return false;
		}
		MessageDialog.openInformation(getShell(), Messages.ImportEntitiesWizard_SuccessTitle, Messages.ImportEntitiesWizard_SuccessMsg);
		return true;
	}

	@Override
	public boolean canFinish(){
		if (!super.canFinish()) return false;
		
		if (getContainer().getCurrentPage() == mapPage){
			return true;
		}
		return false;
	}
	
    public void addPages() {
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    	
    	filePage = new FileWizardPage();
    	mapPage = new AttributeMappingWizardPage(importer);
    	
    	super.addPage(filePage);
    	super.addPage(mapPage);
    }
    
	
    
	@Override
	public void handlePageChanging(PageChangingEvent event) {
		if (event.getCurrentPage() == filePage){
			final File f = new File(filePage.getFile());
			if (!f.exists()){
				MessageDialog.openError(getShell(), Messages.ImportEntitiesWizard_ErrorDialogTitle, 
					MessageFormat.format(Messages.ImportEntitiesWizard_FileNotFoundError, new Object[]{f.toString()}));
				event.doit = false;
				return;
			}
			
			importer.setFile(f);
			try{
				importer.getConfiguration().setDelimitier(filePage.getDelimiter());
			}catch (Exception ex){
				MessageDialog.openError(getShell(), Messages.ImportEntitiesWizard_ErrorDialogTitle, ex.getMessage());
				event.doit = false;
				return;
			}
			
			error = null;
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.ImportEntitiesWizard_ReadingProgress, 10);
						monitor.worked(2);
						try{
							importer.getFileHeaders();
						}catch (Exception ex){
							error = MessageFormat.format(Messages.ImportEntitiesWizard_ReadingError, new Object[]{f.toString()}) + "\n\n" + ex.getMessage(); //$NON-NLS-1$
						}
						monitor.worked(5);
						final Session s = HibernateManager.openSession();
						try{
							Display.getDefault().syncExec(new Runnable(){
								@Override
								public void run() {
									try{
										mapPage.initFields(s);
									}catch (Exception ex){
										error = MessageFormat.format(Messages.ImportEntitiesWizard_ProcessingError, new Object[]{f.toString()}) + "\n\n" + ex.getMessage(); //$NON-NLS-1$
									}
							}});
							monitor.worked(3);
							
						}finally{
							s.close();
							monitor.done();
						}
						
					}
				});
			} catch (Exception ex) {
				error = MessageFormat.format(Messages.ImportEntitiesWizard_ReadingError, new Object[]{f.toString()}) + "\n\n" + ex.getMessage(); //$NON-NLS-1$
			}
			
			if (error != null){
				MessageDialog.openError(getShell(), Messages.ImportEntitiesWizard_ErrorDialogTitle, error); 
				event.doit = false;
				return;
			}
		}
	}

}
