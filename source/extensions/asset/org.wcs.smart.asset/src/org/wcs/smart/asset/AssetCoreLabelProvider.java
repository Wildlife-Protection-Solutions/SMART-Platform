package org.wcs.smart.asset;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.asset.model.Asset;

public class AssetCoreLabelProvider {

	public static Image getStatusImage(Asset asset) {
		switch(asset.getStatus()) {
		case ACTIVE:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_ACTIVE);
		case INACTIVE:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_INACTIVE);
		case RETIRED:
			return AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATUS_RETIRED);
		}
		return null;
	}
}
