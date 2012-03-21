package org.wcs.smart.patrol.internal.ui.views;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.PlatformUI;

public class ShowPatrolFilterHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PatrolListView view = (PatrolListView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(PatrolListView.ID);
		
		PatrolFilterDialog pfd = new PatrolFilterDialog(view.getViewSite().getShell(), view);
		int ret = pfd.open();
		if (ret == Dialog.OK) {
			view.updateContent();
		}
		return null;
	}
}
