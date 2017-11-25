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
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.ui.handler.NewAssetHandler;

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
		for (FileProxy other : processor.getFileDetails()) {
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
		return MessageFormat.format("Create Asset {0}", warning.getAssetId());
	}
	
	@Override
	public Image getMenuImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON);
	}
}
