package org.wcs.smart.ui.internal.backup;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.handlers.HandlerUtil;

public class AutoBackupConfigHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final AutoBackupDialog dialog = new AutoBackupDialog(HandlerUtil.getActiveShell(event),"SMART System Automitic Backup Settings", "Update the following settings and select OK", "Save");
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}
		return null;
	}

}
