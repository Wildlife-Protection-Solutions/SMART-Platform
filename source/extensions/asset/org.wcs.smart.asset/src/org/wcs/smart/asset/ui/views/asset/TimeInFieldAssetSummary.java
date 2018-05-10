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

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Asset summary computation that computes the total time
 * the asset spent in the field (over all deployments)
 * 
 * @author Emily
 *
 */
public class TimeInFieldAssetSummary implements IAssetSummary {
	
	public static TimeInFieldAssetSummary INSTANCE = new TimeInFieldAssetSummary();
	
	private TimeInFieldAssetSummary() {
		
	}
	
	@Override
	public String getSummaryName() {
		return Messages.TimeInFieldAssetSummary_TimeInField;
	}

	@Override
	public String getSummaryValue(Asset asset, Session session) {
		double totalHours = 0;
		if (asset.getUuid() != null) {
			try(ScrollableResults results = QueryFactory.buildQuery(session, AssetDeployment.class, "asset", asset).scroll()){ //$NON-NLS-1$
				while(results.next()) {
					AssetDeployment as = (AssetDeployment) results.get(0);
					totalHours += as.getActiveTimeInSeconds();
				}
			}
		}
		return AssetUtils.formatTime(totalHours);
	}

}
