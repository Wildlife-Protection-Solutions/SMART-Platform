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
package org.wcs.smart.plan.ui.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.plan.ui.newPlanWizard.CreatePlanWizard;


/**
 * Handler to display new planwizard.
 * 
 * @author Emily
 * @author jeffloun
 *
 */
public class NewPlanHandler extends AbstractHandler {

	private WizardDialog dialog = null;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		//Show Create Patrol Wizard
		final CreatePlanWizard wizard = new CreatePlanWizard();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(
				HandlerUtil.getActiveShell(event));
		try {
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName("Loading Wizard");
					dialog = new WizardDialog(
							HandlerUtil.getActiveShell(event), wizard);

				}
			});
		} catch (Exception ex) {
			dialog = null;
			SmartPatrolPlugIn.displayLog("Error loading new plan wizard. "
					+ ex.getMessage(), ex);
		}
		if (dialog != null) {
			dialog.open();
		}

		return null;
	}

}
