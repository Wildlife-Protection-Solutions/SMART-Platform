package org.wcs.smart.connect.ui.startup;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.IAdvancedStartupOption;

public class SyncMultiStartupOption implements IAdvancedStartupOption {
	
	public static final int ORDER = 41;
	
	@Override
	public String getLabel() {
		return "Sync multiple Conservation Areas with SMART Connect";
	}

	@Override
	public Status performTask(Shell activeShell) throws Exception {
		WizardDialog wd = new WizardDialog(activeShell, new SyncMultipleCaWizard());
		wd.open();
		return Status.OK;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
