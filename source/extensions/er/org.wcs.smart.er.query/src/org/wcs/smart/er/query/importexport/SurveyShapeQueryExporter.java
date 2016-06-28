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
package org.wcs.smart.er.query.importexport;

import org.geotools.data.DataUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.query.map.geotools.SurveyQueryDataSource;
import org.wcs.smart.er.query.map.geotools.SurveyResultItemFeature;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
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
public class SurveyShapeQueryExporter extends ShapeQueryExporter{

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query.getTypeKey().equals(SurveyObservationQuery.KEY) ||
				query.getTypeKey().equals(SurveyWaypointQuery.KEY)||
				query.getTypeKey().equals(MissionQuery.KEY) ||
				query.getTypeKey().equals(MissionTrackQuery.KEY)	){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType, SimpleFeatureType type) throws Exception{
		if (queryType.getKey().equals(MissionQuery.KEY)){
			return SurveyResultItemFeature.createTrackFeature((SurveyQueryResultItem)it,  queryColumns, type);
		}else if ( query.getTypeKey().equals(SurveyObservationQuery.KEY) ||
				    query.getTypeKey().equals(SurveyWaypointQuery.KEY)){
			return SurveyResultItemFeature.createObservationFeature((SurveyQueryResultItem)it, queryColumns, type);
		}else if (query.getTypeKey().equals(MissionTrackQuery.KEY)){
			return SurveyResultItemFeature.createTrackFeature((MissionTrackResultItem)it, queryColumns, type);
		}
		return null;
	}
	
	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType) throws Exception{
		if (queryType.getKey().equals(MissionQuery.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.TRACKS_TYPE, SurveyQueryDataSource.getTrackFeatureSchemaDef(this.queryColumns, false, true)); //$NON-NLS-1$
		}else if ( query.getTypeKey().equals(SurveyObservationQuery.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_TYPE, SurveyQueryDataSource.getWaypointFeatureSchemaDef(this.queryColumns, false, true)); //$NON-NLS-1$
		}else if (query.getTypeKey().equals(SurveyWaypointQuery.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_TYPE, SurveyQueryDataSource.getWaypointFeatureSchemaDef(this.queryColumns, false, true)); //$NON-NLS-1$
		}else if (query.getTypeKey().equals(MissionTrackQuery.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.TRACKS_TYPE, SurveyQueryDataSource.getTrackFeatureSchemaDef(this.queryColumns, false, true)); //$NON-NLS-1$
		}
		return null;
		
	}
		
	/**
	 * Executes any tasks required before data is 
	 * written.  Here writers can be initialized
	 * and header lines written.
	 * 
	 * @throws Exception
	 */
	protected void init() throws Exception{
		super.init();
	}
	
	/**
	 * Executes any  tasks required after all
	 * data is written.
	 * @throws Exception
	 */
	protected void finish() throws Exception{
		super.finish();
	}
}

