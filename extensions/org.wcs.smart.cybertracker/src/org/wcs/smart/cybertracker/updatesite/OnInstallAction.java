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
package org.wcs.smart.cybertracker.updatesite;

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Action that is called when CyberTracker plug-in is installed or upgraded using update site
 * 
 * @author elitvin
 * @since 2.0.0
 */
@SuppressWarnings("restriction")
public class OnInstallAction extends ProvisioningAction {

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
		if (upgradeFrom != null)
			performUpgrade(iu, upgradeFrom);
		else
			performNewInstall(iu);
		return Status.OK_STATUS;
	}

	private void performUpgrade(IInstallableUnit iu, IInstallableUnit oldIu) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "CyberTracker upgrade",
						"CyberTracker feature is in the process of being upgraded.");
			}
		});
	}

	private void performNewInstall(IInstallableUnit iu) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "CyberTracker intall",
						"CyberTracker feature is in the process of being installed!");
			}
		});
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

}
