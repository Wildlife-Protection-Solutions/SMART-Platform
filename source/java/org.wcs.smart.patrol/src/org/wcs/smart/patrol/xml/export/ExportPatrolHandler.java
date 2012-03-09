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
package org.wcs.smart.patrol.xml.export;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.editor.PatrolEditor;

/**
 * Handler for exporting patrol data.
 * 
 * <p>Displays a dialog for users to select
 * export location and other parameters, then exports
 * the patrol data.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportPatrolHandler extends AbstractHandler  {


	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if (editor instanceof PatrolEditor){
			PatrolExportDialog dialog = new PatrolExportDialog(editor.getSite().getShell(), ((PatrolEditor) editor).getPatrol());
			if (dialog.open() != IDialogConstants.OK_ID){
				return null;
			}
			
			final boolean includeAtt = dialog.getIncludeAttachments();
			final File file = PatrolExporter.getOutputFile(dialog.getFileName(), includeAtt);
			
			if (file.exists()){
				if (!MessageDialog.openConfirm(editor.getSite().getShell(), "Overwrite?", "The file " + file.toString() + " exists.  Are you sure you want to overwrite?")){
					return null;
				}
			}
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(editor.getSite().getShell());
			try {
				pmd.run(false, false, new IRunnableWithProgress() {				
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						try {
							File f = PatrolExporter.exportPatrol(((PatrolEditor) editor).getPatrol(), file, includeAtt, monitor);
							MessageDialog.openInformation(editor.getSite().getShell(), "Export", "Patrol data exported successfully to " + f.getAbsolutePath());
						} catch (Exception e) {
							SmartPatrolPlugIn.displayLog("Could not export the patrol. " + e.getMessage(), e);
						}						
					}
				});
			} catch (Exception e) {
				SmartPatrolPlugIn.displayLog("Could not export the patrol. " + e.getMessage(), e);
			}			
		}
		return null;
	}


}
