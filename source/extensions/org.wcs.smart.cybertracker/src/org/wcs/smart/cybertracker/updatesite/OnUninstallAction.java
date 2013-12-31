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
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Action that is called when CyberTracker plug-in is uninstalled
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class OnUninstallAction extends ProvisioningAction {

	@Override
	public IStatus execute(Map<String, Object> parameters) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "CyberTracker unintall",
						"Do you want to completly remove all database records created by CyberTracker plug-in?")) {

					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "CyberTracker unintall",
							"TODO: Implement db tables removal.");
				}

				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "CyberTracker unintall",
						"CyberTracker feature is in the process of being uninstalled!");
			}
		});
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(Map<String, Object> parameters) {
		return null;
	}

}