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
package org.wcs.smart.entity.query.report.map;

import java.util.Collections;
import java.util.List;

import org.wcs.smart.data.oda.smart.impl.GriddedQueryRasterCreator;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.report.birt.query.AbstractQueryMapLayer;

/**
 * SMART Query Map Layer
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer{
	
	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(EntityGriddedQuery.KEY) ||
				queryTypeKey.equals(EntityObservationQuery.KEY) ||
				queryTypeKey.equals(EntityWaypointQuery.KEY)){
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(String queryTypeKey){
		if (queryTypeKey.equals(EntityObservationQuery.KEY) ||
				queryTypeKey.equals(EntityWaypointQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, EntityQueryResultItem.WAYPOINT_GEOM_COLUMNKEY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(EntityGriddedQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.RASTER, null);
			return Collections.singletonList(def);
		}
		return null;
	}

}
