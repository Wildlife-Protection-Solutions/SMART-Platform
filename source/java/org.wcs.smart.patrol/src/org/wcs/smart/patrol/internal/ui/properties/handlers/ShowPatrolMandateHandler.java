package org.wcs.smart.patrol.internal.ui.properties.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.wcs.smart.patrol.internal.ui.properties.PatrolMandatePropertyPage;

public class ShowPatrolMandateHandler extends AbstractHandler {

	
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PatrolMandatePropertyPage dialog = new PatrolMandatePropertyPage();
		dialog.open();
		
		return null;
	}
}
