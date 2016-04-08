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
package org.wcs.smart.er.query.report.map;

import java.util.Collections;
import java.util.List;

import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.report.birt.map.AbstractQueryMapLayer;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;

/**
 * SMART query map layer implementation for survey queries
 * 
 * @author Emily
 *
 */
public class QueryMapLayer extends AbstractQueryMapLayer{

	@Override
	public boolean canAddToMap(String queryTypeKey) {
		if (queryTypeKey.equals(SurveyGriddedQuery.KEY) ||
				queryTypeKey.equals(MissionQuery.KEY) ||
				queryTypeKey.equals(SurveyObservationQuery.KEY) ||
				queryTypeKey.equals(MissionTrackQuery.KEY) ||
				queryTypeKey.equals(SurveyWaypointQuery.KEY)){
			return true;
		}
		return false;
	}

	@Override
	public List<MapLayerInfo> getGeometryOptions(String queryTypeKey) {		
		if (queryTypeKey.equals(SurveyObservationQuery.KEY) ||
				queryTypeKey.equals(SurveyWaypointQuery.KEY)){			
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.POINT, SurveyQueryResultItem.WAYPOINT_GEOMETRY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(MissionQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.MULTILINE, SurveyQueryResultItem.TRACK_GEOMETRY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.MULTILINE, MissionTrackResultItem.TRACK_GEOMETRY);
			return Collections.singletonList(def);
		}else if (queryTypeKey.equals(SurveyGriddedQuery.KEY)){
			MapLayerInfo def = new MapLayerInfo(null, null, LayerType.RASTER, "raster");
			return Collections.singletonList(def);
		}
		return null;
	}

}
