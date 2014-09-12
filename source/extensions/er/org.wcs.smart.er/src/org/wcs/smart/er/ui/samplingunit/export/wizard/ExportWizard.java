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
package org.wcs.smart.er.ui.samplingunit.export.wizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.samplingunit.export.CsvSamplingUnitExporter;
import org.wcs.smart.er.ui.samplingunit.export.ISamplingUnitExporter;
import org.wcs.smart.er.ui.samplingunit.export.ShpSamplingUnitExporter;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Import sampling unit wizard.
 * 
 * @author Emily
 *
 */
public class ExportWizard extends Wizard implements IPageChangingListener{

	
	private SurveyDesign surveyDesign;
	
	private FormatPage formatPage;
	private LocationPage locationPage;
	private TypePage typePage;
	
	private boolean canFinish = false;
	
	/**
	 * Creates a new wizard
	 */
	public ExportWizard(SurveyDesign surveyDesign){
		setNeedsProgressMonitor(true);
		this.surveyDesign = surveyDesign;
		setDialogSettings(SmartPlugIn.getDefault().getDialogSettings());
	}
	
	@Override
	public void dispose(){
		super.dispose();
		
	}
	public boolean canFinish(){
		if (canFinish){
			return super.canFinish();
		}
		return false;
	}
	
	@Override
	public boolean performFinish() {
		final File dir = locationPage.getDirectory();
		if (!dir.exists()){
			if (!MessageDialog.openQuestion(getShell(), Messages.ExportWizard_ExportDialogTitle, 
				MessageFormat.format(Messages.ExportWizard_DirectoryMessage, new Object[]{dir.getAbsolutePath()}))){
				return false;
			}
			try{
				FileUtils.forceMkdir(dir);
			}catch(IOException ex){
				EcologicalRecordsPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
		
		final HashMap<Object, Object> options = new HashMap<Object, Object>();
		ISamplingUnitExporter lexporter = null;
		if (formatPage.isShapefile()){
			lexporter = new ShpSamplingUnitExporter();
		}else{
			lexporter = new CsvSamplingUnitExporter();
			options.put(CsvSamplingUnitExporter.DELIMETER_KEY, formatPage.getDelimiter());
		}
		final ISamplingUnitExporter exporter = lexporter;
		
		final boolean exportPlots = typePage.exportPlots();
		final boolean exportTransects = typePage.exportTransect();
		final boolean exportRecon = typePage.exportRecon();
		try{
			getContainer().run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ExportWizard_ProgressLabel, 2);
					Session session = HibernateManager.openSession();
					try {
						if (exportPlots) {
							File plotFile = new File(dir, surveyDesign.getName() + "_" + "plots" + "." + exporter.getFileExtension()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							options.put(ISamplingUnitExporter.SU_TYPE_KEY, SamplingUnitType.PLOT);
							exporter.exportFile(plotFile, surveyDesign, session, options, new SubProgressMonitor(monitor, 1));
						}else{
							monitor.worked(1);
						}
						if (exportTransects) {
							options.put(ISamplingUnitExporter.SU_TYPE_KEY, SamplingUnitType.TRANSECT);
							File transectFile = new File(dir, surveyDesign.getName() + "_" + "transects" + "." + exporter.getFileExtension()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							exporter.exportFile(transectFile, surveyDesign, session, options, new SubProgressMonitor(monitor, 1));
						}else{
							monitor.worked(1);
						}
						if (exportRecon){
							options.put(ISamplingUnitExporter.SU_TYPE_KEY, SamplingUnitType.RECON);
							File reconFile = new File(dir, surveyDesign.getName() + "_" + "reconnaissance" + "." + exporter.getFileExtension()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							exporter.exportFile(reconFile, surveyDesign, session, options, new SubProgressMonitor(monitor, 1));
						}else{
							monitor.worked(1);
						}
					} catch (Exception ex) {
						EcologicalRecordsPlugIn.displayLog(
								Messages.ExportWizard_ExportError + "\n\n" //$NON-NLS-1$
										+ ex.getMessage(), ex);
					} finally {
						session.close();
						monitor.done();
					}
				}
			});
		}catch(Exception ex){
			
		}
		return true;
	}
	
	/**
     * The <code>Wizard</code> implementation of this <code>IWizard</code>
     * method does nothing. Subclasses should extend if extra pages need to be
     * added before the wizard opens. New pages should be added by calling
     * <code>addPage</code>.
     */
	public void addPages() {
    	setWindowTitle(Messages.ExportWizard_WindowTitle);
    	
    	typePage = new TypePage();
    	formatPage = new FormatPage();
    	locationPage = new LocationPage();
    	
    	super.addPage(formatPage);
    	super.addPage(typePage);
    	super.addPage(locationPage);
    	
    	((WizardDialog) getContainer()).addPageChangingListener(this);
    }

	@Override
	public void handlePageChanging(PageChangingEvent event) {
		canFinish = event.getTargetPage() == locationPage;
	}

}
