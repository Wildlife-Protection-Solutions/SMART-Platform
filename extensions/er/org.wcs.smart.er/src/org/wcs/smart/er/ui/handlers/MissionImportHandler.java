/*
 *  Copyright (C) 2012 Wildlife Conservation Society
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
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.xml.MissionImportDialog;
import org.wcs.smart.er.xml.MissionImporter;


/**
 * Command handler for importing mission data from xml and zip files.
 * 
 * @author Jeff
 * @since 4.0.0
 */
public class MissionImportHandler extends AbstractHandler {
	private static final String MISSION_NOT_IMPORTED_ERROR_MSG = "Unable to Import Survey or Mission. ";


	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbench activeWorkbench = HandlerUtil
				.getActiveWorkbenchWindow(event).getWorkbench();

		MissionImportDialog dialog = new MissionImportDialog();
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

		List<String> files = dialog.getFileNames();
		
		if (files.size() == 1){
			File file = new File(files.get(0));
			if (!file.exists()) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						"Error" , MessageFormat.format("The location {0} cannot be found",  new Object[]{file.toString()}));
				return null;
			}			
			importFile(activeWorkbench, file, dialog.isKeepIDs());
		
		}else if (files.size() > 0){
			importFiles(activeWorkbench, files, dialog.isKeepIDs());
		}
		return null;
	}

	public void importFiles(final IWorkbench activeWorkbench, final List<String> files, final boolean keepIDs){
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
					
					
					monitor.beginTask("Loading Missions", files.size());
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					int imported = 0;
					for (int i = 0; i < files.size(); i ++){
						File file = new File(files.get(i));
						monitor.subTask("Processing" + file.toString());
						monitor.worked(1);
						if (file.isDirectory()) continue;
						try{
							Mission m = MissionImporter.importMission(file, keepIDs, nullPm);
							if (m != null) {
								imported ++;
								SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.MISSION_ADDED, m);
							}
						}catch (Exception ex){
							EcologicalRecordsPlugIn.displayLog(MessageFormat.format("File {0} not imported" + "\n\n", new Object[]{file.toString()}) + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						if (monitor.isCanceled()){
							final int aimported = imported;
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(display.getActiveShell(), "Cancelled", MessageFormat.format("The import has been cancelled.  {0} of {1} patrols have been loaded.", aimported, files.size()));									
								}
							});
							
							return;
						}
					}
					final int iimported = imported;
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(display.getActiveShell(), "Import Complete", MessageFormat.format("Import Complete.  {0} of {1} files imported.", iimported, files.size()));
							
						}});
				}
			});
		} catch (Exception e) {
			EcologicalRecordsPlugIn.displayLog(
					MISSION_NOT_IMPORTED_ERROR_MSG + e.getLocalizedMessage(), e);
		}
	}
	
	
	public void importFile(final IWorkbench activeWorkbench, final File file, final boolean keepIDs) {
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display
				.getCurrent().getActiveShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try {
						//Mission m = MissionImporter.importMissionFromFile(file, keepIDs, monitor); 
						Mission m = MissionImporter.importMission(file, keepIDs, monitor);
						if (m != null) {
							SurveyEventHandler.getInstance().fireEvent(SurveyEventHandler.EventType.MISSION_ADDED, m);
							
							/*
							//Currently we are not opening the mission anywhere after loading, maybe we will want to do this in the future?
							 * This code doesn't work, just copied from Patrol import.
							  
							final MissionEditorInput input = new MissionEditorInput(m.getId(), m.getUuid());
							
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									FieldDataPerspective.openPerspective(MissionListView.ID);
									try {
										PlatformUI
												.getWorkbench()
												.getActiveWorkbenchWindow()
												.getActivePage()
												.openEditor(input,
														MissionEditor.ID);
									} catch (Exception ex) {
										EcologicalRecordsPlugIn.log("Error loading imported mission.", //$NON-NLS-1$
														ex);
									}
								}
							});
								*/
							
						}
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								EcologicalRecordsPlugIn.displayLog(MISSION_NOT_IMPORTED_ERROR_MSG + e.getLocalizedMessage(), e);
							}});
					}

				}
			});
		} catch (Exception e) {
			EcologicalRecordsPlugIn.displayLog(
					MISSION_NOT_IMPORTED_ERROR_MSG  + e.getLocalizedMessage(), e);
		}
	}
}

