/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
