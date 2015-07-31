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
package org.wcs.smart.patrol.query.exportimport;

import org.geotools.data.DataUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.patrol.query.map.geotools.PatrolQueryDataSource;
import org.wcs.smart.patrol.query.map.geotools.QueryDataSource;
import org.wcs.smart.patrol.query.map.geotools.QueryResultItemFeature;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolShapeQueryExporter extends ShapeQueryExporter{

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query.getTypeKey().equals(PatrolObservationQuery.KEY) ||
				query.getTypeKey().equals(PatrolWaypointQuery.KEY)||
				query.getTypeKey().equals(PatrolQuery.KEY)){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType) throws Exception{
		if (queryType.getKey().equals(PatrolQuery.KEY)){
			return QueryResultItemFeature.createTrackFeature((PatrolQueryResultItem)it,  queryColumns, shapefile.getSchema(shapefile.getTypeNames()[0]));
		}else{
			return QueryResultItemFeature.createObservationFeature((PatrolQueryResultItem)it,  queryColumns, shapefile.getSchema(shapefile.getTypeNames()[0]));
		}
	}
	
	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType) throws Exception{
		if (queryType.getKey().equals(PatrolQuery.KEY)){
			return DataUtilities.createType("smart." + PatrolQueryDataSource.PATROL_TYPE, PatrolQueryDataSource.getFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
		}else{
			return DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE, QueryDataSource.getFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
		}
	}
		
}

