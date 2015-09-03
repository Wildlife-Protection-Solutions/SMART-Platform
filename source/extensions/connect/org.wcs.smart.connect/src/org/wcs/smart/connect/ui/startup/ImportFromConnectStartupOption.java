package org.wcs.smart.connect.ui.startup;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.IAdvancedStartupOption;

public class ImportFromConnectStartupOption implements IAdvancedStartupOption {
	public static final int ORDER = 40;
	@Override
	public String getLabel() {
		return "Download a Conservation Area from a SMART Connect Instance";
	}

	@Override
	public Status performTask(Shell activeShell) throws Exception {
		WizardDialog wd = new WizardDialog(activeShell, new DownloadConnectWizard());
		wd.open();
		return Status.OK;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
