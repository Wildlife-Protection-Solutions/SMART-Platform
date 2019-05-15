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
package org.wcs.smart.i2.ui.handler;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.xml.XmlToIntelData;

/**
 * Import model data from xml file
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ImportModelFromXmlHandler {

	public static final String PREFERENCE_DIR_KEY = ImportModelFromXmlHandler.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	@Execute
	public void execute(IEclipseContext context) {
		String initDir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
			
		FileDialog fd = new FileDialog(context.get(Shell.class), SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.zip", "*.*"});  //$NON-NLS-1$//$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.ImportModelFromXmlHandler_ZipFileLabel, Messages.ImportModelFromXmlHandler_AllFilesLabel});
		if (initDir != null) {
			fd.setFileName(initDir);
		}
		String file = fd.open();
		if (file == null) return;
		Intelligence2PlugIn.getDefault().getPreferenceStore().putValue(PREFERENCE_DIR_KEY, file);
		Path path = Paths.get(file);
		if (!Files.exists(path)) {
			MessageDialog.openWarning(context.get(Shell.class), Messages.ImportModelFromXmlHandler_NotFoundTitle, MessageFormat.format(Messages.ImportModelFromXmlHandler_NotFoundMessage, path.toString()));
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try {
			pmd.run(true,  true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					XmlToIntelData dd = new XmlToIntelData(SmartDB.getCurrentConservationArea());
					try {
						dd.importXmlData(path, monitor, context.get(IEventBroker.class));
					}catch(OperationCanceledException ex) {
						Display.getDefault().syncExec(()-> MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ImportModelFromXmlHandler_CanceledTitle, Messages.ImportModelFromXmlHandler_CanceledMessage));
					}catch (Exception ex) {
						Intelligence2PlugIn.displayLog(Messages.ImportModelFromXmlHandler_ErrorMessage + ex.getMessage(), ex);
					}
				}
			});
		}catch (Exception ex) {
			Intelligence2PlugIn.displayLog(Messages.ImportModelFromXmlHandler_ErrorMessage + ex.getMessage(), ex);
		}
		
	}
	
	// E3
	public static class ImportModelFromXmlHandlerWrapper extends DIHandler<ImportModelFromXmlHandler> {
		public ImportModelFromXmlHandlerWrapper() {
			super(ImportModelFromXmlHandler.class);
		}
	}

}
