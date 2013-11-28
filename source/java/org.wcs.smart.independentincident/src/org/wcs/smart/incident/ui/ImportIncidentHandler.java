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
package org.wcs.smart.incident.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.common.control.XmlImportDialog;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.xml.IncidentImporter;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Command handler for importing incidents data from xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportIncidentHandler extends AbstractHandler {

	private static final String PATROL_NOT_IMPORTED_ERROR_MSG = "Incident not imported.";


	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbench activeWorkbench = HandlerUtil
				.getActiveWorkbenchWindow(event).getWorkbench();

		XmlImportDialog dialog = new XmlImportDialog(Display.getCurrent().getActiveShell(),
				"Import Incident Data",
				"Import Incidents",
				"Select the incident file(s) to import.");
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

		List<String> files = dialog.getFileNames();
		
		if (files.size() == 1){
			File file = new File(files.get(0));
			if (!file.exists()) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Error", MessageFormat.format("The location {0} cannot be found",  new Object[]{file.toString()}));
				return null;
			}			
			importFiles(activeWorkbench, files);
		
		}else if (files.size() > 0){
			importFiles(activeWorkbench, files);
		}
		return null;
	}

	public void importFiles(final IWorkbench activeWorkbench, final List<String> files){
		final Display display = Display.getCurrent();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(display.getActiveShell());
		
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					Collections.sort(files, new Comparator<String>() {

						@Override
						public int compare(String o1, String o2) {
							if (o1.length() < o2.length()){
								return -1;
							}else if (o1.length() > o2.length()){
								return 1;
							}
							
							return Collator.getInstance().compare(o1,o2);
						}
					});
					
					
					monitor.beginTask("Loading Incidents", files.size());
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					for (int i = 0; i < files.size(); i ++){
						File file = new File(files.get(i));
						monitor.subTask(MessageFormat.format("Processing file {0}.", new Object[]{file.toString()}));
					
						monitor.worked(1);
						if (file.isDirectory()) continue;
						try{
							Waypoint wp = IncidentImporter.importIncident(file, nullPm);
							if (wp != null) {
								IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, wp);
							}
						}catch (Exception ex){
							IncidentPlugIn.displayLog(MessageFormat.format("File {0} not imported.", new Object[]{file.toString()}) + ex.getLocalizedMessage(), ex);
						}
						if (monitor.isCanceled()){
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(display.getActiveShell(), "Cancelled", "The import has been cancelled.  All incident loaded to this point will remain in the database.");									
								}
							});
							
							return;
						}
					}
				}
			});
		} catch (Exception e) {
			IncidentPlugIn.displayLog(
					PATROL_NOT_IMPORTED_ERROR_MSG + e.getLocalizedMessage(), e);
		}
	}
}