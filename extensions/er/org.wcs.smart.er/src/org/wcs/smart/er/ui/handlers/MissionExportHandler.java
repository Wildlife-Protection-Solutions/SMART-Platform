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
package org.wcs.smart.er.ui.handlers;

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
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.mission.export.MultiMissionExportDialog;
import org.wcs.smart.er.xml.MissionExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
* Handler for exporting survey designs.
* @author Jeff
*
*/

public class MissionExportHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Shell shell = HandlerUtil.getActiveShell(event);
		
		MultiMissionExportDialog dialog = new MultiMissionExportDialog(shell);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}
		
		final List<byte[]> missions = dialog.getObjectUuids();	
		final boolean includeAtt = dialog.getIncludeAttachments();
		final File dir = new File(dialog.getDirectory());
		if (missions.size() == 0){
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
					monitor.beginTask("Export Missions", missions.size());
					int exportCnt = 0;
					for (int i = 0; i < missions.size(); i++) {
						if (monitor.isCanceled()) break;
						byte[] puuid = missions.get(i);
						String id = null;
						try {
							monitor.subTask(MessageFormat.format("Exporting Mission {0}",new Object[]{ SmartUtils.encodeHex(puuid)}));
							Mission m = null;
							Session s = HibernateManager.openSession();
							s.beginTransaction();
							
							try {
								m = (Mission) s.load(Mission.class, puuid);
								id = m.getId();
							} catch (Exception ex) {
								displayLogError(MessageFormat.format("Error Parsing Mission, ID: {0}", new Object[]{SmartUtils.encodeHex(puuid)}), ex);
								continue;
							} finally {
								s.getTransaction().commit();
								s.close();
							}

							monitor.subTask(MessageFormat.format("Exporting Mission ID: {0}",new Object[]{ id }));

							File outFile = MissionExporter.getOutputFile(dir, m.getId(), includeAtt);
							MissionExporter.exportPatrol(m, outFile, includeAtt, new NullProgressMonitor());
							exportCnt++;
						} catch (Exception ex) {
							displayLogError(MessageFormat.format("Error exporting Missions: {0}", new Object[]{id!= null ? id : SmartUtils.encodeHex(puuid)}) + "\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						monitor.worked(1);
					}
					if (monitor.isCanceled()){
						displayInfo("Export Cancelled", MessageFormat.format("{0, number, integer} of {2, number, integer} selected missions exported to {1}.", new Object[]{exportCnt,dir.toString(),missions.size()}));
					}else{
						displayInfo("Export Complete.", MessageFormat.format("{0, number, integer} of {2, number, integer} selected missions exported to {1}.", new Object[]{exportCnt,dir.toString(),missions.size()}));
					}
				}

			});
		} catch (Exception e) {
			EcologicalRecordsPlugIn.displayLog(
					"Could not export the missions." + e.getLocalizedMessage(), e);
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
				EcologicalRecordsPlugIn.displayLog(error, ex);
			}
		});

	}

	}