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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.common.control.XmlImportDialog;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.xml.IntelligenceImporter;

/**
 * Command handler for importing intelligence data from xml file.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ImportIntelligenceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbench activeWorkbench = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench();

		XmlImportDialog dialog = new XmlImportDialog(Display.getCurrent().getActiveShell(),
				Messages.ImportIntelligenceHandler_Dialog_Title,
				Messages.ImportIntelligenceHandler_Dialog_Text,
				Messages.ImportIntelligenceHandler_Dialog_Message);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return null;
		}

		List<String> files = dialog.getFileNames();
		importFiles(activeWorkbench, files);
		
		return null;
	}

	private void importFiles(final IWorkbench activeWorkbench, final List<String> files) {
		final Display display = Display.getCurrent();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(display.getActiveShell());
		
		try {
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.ImportIntelligenceHandler_LoadingFile, files.size());
					IProgressMonitor nullPm = new NullProgressMonitor();
						
					int imported = 0;
					for (int i = 0; i < files.size(); i ++){
						File file = new File(files.get(i));
						monitor.subTask(MessageFormat.format(Messages.ImportIntelligenceHandler_ProcessingFile, file.toString()));
						monitor.worked(1);
						if (file.isDirectory()) continue;
						try{
							Intelligence intel = IntelligenceImporter.importIntelligence(file, nullPm);
							if (intel != null) {
								imported++;
								IntelligenceEventManager.getInstance().intelligenceAdded(intel);
							}
						}catch (Exception ex){
							IntelligencePlugIn.displayLog(MessageFormat.format(Messages.ImportIntelligenceHandler_CannotImportFile, file.toString()) + "\n\n" + ex.getLocalizedMessage(), ex); //$NON-NLS-1$
						}
						if (monitor.isCanceled()){
							final int fimported = imported;
							display.syncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openInformation(display.getActiveShell(), Messages.ImportIntelligenceHandler_CancelInfoDialog_Title,
											MessageFormat.format(Messages.ImportIntelligenceHandler_CancelledMessage,fimported, files.size() ));									
								}
							});
							return;
						}
					}
					
					final int iimported = imported;
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openInformation(display.getActiveShell(), Messages.ImportIntelligenceHandler_CompleteTitle, MessageFormat.format(Messages.ImportIntelligenceHandler_CompleteMessage, iimported, files.size()));									
						}
					});
			
				}
			});
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.ImportIntelligenceHandler_ImportError + e.getLocalizedMessage(), e);
		}
	}

}
