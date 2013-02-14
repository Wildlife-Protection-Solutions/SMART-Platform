package org.wcs.smart.patrol.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.ui.PatrolPerspective;

/**
 * Handler that display the patrol perspective.
 * 
 * @author Emily
 *
 */
public class ShowPatrolPersepctiveHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		//Open Patrol Perspective
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getWorkbench()
					.showPerspective(PatrolPerspective.ID,
							HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			SmartPatrolPlugIn
					.displayLog(Messages.ShowPatrolPersepctiveHandler_Error_LoadingPatrolPerspective, e);
		}
		return null;
	}
}
