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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.observation.ui.FieldDataPerspective;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolListView;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Command handler for importing patrol data from xml file.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportPatrolHandler extends AbstractHandler {

	private static final String PATROL_NOT_IMPORTED_ERROR_MSG = Messages.ImportPatrolHandler_PatrolNotImported_Error;


	/**
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbench activeWorkbench = HandlerUtil
				.getActiveWorkbenchWindow(event).getWorkbench();

		PatrolXmlImportDialog dialog = new PatrolXmlImportDialog();
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

		List<String> files = dialog.getFileNames();
		
		if (files.size() == 1){
			File file = new File(files.get(0));
			if (!file.exists()) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(),
						Messages.ImportPatrolHandler_ErrorDialog_Title, MessageFormat.format(Messages.ImportPatrolHandler_Error_DirectoryNotFound,  new Object[]{file.toString()}));
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
					
					
					monitor.beginTask(Messages.ImportPatrolHandler_Progress_LoadingPatrols, files.size());
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					int imported = 0;
					for (int i = 0; i < files.size(); i ++){
						File file = new File(files.get(i));
						monitor.subTask(Messages.ImportPatrolHandler_Progress_ProcessingFile + file.toString());
						monitor.worked(1);
						if (file.isDirectory()) continue;
						try{
							Patrol p = PatrolImporter.importPatrol(file, keepIDs, nullPm);
							if (p != null) {
								imported ++;
								PatrolEventManager.getInstance().patrolAdded(p);
							}
						}catch (Exception ex){
							SmartPatrolPlugIn.displayLog(MessageFormat.format(Messages.ImportPatrolHandler_Error_FileNotImported + "\n\n", new Object[]{file.toString()}) + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						if (monitor.isCanceled()){
							final int aimported = imported;
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(display.getActiveShell(), Messages.ImportPatrolHandler_Cancelled_DialogTitle, MessageFormat.format(Messages.ImportPatrolHandler_Cancelled_DialogMessage1, aimported, files.size()));									
								}
							});
							
							return;
						}
					}
					final int iimported = imported;
					display.syncExec(new Runnable(){
						@Override
						public void run() {
							MessageDialog.openInformation(display.getActiveShell(), Messages.ImportPatrolHandler_MessageTitle, MessageFormat.format(Messages.ImportPatrolHandler_CompleteMessage, iimported, files.size()));
							
						}});
				}
			});
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(
					PATROL_NOT_IMPORTED_ERROR_MSG + e.getLocalizedMessage(), e);
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
						Patrol p = PatrolImporter.importPatrol(file, keepIDs, monitor);
						if (p != null) {
							PatrolEventManager.getInstance().patrolAdded(p);
							final PatrolEditorInput input = new PatrolEditorInput(
									p.getUuid(), p.getId(), p
											.getPatrolType(), p
											.getStartDate(), p.getEndDate());
							
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									FieldDataPerspective.openPerspective(PatrolListView.ID);
									try {
										PlatformUI
												.getWorkbench()
												.getActiveWorkbenchWindow()
												.getActivePage()
												.openEditor(input,
														PatrolEditor.ID);
									} catch (Exception ex) {
										SmartPatrolPlugIn
												.log("Error loading imported patrol.", //$NON-NLS-1$
														ex);
									}
								}
							});
								
							
						}
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPatrolPlugIn.displayLog(PATROL_NOT_IMPORTED_ERROR_MSG+ e.getLocalizedMessage(), e);
							}});
					}

				}
			});
		} catch (Exception e) {
			SmartPatrolPlugIn.displayLog(
					PATROL_NOT_IMPORTED_ERROR_MSG + e.getLocalizedMessage(), e);
		}
	}
}