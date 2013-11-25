package org.wcs.smart.patrol.internal.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.wcs.smart.observation.ui.FieldDataPerspective;
import org.wcs.smart.patrol.internal.ui.views.PatrolListView;

/**
 * Handler that display the patrol perspective.
 * 
 * @author Emily
 *
 */
public class ShowPatrolPersepctiveHandler extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		//Open Field Data Perspective
		FieldDataPerspective.openPerspective(PatrolListView.ID);
		return null;
	}
}
