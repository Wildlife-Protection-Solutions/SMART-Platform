package org.wcs.smart.qa.ui.handler;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.ui.view.ValidationResultsEditor;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class NewManualValidationHandler {

	@Execute
	public void createNewRecord(IEclipseContext context){
		(new ShowPerspectiveHandler()).execute("org.wcs.smart.observation.FieldDataPerspective", context.get(MWindow.class));
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(ValidationResultsEditor.MANUAL_VALIDATION_INPUT, ValidationResultsEditor.ID);
		} catch (PartInitException e) {
			QaPlugIn.displayLog("Error loading manual data validation UI.", e);
		}
	}
	
	// E3
	public static class NewManualValidationHandlerWrapper extends DIHandler<NewManualValidationHandler> {
		public NewManualValidationHandlerWrapper() {
			super(NewManualValidationHandler.class);
		}
	}
}
