package org.wcs.smart.asset.ui.views.asset;

import java.text.MessageFormat;
import java.util.Date;

import javax.persistence.Query;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.hibernate.QueryFactory;

public class IncidentAssetSummary implements IAssetSummary {

public static IncidentAssetSummary INSTANCE = new IncidentAssetSummary();
	
	private IncidentAssetSummary() {
		
	}
	
	@Override
	public String getSummaryName() {
		return "Total Incidents:";
	}

	@Override
	public String getSummaryValue(Asset asset, Session session) {
		
		String queryString = "SELECT count(*) FROM AssetWaypoint WHERE id.assetDeployment.asset = :asset";
		Query query = session.createQuery(queryString);
		query.setParameter("asset", asset);
		Long cnt = (Long) query.getSingleResult();
		
		return MessageFormat.format("{0} incidents", cnt);
	}

}
