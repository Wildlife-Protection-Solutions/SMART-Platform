package org.wcs.smart.query.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.query.QueryPlugIn;

public class ShowQueryPersepctiveHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		//Open Patrol Perspective
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getWorkbench()
					.showPerspective(QueryPerspective.ID,
							HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			QueryPlugIn.displayLog("Error loading Query perspective.", e);
		}
		return null;
	}

}
