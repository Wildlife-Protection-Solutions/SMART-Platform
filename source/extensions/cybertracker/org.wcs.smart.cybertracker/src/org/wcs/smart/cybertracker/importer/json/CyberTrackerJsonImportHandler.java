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
package org.wcs.smart.cybertracker.importer.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Handler for importing data from CyberTracker application.
 * 
 * @author elitvin
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public class CyberTrackerJsonImportHandler {

	@Execute
	public void execute(MWindow activeWindow, Shell shell){
		(new ShowFieldDataPerspective()).execute(
				"org.wcs.smart.patrol.ui.PatrolListView", activeWindow); //$NON-NLS-1$
		
		FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
		fd.setText(Messages.CyberTrackerJsonImportHandler_FileDialogTitle);
		fd.setFilterExtensions(new String[] {"*.json", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.CyberTrackerJsonImportHandler_JsonFiles, Messages.CyberTrackerJsonImportHandler_AllFiles});
		
		List<Path> toProcess = new ArrayList<>();
		if (fd.open() != null) {
			String path = fd.getFilterPath();
			String[] files = fd.getFileNames();	
			Path root = Paths.get(path);
			for (String f : files) {
				toProcess.add(root.resolve(f));
			}
		}
		
		try {
			JsonImportEditor editor = (JsonImportEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().openEditor(JsonImportEditor.INPUT, JsonImportEditor.ID);
			if (editor != null) editor.processFiles(toProcess);
		} catch (Throwable t) {
			SmartPlugIn.displayLog(t.getLocalizedMessage(), t);
		}
	}
	
	public static class CyberTrackerJsonImportHandlerWrapper extends DIHandler<CyberTrackerJsonImportHandler>{
		public CyberTrackerJsonImportHandlerWrapper(){
			super(CyberTrackerJsonImportHandler.class);
		}
	}
}
