package org.wcs.smart.query.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.QueryResultsEditor;
import org.wcs.smart.query.ui.QueryResultsInput;

public class CreateQueryHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		QueryResultsInput input = new QueryResultsInput();
		
		//Open Editor Perspective
		try {
			IEditorPart openedPage  = null;
			IWorkbenchPage page = null;
			try {
				page =  PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				openedPage = page.openEditor(input, QueryResultsEditor.ID);						
			} catch (Throwable t) {
				QueryPlugIn.displayLog(t.getMessage(), t);
			}
		} catch (Exception e) {
			QueryPlugIn.displayLog("Error loading query editor.", e);
		}
		return null;
	}

}
