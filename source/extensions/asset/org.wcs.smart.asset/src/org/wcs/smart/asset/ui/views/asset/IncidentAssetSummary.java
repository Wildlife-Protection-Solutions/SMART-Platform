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
package org.wcs.smart.asset.ui.views.asset;

import java.text.MessageFormat;

import javax.persistence.Query;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;

/**
 * Asset summary computation that computes the total number of incidents
 * associated with all deployments of the asset.
 * 
 * @author Emily
 *
 */
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
		Long cnt = 0l;
		if (asset.getUuid() != null) {
			String queryString = "SELECT count(*) FROM AssetWaypoint WHERE id.assetDeployment.asset = :asset";
			Query query = session.createQuery(queryString);
			query.setParameter("asset", asset);
			cnt = (Long) query.getSingleResult();
		}
		
		return MessageFormat.format("{0} incidents", cnt);
	}

}
