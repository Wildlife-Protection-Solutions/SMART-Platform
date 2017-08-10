package org.wcs.smart.i2.ui.handler;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.xml.XmlToIntelData;

import com.ibm.icu.text.MessageFormat;

public class ImportModelFromXmlHandler {

	public static final String PREFERENCE_DIR_KEY = ImportModelFromXmlHandler.class.getCanonicalName() + ".dir";  //$NON-NLS-1$
	
	@Execute
	public void execute(IEclipseContext context) {
		String initDir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
			
		FileDialog fd = new FileDialog(context.get(Shell.class), SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-2$
		fd.setFilterNames(new String[] {"zip file (*.zip)", "all files (*.*)"});
		if (initDir != null) {
			fd.setFileName(initDir);
		}
		String file = fd.open();
		if (file == null) return;
		Intelligence2PlugIn.getDefault().getPreferenceStore().putValue(PREFERENCE_DIR_KEY, file);
		Path path = Paths.get(file);
		if (!Files.exists(path)) {
			MessageDialog.openWarning(context.get(Shell.class), "Not Found", MessageFormat.format("File not found {0}", path.toString()));
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
		try {
			pmd.run(true,  true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					XmlToIntelData dd = new XmlToIntelData(SmartDB.getCurrentConservationArea());
					try {
						dd.importXmlData(path, monitor);
					}catch(OperationCanceledException ex) {
						Display.getDefault().syncExec(()-> MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Canceled", "Operation canceled by user."));
					}catch (Exception ex) {
						Intelligence2PlugIn.displayLog("Error importing xml data: " + ex.getMessage(), ex);
					}
				}
			});
		}catch (Exception ex) {
			Intelligence2PlugIn.displayLog("Error importing xml data:" + ex.getMessage(), ex);
		}
		
	}
	
	// E3
	public static class ImportModelFromXmlHandlerWrapper extends DIHandler<ImportModelFromXmlHandler> {
		public ImportModelFromXmlHandlerWrapper() {
			super(ImportModelFromXmlHandler.class);
		}
	}

}
