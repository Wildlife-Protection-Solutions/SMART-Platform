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
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for exporting patrol data.
 * 
 * <p>
 * Displays a dialog for users to select export location and other parameters,
 * then exports the patrol data.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ExportPatrolHandler extends AbstractHandler {

	
	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell shell = HandlerUtil.getActiveShell(event);
		
		MultiPatrolExportDialog dialog = new MultiPatrolExportDialog(shell);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}
		
		final List<byte[]> patrols = dialog.getObjectUuids();	
		final boolean includeAtt = dialog.getIncludeAttachments();
		final File dir = new File(dialog.getDirectory());
		if (patrols.size() == 0){
			return null;
		}
		if (!dir.exists() || !dir.isDirectory()){
			return null;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ExportPatrolHandler_Progress_ExportingPatrols, patrols.size());
					int exportCnt = 0;
					for (int i = 0; i < patrols.size(); i++) {
						if (monitor.isCanceled()) break;
						byte[] puuid = patrols.get(i);
						String id = null;
						try {
							monitor.subTask(MessageFormat.format(Messages.ExportPatrolHandler_Progress_LoadingPatrol,new Object[]{ SmartUtils.encodeHex(puuid)}));
							Patrol p = null;
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							
							try {
								p = (Patrol) s.load(Patrol.class, puuid);
								id = p.getId();
							} catch (Exception ex) {
								displayLogError(MessageFormat.format(Messages.ExportPatrolHandler_Error_CouldNotFindPatrol, new Object[]{SmartUtils.encodeHex(puuid)}), ex);
								continue;
							} finally {
								s.getTransaction().commit();
								s.close();
							}

							monitor.subTask(MessageFormat.format(Messages.ExportPatrolHandler_Progress_ExportingPatrol,new Object[]{ id }));

							File outFile = PatrolExporter.getOutputFile(dir, p.getId(), includeAtt);
							PatrolExporter.exportPatrol(p, outFile, includeAtt, new NullProgressMonitor());
							exportCnt++;
						} catch (Exception ex) {
							displayLogError(MessageFormat.format(Messages.ExportPatrolHandler_Error_ExportingPatrol , new Object[]{id!= null ? id : SmartUtils.encodeHex(puuid)}) + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						monitor.worked(1);
					}
					if (monitor.isCanceled()){
						displayInfo(Messages.ExportPatrolHandler_ExportCancelledDialogTitle, MessageFormat.format(Messages.ExportPatrolHandler_ExportComplete_DialogMessage1, new Object[]{exportCnt,dir.toString(),patrols.size()}));
					}else{
						displayInfo(Messages.ExportPatrolHandler_ExportComplete_DialogTitle, MessageFormat.format(Messages.ExportPatrolHandler_ExportComplete_DialogMessage1, new Object[]{exportCnt,dir.toString(),patrols.size()}));
					}
				}

			});
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(
					Messages.ExportPatrolHandler_ExportErrorMessage + e.getLocalizedMessage(), e);
		}

		return null;

	}

	private void displayInfo(final String title, final String message) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault()
						.getActiveShell(), title, message);
			}
		});
	}

	private void displayLogError(final String error, final Exception ex) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				SmartPatrolPlugIn.displayLog(error, ex);
			}
		});

	}
}
