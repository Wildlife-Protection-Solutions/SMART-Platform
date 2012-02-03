package org.wcs.smart.ui.internal;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.DefaultPerspective;
import org.wcs.smart.SmartPlugIn;

public class ShowDefaultMapPerspective extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		//Open Patrol Perspective
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getWorkbench()
					.showPerspective(DefaultPerspective.ID,
							HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			SmartPlugIn.displayLog(HandlerUtil.getActiveWorkbenchWindow(event).getShell(),  "Error loading patrol perspective.", e);
		}
		return null;
	}

}
