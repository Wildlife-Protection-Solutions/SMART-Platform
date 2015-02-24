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
package org.wcs.smart.intelligence.ui.handlers;

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
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.xml.export.IntelligenceExporter;
import org.wcs.smart.intelligence.xml.export.MultiIntelligenceExportDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for exporting intelligence data.
 * 
 * <p>
 * Displays a dialog for users to select export location and other parameters,
 * then exports the intelligence data.
 * </p>
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ExportIntelligenceHandler {

	@Execute
	public void execute(final Shell activeShell) {
		
		MultiIntelligenceExportDialog dialog = new MultiIntelligenceExportDialog(activeShell);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}
		
		final List<byte[]> ids = dialog.getObjectUuids();
		final boolean includeAtt = dialog.getIncludeAttachments();
		final File dir = new File(dialog.getDirectory());

		if (ids.size() == 0 || !dir.exists() || !dir.isDirectory()){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ExportIntelligenceHandler_Export_Task, ids.size());
					int exportCnt = 0;
					for (int i = 0; i < ids.size(); i++) {
						if (monitor.isCanceled()){
							break;
						}
						byte[] uuid = ids.get(i);
						String name = null;
						try {
							monitor.subTask(MessageFormat.format(Messages.ExportIntelligenceHandler_LoadIntelligence_SubTask,  SmartUtils.encodeHex(uuid)));
							Intelligence intel = null;
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							try {
								intel = (Intelligence) s.load(Intelligence.class, uuid);
								intel.getReceivedDate();
								name = intel.getName();
							} catch (Exception ex) {
								IntelligencePlugIn.displayLog(MessageFormat.format(Messages.ExportIntelligenceHandler_LoadIntelligence_Error, SmartUtils.encodeHex(uuid)), ex);
								continue;
							} finally {
								s.getTransaction().commit();
								s.close();
							}

							monitor.subTask(MessageFormat.format(Messages.ExportIntelligenceHandler_ExportIntelligence_SubTask, name));

							File outFile = IntelligenceExporter.getOutputFile(dir, intel.getName(), includeAtt);
							IntelligenceExporter.exportIntelligence(intel, outFile, includeAtt, new NullProgressMonitor());

							exportCnt++;
						} catch (Exception ex) {
							IntelligencePlugIn.displayLog(MessageFormat.format(Messages.ExportIntelligenceHandler_ExportIntelligence_Error , name != null ? name : SmartUtils.encodeHex(uuid)) + "\n" +  ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						monitor.worked(1);
					}
					if (monitor.isCanceled()){
						displayInfo(activeShell, Messages.ExportIntelligenceHandler_ExportCancelledDialogTitle, MessageFormat.format(Messages.ExportIntelligenceHandler_Completed_Message1, new Object[]{exportCnt,dir.toString(), ids.size()}));
					}else{
						displayInfo(activeShell, Messages.MultiIntelligenceExportDialog_Title1, MessageFormat.format(Messages.ExportIntelligenceHandler_Completed_Message1, new Object[]{exportCnt,dir.toString(), ids.size()}));
					}
				}

			});
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.ExportIntelligenceHandler_Failed_Error_Message + e.getLocalizedMessage(), e);
		}
	}

	private void displayInfo(final Shell activeShell, final String title, final String message) {
		activeShell.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(activeShell, title, message);
			}
		});
	}

	
	public static class ExportIntelligenceHandlerWrapper extends DIHandler<ExportIntelligenceHandler>{
		public ExportIntelligenceHandlerWrapper(){
			super(ExportIntelligenceHandler.class);
		}
	}
}
