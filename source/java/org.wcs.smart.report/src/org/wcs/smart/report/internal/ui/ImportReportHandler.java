package org.wcs.smart.report.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.internal.ui.export.ImportReportWizard;

public class ImportReportHandler extends AbstractHandler {

	private static final String ERROR_MSG = Messages.ImportReportHandler_Error_ImportingReport;

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
					.displayLog(Messages.ImportReportHandler_Error_LoadingPerspective, e);
		}
		
		final Shell parent = HandlerUtil.getActiveShell(event);
		ImportReportWizard wizard = new ImportReportWizard();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.open();
		
//		
//		FileDialog fd = new FileDialog(parent);
//		fd.setFilterNames(new String[]{Messages.ImportReportHandler_ZipFileFilterName, Messages.ImportReportHandler_AllFilesFilterName});
//		fd.setFilterExtensions(new String[]{"*.zip", "*.*"});  //$NON-NLS-1$//$NON-NLS-2$
//		fd.setText(Messages.ImportReportHandler_SelectReportLabel);
//		String file = fd.open();
//		if (file == null){
//			return null;
//		}
//		
//		final File reportFile = new File(file);
//		if (!reportFile.exists()){
//			MessageDialog.openError(parent, Messages.ImportReportHandler_Error_DialogTitle, 
//					MessageFormat.format(Messages.ImportReportHandler_Error_FileNotFound, new Object[]{ reportFile.getAbsolutePath()}));
//			return null;
//		}
//		
//		ProgressMonitorDialog pmd = new ProgressMonitorDialog(parent);
//		try {
//			pmd.run(true, false, new IRunnableWithProgress() {
//				
//				@Override
//				public void run(IProgressMonitor monitor) throws InvocationTargetException,
//						InterruptedException {
//					ImportReportEngine importer = new ImportReportEngine();
//					try{
//						if (importer.importReport(reportFile)){
//							Display.getDefault().syncExec(new Runnable(){
//								@Override
//								public void run() {
//									MessageDialog.openInformation(parent, Messages.ImportReportHandler_ImportOk_DialogTitle, Messages.ImportReportHandler_ImportOk);
//								}});
//						}
//					}catch (Exception ex){
//						ReportPlugIn.displayLog(ERROR_MSG + ex.getLocalizedMessage(), ex);
//					}
//					
//				}
//			});
//		} catch (Exception ex) {
//			ReportPlugIn.displayLog(ERROR_MSG + ex.getLocalizedMessage(), ex);
//		}
		
		
		return null;
	}

}
