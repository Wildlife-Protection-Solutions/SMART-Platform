package org.wcs.smart.patrol.internal.ui.properties.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.createpatrol.CreatePatrolWizard;
import org.wcs.smart.patrol.ui.PatrolPerspective;

public class NewPatrolHandler extends AbstractHandler {

	private WizardDialog dialog = null;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		//Open Patrol Perspective
		try {
			HandlerUtil
					.getActiveWorkbenchWindow(event)
					.getWorkbench()
					.showPerspective(PatrolPerspective.ID,
							HandlerUtil.getActiveWorkbenchWindow(event));
		} catch (WorkbenchException e) {
			SmartPatrolPlugIn
					.displayLog("Error loading patrol perspective.", e);
		}

		//Show Create Patrol Wizard
		final CreatePatrolWizard wizard = new CreatePatrolWizard();
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
			SmartPatrolPlugIn.displayLog("Error loading new patrol wizard. "
					+ ex.getMessage(), ex);
		}
		if (dialog != null) {
			dialog.open();
		}

		return null;
	}

}
