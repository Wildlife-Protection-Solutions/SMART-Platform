package org.wcs.smart.paws.ui;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.run.RunEditor;
import org.wcs.smart.paws.ui.run.RunEditorInput;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.QueryPerspective;
import org.wcs.smart.ui.ShowPerspectiveHandler;

public class ShowRunHandler {
	
	@Execute
	public void execute(MWindow window, PawsRun pawsRun ) {
		(new ShowPerspectiveHandler()).execute(QueryPlugIn.getActivePerspectiveId(), window);
		
		try {
			IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = ww.getActivePage();
			RunEditorInput rinput = new RunEditorInput(pawsRun);
			page.openEditor(rinput, RunEditor.ID);
		} catch (PartInitException e) {
			PawsPlugIn.displayLog("Error loading configuration." + "\n\n" + e.getMessage(), e);
		}
	}
}
