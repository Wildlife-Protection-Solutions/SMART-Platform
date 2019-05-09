package org.wcs.smart.paws.ui.config;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.query.ui.QueryPerspective;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class EditConfigHandler {

	@Execute
	public void execute(MWindow window, ConfigEditorInput input ) {
		(new ShowPerspectiveHandler()).execute(QueryPerspective.ID, window);
		
		try {
			IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = ww.getActivePage();
			page.openEditor(input, ConfigurationEditor.ID);
		} catch (PartInitException e) {
			PawsPlugIn.displayLog("Error loading configuration." + "\n\n" + e.getMessage(), e);
		}
	}
}
