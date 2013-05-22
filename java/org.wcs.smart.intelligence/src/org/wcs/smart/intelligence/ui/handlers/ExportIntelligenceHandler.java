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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
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
public class ExportIntelligenceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		MultiIntelligenceExportDialog dialog = new MultiIntelligenceExportDialog(shell);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}
		
		final List<byte[]> ids = dialog.getObjectUuids();
		final boolean includeAtt = dialog.getIncludeAttachments();
		final File dir = new File(dialog.getDirectory());

		if (ids.size() == 0 || !dir.exists() || !dir.isDirectory()){
			return null;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
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
								displayLogError(MessageFormat.format(Messages.ExportIntelligenceHandler_LoadIntelligence_Error, SmartUtils.encodeHex(uuid)), ex);
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
							displayLogError(MessageFormat.format(Messages.ExportIntelligenceHandler_ExportIntelligence_Error , name != null ? name : SmartUtils.encodeHex(uuid)) + "\n" +  ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						monitor.worked(1);
					}
					if (monitor.isCanceled()){
						displayInfo(Messages.ExportIntelligenceHandler_ExportCancelledDialogTitle, MessageFormat.format(Messages.ExportIntelligenceHandler_Completed_Message1, new Object[]{exportCnt,dir.toString(), ids.size()}));
					}else{
						displayInfo(Messages.MultiIntelligenceExportDialog_Title1, MessageFormat.format(Messages.ExportIntelligenceHandler_Completed_Message1, new Object[]{exportCnt,dir.toString(), ids.size()}));
					}
				}

			});
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.ExportIntelligenceHandler_Failed_Error_Message + e.getLocalizedMessage(), e);
		}
		
		return null;
	}

	private void displayInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

	private void displayLogError(final String error, final Exception ex) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				IntelligencePlugIn.displayLog(error, ex);
			}
		});
	}
	
}
