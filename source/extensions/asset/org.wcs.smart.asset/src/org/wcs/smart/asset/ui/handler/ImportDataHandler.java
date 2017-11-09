package org.wcs.smart.asset.ui.handler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.ui.views.data.DataImporterInput;
import org.wcs.smart.asset.ui.views.data.DataImporterView;
import org.wcs.smart.asset.ui.views.station.StationEditor;

public class ImportDataHandler {

	
	public void execute() {
		
		FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI | SWT.OPEN);
		if (fd.open() == null) return;
		
		
		List<Path> paths = new ArrayList<>();
		for (String file : fd.getFileNames()) {
			paths.add(Paths.get(fd.getFilterPath(), file));
		}
		
		DataImporterInput input = new DataImporterInput(paths);
		
		try {			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, DataImporterView.ID);
		} catch (PartInitException e) {
			AssetPlugIn.displayLog(MessageFormat.format("Error opening station editor: {0}", e.getMessage()), e);
		}	
		
	}
}
