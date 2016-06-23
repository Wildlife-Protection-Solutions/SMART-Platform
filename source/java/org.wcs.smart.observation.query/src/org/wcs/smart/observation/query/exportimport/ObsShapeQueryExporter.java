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
package org.wcs.smart.observation.query.exportimport;

import org.geotools.data.DataUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.observation.query.map.geotools.QueryDataSource;
import org.wcs.smart.observation.query.map.geotools.QueryResultItemFeature;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
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
public class ObsShapeQueryExporter extends ShapeQueryExporter{

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query.getTypeKey().equals(ObsObservationQuery.KEY) ||
				query.getTypeKey().equals(ObservationWaypointQuery.KEY)){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType, SimpleFeatureType type) throws Exception{
		return QueryResultItemFeature.createObservationFeature((ObservationQueryResultItem) it, queryColumns, type);
	}

	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType) throws Exception{
		return DataUtilities.createType("smart." + QueryDataSource.WAYPOINT_TYPE, QueryDataSource.getFeatureSchemaDef(this.queryColumns, false, true)); //$NON-NLS-1$
		
	}
		
}

