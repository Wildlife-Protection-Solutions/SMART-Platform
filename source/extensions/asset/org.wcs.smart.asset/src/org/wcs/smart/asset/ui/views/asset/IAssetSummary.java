package org.wcs.smart.asset.ui.views.asset;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;

public interface IAssetSummary {

	public String getSummaryName();
	
	public String getSummaryValue(Asset asset, Session session);
}
