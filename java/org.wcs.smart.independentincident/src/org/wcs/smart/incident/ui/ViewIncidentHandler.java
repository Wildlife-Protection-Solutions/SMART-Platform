package org.wcs.smart.incident.ui;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.wcs.smart.observation.ui.FieldDataPerspective;


public class ViewIncidentHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FieldDataPerspective.openPerspective(IndIncidentListView.ID);
		return null;
	}

}
