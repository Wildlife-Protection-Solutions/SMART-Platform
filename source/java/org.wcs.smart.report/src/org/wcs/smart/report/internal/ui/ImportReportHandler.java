package org.wcs.smart.report.internal.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.in.internal.ImportReportEngine;

public class ImportReportHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			String activeId = HandlerUtil.getActivePart(event).getSite().getPage().getPerspective().getId();
			if (!activeId.equals(ReportViewerPerspective.ID)){
				//show report perspective 
				HandlerUtil
				.getActiveWorkbenchWindow(event)
				.getWorkbench()
				.showPerspective(ReportViewerPerspective.ID,
						HandlerUtil.getActiveWorkbenchWindow(event));	
			}
			
		} catch (WorkbenchException e) {
			ReportPlugIn
					.displayLog("Error loading query perspective.", e);
		}
		
		final Shell parent = HandlerUtil.getActiveShell(event);
		
		FileDialog fd = new FileDialog(parent);
		fd.setFilterNames(new String[]{"zip (*.zip)", "All Files (*.*)"});
		fd.setFilterExtensions(new String[]{"*.zip", "*.*"});
		fd.setText("Select report file to import.");
		String file = fd.open();
		if (file == null){
			return null;
		}
		
		final File reportFile = new File(file);
		if (!reportFile.exists()){
			MessageDialog.openError(parent, "Error", "File " + reportFile.getAbsolutePath() + " not found.");
			return null;
		}
		
		//TODO: run in progress monitor dialog
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(parent);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					ImportReportEngine importer = new ImportReportEngine();
					try{
						if (importer.importReport(reportFile)){
							Display.getDefault().syncExec(new Runnable(){
								@Override
								public void run() {
									MessageDialog.openInformation(parent, "Import", "Report Imported Successfully");
								}});
						}
					}catch (Exception ex){
						ReportPlugIn.displayLog("Could not import report file: " + ex.getMessage(), ex);
					}
					
				}
			});
		} catch (Exception ex) {
			ReportPlugIn.displayLog("Error importing report: " + ex.getMessage(), ex);
		}
		
		
		return null;
	}

}
