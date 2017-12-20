package org.wcs.smart.asset.ui.handler;

import java.text.MessageFormat;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.ui.views.data.DataImporterInput;
import org.wcs.smart.asset.ui.views.data.DataImporterView;

public class ImportDataHandler {

	
	public void execute() {
		
		
		
		DataImporterInput input = new DataImporterInput();
		
		try {			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, DataImporterView.ID);
		} catch (PartInitException e) {
			AssetPlugIn.displayLog(MessageFormat.format("Error opening station editor: {0}", e.getMessage()), e);
		}	
		
	}
}
