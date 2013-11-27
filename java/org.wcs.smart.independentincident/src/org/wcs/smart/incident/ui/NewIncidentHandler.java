package org.wcs.smart.incident.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.incident.ui.newwizard.NewIncidentWizard;

public class NewIncidentHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		WizardDialog wd = new WizardDialog(HandlerUtil.getActiveShell(event), new NewIncidentWizard());
		wd.open();
		
		return null;
	}

}
