/*   
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.handler;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.AssetXmlToAssetData;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Import model data from xml file
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ImportAssetModelFromXmlHandler {

	public static final String PREFERENCE_DIR_KEY = ImportAssetModelFromXmlHandler.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	@Execute
	public void execute(IEclipseContext context) {
		String initDir = AssetPlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
			
		FileDialog fd = new FileDialog(context.get(Shell.class), SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.xml", "*.*"});  //$NON-NLS-1$//$NON-NLS-2$
		fd.setFilterNames(new String[] {"XML Files (*.xml)", "All Files (*.*)"});
		if (initDir != null) {
			fd.setFileName(initDir);
		}
		String file = fd.open();
		if (file == null) return;
		AssetPlugIn.getDefault().getPreferenceStore().putValue(PREFERENCE_DIR_KEY, file);
		
		Path path = Paths.get(file);
		if (!Files.exists(path)) {
			MessageDialog.openWarning(context.get(Shell.class), "Not Found", MessageFormat.format("File {0} not found", path.toString()));
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try {
			pmd.run(true,  true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					AssetXmlToAssetData dd = new AssetXmlToAssetData(SmartDB.getCurrentConservationArea());
					try {
						dd.importXmlData(path, monitor);
					}catch(OperationCanceledException ex) {
						Display.getDefault().syncExec(()-> MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Canceled", "Operation canceled by user"));
					}catch (Exception ex) {
						AssetPlugIn.displayLog("Error importing asset model data: "  + ex.getMessage(), ex);
					}
				}
			});
		}catch (Exception ex) {
			AssetPlugIn.displayLog("Error importing  asset model data: "  + ex.getMessage(), ex);
		}
		
	}
	
	// E3
	public static class ImportAssetModelFromXmlHandlerWrapper extends DIHandler<ImportAssetModelFromXmlHandler> {
		public ImportAssetModelFromXmlHandlerWrapper() {
			super(ImportAssetModelFromXmlHandler.class);
		}
	}

}
