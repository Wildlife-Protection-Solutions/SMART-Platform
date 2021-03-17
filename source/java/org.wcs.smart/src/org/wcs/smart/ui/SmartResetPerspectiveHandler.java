 
package org.wcs.smart.ui;

import javax.inject.Inject;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.ui.internal.handlers.ResetPerspectiveHandler;

public class SmartResetPerspectiveHandler {
	@Execute
	public void execute(ExecutionEvent event) {
		(new ResetPerspectiveHandler()).execute(event);

	}
		
}