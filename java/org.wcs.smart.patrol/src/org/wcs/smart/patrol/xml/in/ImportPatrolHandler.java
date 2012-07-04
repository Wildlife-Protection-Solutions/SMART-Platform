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
package org.wcs.smart.patrol.xml.in;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditor;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditorInput;
import org.wcs.smart.patrol.internal.ui.editor.PatrolPerspective;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Command handler for importing patrol data from xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportPatrolHandler extends AbstractHandler {

	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbench activeWorkbench = HandlerUtil
				.getActiveWorkbenchWindow(event).getWorkbench();

		ImportPatrolDialog dialog = new ImportPatrolDialog(Display.getCurrent()
				.getActiveShell());
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

		final File file = new File(dialog.getFileName());
		if (!file.exists()) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),
					"Error", "The location " + file.toString()
							+ " cannot be found. ");
			return null;
		}
		
		if (file.isFile()) {
			importFile(activeWorkbench, file);
		}else if (file.isDirectory()){
			importDirectory(activeWorkbench, file);
		}
		return null;
	}

	public void importDirectory(final IWorkbench activeWorkbench, final File directory){
		final Display display = Display.getCurrent();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(display.getActiveShell());
		
		try {
			pmd.run(false, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					File[] files = directory.listFiles();
					monitor.beginTask("Loading Patrols", files.length);
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					for (int i = 0; i < files.length; i ++){
						monitor.subTask("Processing " + files[i].toString());
						monitor.worked(1);
						
						if (files[i].isDirectory()) continue;
						
						try{
							Patrol p = PatrolImporter.importPatrol(files[i], nullPm);
							if (p != null) {
								PatrolEventManager.getInstance().patrolAdded(p);
							}
						}catch (Exception ex){
							SmartPatrolPlugIn.displayLog("File " + files[i].toString() + " not imported: " + ex.getMessage(), ex);
						}
						
						while(display.readAndDispatch()){}
						
						if (monitor.isCanceled()){
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(display.getActiveShell(), "Cancelled", "The import has been cancelled.  All patrols loaded to this point will remain in the database.");									
								}
							});
							
							return;
						}
					}
				}
			});
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(
					"Patrol not imported. " + e.getMessage(), e);
		}
	}
	
	
	public void importFile(final IWorkbench activeWorkbench, final File file) {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display
				.getCurrent().getActiveShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						Patrol p = PatrolImporter.importPatrol(file, monitor);
						if (p != null) {
							PatrolEventManager.getInstance().patrolAdded(p);

							try {
								activeWorkbench.showPerspective(
										PatrolPerspective.ID, activeWorkbench
												.getActiveWorkbenchWindow());
								PatrolEditorInput input = new PatrolEditorInput(
										p.getUuid(), p.getId(), p
												.getPatrolType(), p
												.getStartDate(), p.getEndDate());
								PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage()
										.openEditor(input, PatrolEditor.ID);
							} catch (Exception ex) {
								SmartPatrolPlugIn.log(
										"Error loading imported patrol.", ex);
							}
						}
					} catch (Exception e) {
						SmartPatrolPlugIn.displayLog("Patrol not imported. "
								+ e.getMessage(), e);
					}

				}
			});
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(
					"Patrol not imported. " + e.getMessage(), e);
		}
	}
}