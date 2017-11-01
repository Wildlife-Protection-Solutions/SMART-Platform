package org.wcs.smart.asset.ui.views.asset;

import java.text.MessageFormat;
import java.util.Date;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.hibernate.QueryFactory;

public class TimeInFieldAssetSummary implements IAssetSummary {
	
	public static TimeInFieldAssetSummary INSTANCE = new TimeInFieldAssetSummary();
	
	private TimeInFieldAssetSummary() {
		
	}
	
	@Override
	public String getSummaryName() {
		return "Total Time In Field:";
	}

	@Override
	public String getSummaryValue(Asset asset, Session session) {
		double totalHours = 0;
		
		try(ScrollableResults results = QueryFactory.buildQuery(session, AssetDeployment.class, "asset", asset).scroll()){
			while(results.next()) {
				AssetDeployment as = (AssetDeployment) results.get(0);
				Date end = new Date();
				if (as.getEndDate() != null) end = as.getEndDate();
				Date start = as.getStartDate();
				
				totalHours += (end.getTime() - start.getTime()) / 1_000.0;
			}
		}
		return AssetUtils.formatTime(totalHours);
	}

}
