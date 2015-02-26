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
package org.wcs.smart.patrol.internal.ui.properties.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.createpatrol.CreatePatrolWizard;
import org.wcs.smart.patrol.internal.ui.views.PatrolListView;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;

/**
 * Handler to display new patrol wizard.
 * 
 * @author Emily
 *
 */
public class NewPatrolHandler {

	private WizardDialog dialog = null;

	@Execute
	public void execute(final Shell activeShell, IEclipseContext context)  {
		//open the correct perspective/view
		final MWindow activeWindow = context.get(MWindow.class);
		(new ShowFieldDataPerspective()).execute(PatrolListView.ID, activeWindow);
		
		//Show Create Patrol Wizard
		final CreatePatrolWizard wizard = new CreatePatrolWizard();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);

		try {
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(Messages.NewPatrolHandler_Progress_DisplayingWizard);
					dialog = new WizardDialog(activeShell, wizard);

				}
			});
		} catch (Exception ex) {
			dialog = null;
			SmartPatrolPlugIn.displayLog(Messages.NewPatrolHandler_Error_LoadingWizard
					+ ex.getLocalizedMessage(), ex);
		}
		if (dialog != null) {
			if (dialog.open() == Window.OK){
				//open patrol
				(new OpenPatrolHandler()).openPatrol(wizard.getPatrol().getUuid(), activeWindow);
			}
		}
	}
	
	public static class NewPatrolHandlerWrapper extends DIHandler<NewPatrolHandler>{
		public NewPatrolHandlerWrapper(){
			super(NewPatrolHandler.class);
		}
	}

}
