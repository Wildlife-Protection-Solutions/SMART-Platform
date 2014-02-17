package org.wcs.smart.p2.common.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Action that is called when some plug-in is being installed.
 * Class contains logic that allows to distinguish if this is plug-in fresh install or upgrade.
 * 
 * Plugins should extend InstallProvisioningAction only if there 
 * is a difference in install and upgrade otherwise they should extend
 * ProvisioningAction directly. If you extend InstallProvisioningAction you 
 * will have to implement both performUpgrade() and performNewInstall() to do the same item. 

 * @author elitvin
 * @since 3.0.0
 */
@SuppressWarnings("restriction")
public abstract class InstallProvisioningAction extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		IInstallableUnit iu = (IInstallableUnit) parameters.get("iu"); //$NON-NLS-1$
		IInstallableUnit upgradeFrom = null;
		Object operand = parameters.get("operand"); //$NON-NLS-1$
		try {
			if (operand instanceof InstallableUnitOperand)
				upgradeFrom = ((InstallableUnitOperand) operand).first();
		}
		catch (Throwable e) {
			// Ignore class not found in case InstallableUnitOperand is missing
		}
		if (upgradeFrom != null) {
			performUpgrade(iu, upgradeFrom);
		} else {
			performNewInstall(iu);
		}
		return Status.OK_STATUS;
	}

	protected abstract void performUpgrade(IInstallableUnit iu, IInstallableUnit oldIu);
	
	protected abstract void performNewInstall(IInstallableUnit iu);

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}


}
