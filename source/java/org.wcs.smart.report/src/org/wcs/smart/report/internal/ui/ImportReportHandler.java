package org.wcs.smart.report.internal.ui;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.internal.ui.export.ImportReportWizard;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class ImportReportHandler  {

	@Execute
	public void execute(Shell activeShell, EModelService mService, MWindow activeWindow){
		
		(new ShowPerspectiveHandler()).execute(ReportViewerPerspective.ID, mService, activeWindow);
		
		ImportReportWizard wizard = new ImportReportWizard();
		WizardDialog dialog = new WizardDialog(activeShell, wizard);
		dialog.open();
	}
	
	public static class ImportReportHandlerWrapper extends DIHandler<ImportReportHandler>{
		public ImportReportHandlerWrapper(){
			super(ImportReportHandler.class);
		}
	}

}
