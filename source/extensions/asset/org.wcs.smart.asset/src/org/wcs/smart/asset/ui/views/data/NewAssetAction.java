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
package org.wcs.smart.asset.ui.views.data;

import java.text.MessageFormat;
import java.util.Iterator;

import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.data.importer.ActionableWarning;
import org.wcs.smart.asset.data.importer.FileProcessor;
import org.wcs.smart.asset.data.importer.FileProxy;
import org.wcs.smart.asset.data.importer.NewAssetWarning;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.ui.handler.NewAssetHandler;

/**
 * Action for creating a new asset and updating all proxies
 * in the associated processor with this new asset
 * 
 * @author Emily
 *
 */
public class NewAssetAction implements ImportAction {

	private NewAssetWarning warning;
	
	@Inject
	private IEventBroker broker;
	
	public NewAssetAction(NewAssetWarning warning) {
		this.warning = warning;
	}

	@Override
	public boolean preformAction(FileProcessor processor, FileProxy selectedItem) {
		Asset newAsset = (new NewAssetHandler()).createAndSave(warning.getAssetId(), broker);
		if (newAsset == null) return false;
		selectedItem.setAsset(newAsset);
		selectedItem.getWarnings().remove(warning);
		
		//lets see if we can find other items that also have this same warning
		for (FileProxy other : processor.getFiles()) {
			if (other.getAsset() != null) continue;
			for (Iterator<ActionableWarning> iterator = other.getWarnings().iterator(); iterator.hasNext();) {
				ActionableWarning aw = iterator.next();
				if (aw instanceof NewAssetWarning && ((NewAssetWarning) aw).getAssetId().equals(warning.getAssetId())) {
					other.setAsset(newAsset);
					iterator.remove();
				}
			}
		}
		return true;
	}
	
	public String getMenuLabel() {
		return MessageFormat.format(Messages.NewAssetAction_ActionName, warning.getAssetId());
	}
	
	@Override
	public Image getMenuImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON);
	}
}
