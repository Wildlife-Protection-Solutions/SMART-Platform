package org.wcs.smart.common.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;


/**
 * Action that is called when some plug-in is being uninstalled.
 * Class contains logic that allows to distinguish if this is plug-in removal or upgrade.
 * 
 * @author elitvin
 * @since 3.0.0
 */
@SuppressWarnings("restriction")
public abstract class UninstallProvisioningAction extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		IInstallableUnit upgradeTo = null;
		Object operand = parameters.get("operand"); //$NON-NLS-1$
		try {
			if (operand instanceof InstallableUnitOperand)
				upgradeTo = ((InstallableUnitOperand) operand).second();
		}
		catch (Throwable e) {
			// Ignore class not found in case InstallableUnitOperand is missing
		}
		if (upgradeTo == null) {
			// We have an unistallation, not an upgrade
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Uninstall 3.0.0",
							"OnUninstallAction - removing");
				}
			});
			performRemove();
		} else {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Uninstall 3.0.0",
							"OnUninstallAction - upgrading");
				}
			});
			performUpgrade();
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	protected abstract void performRemove();

	protected void performUpgrade() {
		//nothing by default
	}
}
