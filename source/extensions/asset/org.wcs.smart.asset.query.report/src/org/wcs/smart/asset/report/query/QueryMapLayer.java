/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.asset.report.query;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.GeometrySummaryQueryResult;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.report.birt.query.AbstractQueryMapLayer;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART Query Map Layer
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer {

	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(AssetObservationQuery.KEY) ||
				queryTypeKey.equals(AssetWaypointQuery.KEY)){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canAddToMap(DataSetHandle handle) {
		if (!(handle instanceof OdaDataSetHandle)) {
			return false;
		}
		OdaDataSetHandle odaHandle = (OdaDataSetHandle) handle;
		if (odaHandle.getExtensionID().equals(AbstractSmartBirtQuery.SMART_DATASET_TYPE)) {
			String queryText = odaHandle.getQueryText();
			String queryTypeKey = queryText.split(":")[0]; //$NON-NLS-1$
			if (canAddToMap(queryTypeKey)) return true;
			
			if (AssetSummaryQuery.isAssetSummary(queryTypeKey)){
				try(Session session = HibernateManager.openSession()){
					UUID uuid = UuidUtils.stringToUuid(queryText.split(":")[1]); //$NON-NLS-1$
					AssetSummaryQuery q = session.get(AssetSummaryQuery.class, uuid);
					if (q != null) {
						return AssetSummaryQuery.canAddGeometry(q.getQueryDefinition());
					}
				}catch (Exception ex) {
					AssetPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(String queryTypeKey){
		if (AssetSummaryQuery.isAssetSummary(queryTypeKey)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, GeometrySummaryQueryResult.GEOMETRY_COLUMN_KEY);
			return Collections.singletonList(def);
		}
		return super.getGeometryOptions(queryTypeKey);
		
	}

}
