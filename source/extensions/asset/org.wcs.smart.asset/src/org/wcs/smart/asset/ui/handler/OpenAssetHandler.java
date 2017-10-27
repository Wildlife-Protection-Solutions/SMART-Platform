/*
 * Copyright (C) 2017 Wildlife Conservation Society
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

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.ui.views.asset.AssetEditor;
import org.wcs.smart.asset.ui.views.asset.AssetEditorInput;

/**
 * Open asset handler 
 * 
 * @author Emily
 *
 */
public class OpenAssetHandler {
	
		public void openAsset(AssetEditorInput input){
			try {			
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, AssetEditor.ID);
			} catch (PartInitException e) {
				AssetPlugIn.displayLog(MessageFormat.format("Error opening asset editor: {0}", e.getMessage()), e);
			}
		}
		
		public void openAsset(Asset asset){
			AssetEditorInput input = new AssetEditorInput(asset.getUuid(),  asset.getId());
			openAsset(input);
		}
	}

