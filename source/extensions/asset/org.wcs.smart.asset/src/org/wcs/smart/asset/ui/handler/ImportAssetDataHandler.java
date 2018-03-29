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

import java.text.MessageFormat;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.ui.views.data.DataImporterInput;
import org.wcs.smart.asset.ui.views.data.DataImporterView;
import org.wcs.smart.observation.ui.FieldDataPerspective;

/**
 * Import assetdata files
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ImportAssetDataHandler {
	
	@Execute
	public void execute(IEclipseContext context) {
		(new org.wcs.smart.ui.ShowPerspectiveHandler()).execute(FieldDataPerspective.ID, context.get(MWindow.class));
		
		DataImporterInput input = new DataImporterInput();
		try {			
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, DataImporterView.ID);
		} catch (PartInitException e) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.ImportAssetDataHandler_OpenError, e.getMessage()), e);
		}	
	}
	
	// E3
	public static class ImportAssetDataHandlerWrapper extends DIHandler<ImportAssetDataHandler> {
		public ImportAssetDataHandlerWrapper() {
			super(ImportAssetDataHandler.class);
		}
	}

}
