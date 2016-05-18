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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.birt.report.engine.api.EmitterInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.query.ui.importexport.ImportQueryUtil;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.export.IExportFormat;
import org.wcs.smart.report.export.IReportExporter;
import org.wcs.smart.report.export.internal.ExportReportEngine;
import org.wcs.smart.report.export.internal.ReportDefintionExporter;
import org.wcs.smart.report.in.internal.ImportReportEngine;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.manger.ReportManager;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.RootReportFolder;


/**
 * Wizard to import query definition.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportReportWizard extends Wizard implements IPageChangingListener{
	private static final String ERROR_MSG = Messages.ExportReportHandler_ExportError;
	
	private ExportReportWizardPage page1;
	private ExportReportCaListPage page2;
	
	private boolean isMultiple;
	private List<Report> initReports;
	
	/**
	 * Creates a new wizard.
	 *
	 */
	public ExportReportWizard(boolean isMultiple, List<Report> initReports) {
		setWindowTitle(Messages.ExportReportWizard_ExportReportWizardTitle);
		
		super.setNeedsProgressMonitor(true);
		this.isMultiple = isMultiple;
		this.initReports = initReports;
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		((WizardDialog)getContainer()).addPageChangingListener(this);
		
		page1 = new ExportReportWizardPage(isMultiple, initReports);
		super.addPage(page1);
		
		page2 = new ExportReportCaListPage();
		super.addPage(page2);
	}

    public boolean canFinish() {
    	return (getContainer().getCurrentPage() == page1
    			&& getContainer().getCurrentPage().isPageComplete() &&
    			getContainer().getCurrentPage().getNextPage() == null) ||
    			(getContainer().getCurrentPage() == page2
    			&& getContainer().getCurrentPage().isPageComplete() );
    }
    
	/**
	 * Runs the import process
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		page1.updateSettings();
		IExportFormat exporter = page1.getExporter();
		File outputLocation = new File(page1.getOutputDir());
		final List<Report> selectedReports = page1.getSelectedReports();
		boolean toCa = page1.exportToConservationArea();
		final List<ConservationArea> cas = page2.getConservationAreasToExport();
		
		if (!toCa){
			if (isMultiple){
				//dir provided			
				ExportReportEngine.validateDirectory(outputLocation);
			}
		
			if (exporter.getExporter() instanceof EmitterInfo){	
				EmitterInfo outputFormat = (EmitterInfo) exporter.getExporter();
				//export reports
				try {
					if (isMultiple){
						ExportReportEngine.exportReports(selectedReports, outputLocation, null, outputFormat);
					}else{
						ExportReportEngine.exportReports(selectedReports, null, outputLocation, outputFormat);
					}
				} catch (Exception e) {
					ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
					return false;
				}
				
			}else if (exporter.getExporter() instanceof IReportExporter){
				IReportExporter rexporter = (IReportExporter) exporter.getExporter();
				try {
					if (isMultiple){
						ExportReportEngine.exportReports(selectedReports, outputLocation, null, rexporter);
					}else{
						ExportReportEngine.exportReports(selectedReports, null, outputLocation, rexporter);
					}
				} catch (Exception e) {
					ReportPlugIn.displayLog(ERROR_MSG + e.getLocalizedMessage(), e);
					return false;
				}
			}
			return true;
		}
		
		//export to conservation area
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					exportToConservationArea(monitor, selectedReports, cas);
					
				}
			});
		} catch (Exception e) {
			ReportPlugIn.displayLog(e.getMessage(), e);
			return false;
		}
		return true;
	}

	private void exportToConservationArea(IProgressMonitor monitor, List<Report> selectedReports, List<ConservationArea> cas){
		//export to ca
		File tempDir;
		try {
			tempDir = Files.createTempDirectory("smartreports").toFile(); //$NON-NLS-1$
		} catch (IOException e1) {
			ReportPlugIn.displayLog(e1.getMessage(), e1);
			return;
		}
		
		final List<String> errors = new ArrayList<String>();
		ReportDefintionExporter defexporter = new ReportDefintionExporter();
		HashMap<Report, File> exports = new HashMap<Report, File>();
		
		for (Report r : selectedReports){
			File file = new File(tempDir, r.getId() + System.nanoTime() + ".zip"); //$NON-NLS-1$
			try {
				defexporter.exportReport(file, r, null, new NullProgressMonitor());
				exports.put(r, file);
			} catch (Exception e) {
				ReportPlugIn.log(e.getMessage(), e);
				errors.add(MessageFormat.format(Messages.ExportReportWizard_ExportError, r.getName(), e.getMessage()));
			}
			
			if (monitor.isCanceled()){
				return;
			}
		}
		List<String> caInfo = new ArrayList<String>();
		for (ConservationArea ca : cas){
			Employee e = ImportQueryUtil.findEmployee(ca);
			RootReportFolder folder = null;
			if (ReportManager.canModifyCaReports()){
				folder = RootReportFolder.CA_ROOT_FOLDER;
			}else if (e.getSmartUserLevel() == SmartUserLevel.ANALYST){
				folder = RootReportFolder.USER_ROOT_FOLDER;
			}else{
				//error
			}
			int incnt = 0;
			for (Entry<Report, File> export : exports.entrySet()){
				try{
					ImportReportEngine importer = new ImportReportEngine();
					importer.importReport(export.getValue(), folder, ca);
					incnt++;
				}catch (Exception ex){
					ReportPlugIn.log(ex.getMessage(), ex);
					errors.add(MessageFormat.format(Messages.ExportReportWizard_ExportError, export.getKey().getName(), ex.getMessage()));
				}
			}
			caInfo.add(MessageFormat.format(Messages.ExportReportWizard_ImportComplete, ca.getNameLabel(), incnt, selectedReports.size() ));
		}
		ExportReportEngine.exportReports(selectedReports, tempDir, null, defexporter);
		
		errors.add(0, ""); //$NON-NLS-1$
		for (String info : caInfo){
			errors.add(0, info);
		}
		
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				WarningDialog wd = new WarningDialog(getShell(), 
						Messages.ExportReportWizard_DialogTitle, Messages.ExportReportWizard_DialogMessage, errors);
				wd.open();		
			}
			
		});
		
	}
	/**
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	@Override
	public void handlePageChanging(PageChangingEvent event) {
	}

}

