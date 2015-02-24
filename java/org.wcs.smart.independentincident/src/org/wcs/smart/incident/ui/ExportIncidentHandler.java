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
import java.text.MessageFormat;
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
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.xml.IncidentExporter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for exporting incident data.
 * 
 * <p>
 * Displays a dialog for users to select export location and other parameters,
 * then exports the incident data.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportIncidentHandler {

	
	@Execute
	public void execute(final Shell activeShell){

		MultiIncidentExportDialog dialog = new MultiIncidentExportDialog(activeShell);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		
		final List<byte[]> incidents = dialog.getObjectUuids();	
		final boolean includeAtt = dialog.getIncludeAttachments();
		final File dir = new File(dialog.getDirectory());
		if (incidents.size() == 0){
			return;
		}
		if (!dir.exists() || !dir.isDirectory()){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ExportIncidentHandler_ExportingProgress, incidents.size());
					int exportCnt = 0;
					for (int i = 0; i < incidents.size(); i++) {
						if (monitor.isCanceled()) break;
						byte[] puuid = incidents.get(i);
						Integer id = null;
						try {
							monitor.subTask(MessageFormat.format(Messages.ExportIncidentHandler_IncidentProgress,new Object[]{ SmartUtils.encodeHex(puuid)}));
							Waypoint wp = null;
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							
							try {
								wp = (Waypoint) s.load(Waypoint.class, puuid);
								id = wp.getId();
							} catch (Exception ex) {
								IncidentPlugIn.displayLog(MessageFormat.format(Messages.ExportIncidentHandler_IncidentNotFound, new Object[]{SmartUtils.encodeHex(puuid)}), ex);
								continue;
							} finally {
								s.getTransaction().commit();
								s.close();
							}

							monitor.subTask(MessageFormat.format(Messages.ExportIncidentHandler_ExportingIncidentProgress,new Object[]{ String.valueOf(id) }));

							File outFile = IncidentExporter.getOutputFile(dir, String.valueOf(wp.getId()), includeAtt);
							IncidentExporter.exportIncident(wp, outFile, includeAtt, new NullProgressMonitor());
							exportCnt++;
						} catch (Exception ex) {
							IncidentPlugIn.displayLog(MessageFormat.format("Error exporting incident.  Skipping ID {0}." , new Object[]{id!= null ? id : SmartUtils.encodeHex(puuid)}) + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
						}
						monitor.worked(1);
					}
					if (monitor.isCanceled()){
						displayInfo(activeShell, Messages.ExportIncidentHandler_CancelledDialogTitle, MessageFormat.format(Messages.ExportIncidentHandler_CancelledMessage, new Object[]{exportCnt,dir.toString(),incidents.size()}));
					}else{
						displayInfo(activeShell, Messages.ExportIncidentHandler_ComnpleteDialogTitle, MessageFormat.format(Messages.ExportIncidentHandler_CompleteMessage, new Object[]{exportCnt,dir.toString(),incidents.size()}));
					}
				}

			});
		} catch (Exception e) {
			IncidentPlugIn.displayLog(
					Messages.ExportIncidentHandler_Error + e.getLocalizedMessage(), e);
		}
	}
	

	private void displayInfo(final Shell shell, final String title, final String message) {
		shell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(shell, title, message);
			}
		});
	}
	
	public static class ExportIncidentHandlerWrapper extends DIHandler<ExportIncidentHandler>{
		public ExportIncidentHandlerWrapper(){
			super(ExportIncidentHandler.class);
		}
	}
}
