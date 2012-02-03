package org.wcs.smart.patrol.internal.ui.properties.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.wcs.smart.patrol.internal.ui.properties.PatrolTypePropertyPage;

public class ShowPatrolTypeHandler extends AbstractHandler {

	
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		PatrolTypePropertyPage dialog = new PatrolTypePropertyPage();
		dialog.open();
		
		return null;
	}
}
