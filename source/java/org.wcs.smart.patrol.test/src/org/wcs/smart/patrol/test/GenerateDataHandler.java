package org.wcs.smart.patrol.test;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

public class GenerateDataHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		PatrolDataGenerator gen = new PatrolDataGenerator();
		gen.generatePatrols();
		return null;
	}

}
