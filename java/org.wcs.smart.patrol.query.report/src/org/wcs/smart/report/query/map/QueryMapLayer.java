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
package org.wcs.smart.report.query.map;

import java.util.Collections;
import java.util.List;

import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.report.birt.map.AbstractQueryMapLayer;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * SMART Query Map Layer
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer {

	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(PatrolGriddedQuery.KEY) ||
				queryTypeKey.equals(PatrolQuery.KEY) ||
				queryTypeKey.equals(PatrolObservationQuery.KEY) ||
				queryTypeKey.equals(PatrolWaypointQuery.KEY)){
			return true;
		}
		//also test the deprecated query types
		if (QueryTypeManager.INSTANCE.findDeprecatedQueryType(queryTypeKey) != null){
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(String queryTypeKey){
		if (queryTypeKey.equals(PatrolWaypointQuery.KEY) ||
				queryTypeKey.equals(PatrolObservationQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, PatrolQueryResultItem.WAYPOINT_GEOMCOLUMN_KEY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(PatrolQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.MULTILINE, PatrolQueryResultItem.TRACK_GEOMCOLUMN_KEY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(PatrolGriddedQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.RASTER, null);
			return Collections.singletonList(def);
		}
		return null;
		
	}

}
