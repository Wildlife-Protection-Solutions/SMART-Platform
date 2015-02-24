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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.common.control.XmlImportDialog;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.xml.IncidentImporter;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Command handler for importing incidents data from xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportIncidentHandler {

	private static final String INCIDENT_NO_IMPORTED = Messages.ImportIncidentHandler_ImportError;

	@Execute
	public void execute(Shell activeShell){
				XmlImportDialog dialog = new XmlImportDialog(activeShell,
				Messages.ImportIncidentHandler_DialogTitle,
				Messages.ImportIncidentHandler_DialogMessage1,
				Messages.ImportIncidentHandler_DialogMessage2);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}

		List<String> files = dialog.getFileNames();
		
		if (files.size() == 1){
			File file = new File(files.get(0));
			if (!file.exists()) {
				MessageDialog.openError(activeShell,
						Messages.ImportIncidentHandler_ErrorDialog, MessageFormat.format(Messages.ImportIncidentHandler_LocationNotFoundError,  new Object[]{file.toString()}));
				return;
			}			
			importFiles(activeShell, files);
		}else if (files.size() > 0){
			importFiles(activeShell, files);
		}
	}

	public void importFiles(final Shell shell, final List<String> files){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		
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
					
					monitor.beginTask(Messages.ImportIncidentHandler_LoadingProgress, files.size());
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					for (int i = 0; i < files.size(); i ++){
						File file = new File(files.get(i));
						monitor.subTask(MessageFormat.format(Messages.ImportIncidentHandler_ProcessingProgress, new Object[]{file.toString()}));
					
						monitor.worked(1);
						if (file.isDirectory()) continue;
						try{
							Waypoint wp = IncidentImporter.importIncident(file, nullPm);
							if (wp != null) {
								IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, wp);
							}
						}catch (Exception ex){
							IncidentPlugIn.displayLog(MessageFormat.format(Messages.ImportIncidentHandler_FileError, new Object[]{file.toString()}) + ex.getLocalizedMessage(), ex);
						}
						if (monitor.isCanceled()){
							shell.getDisplay().syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(shell, Messages.ImportIncidentHandler_CancelledDialogTitle, Messages.ImportIncidentHandler_CancelledDialogMessage);									
								}
							});
							
							return;
						}
					}
				}
			});
		} catch (Exception e) {
			IncidentPlugIn.displayLog(
					INCIDENT_NO_IMPORTED + e.getLocalizedMessage(), e);
		}
	}
	
	public static class ImportIncidentHandlerWrapper extends DIHandler<ImportIncidentHandler>{
		public ImportIncidentHandlerWrapper(){
			super(ImportIncidentHandler.class);
		}
	}
}