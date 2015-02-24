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
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.query.map.geotools.SurveyQueryDataSource;
import org.wcs.smart.er.query.map.geotools.SurveyResultItemFeature;
import org.wcs.smart.er.query.model.MissionQueryType;
import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyWaypointQueryType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.importexport.ShapeQueryExporter;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.Query;

/**
 * Shapefile query exporter.  Exports
 * the results of a query to a shapefile.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyShapeQueryExporter extends ShapeQueryExporter{

	private Session session = null;
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.export.IQueryExporter#canExport(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean canExport(Query query) {
		if (query.getType().getKey().equals(SurveyObservationQueryType.KEY) ||
				query.getType().getKey().equals(SurveyWaypointQueryType.KEY)||
				query.getType().getKey().equals(MissionQueryType.KEY) ||
				query.getType().getKey().equals(MissionTrackQueryType.KEY)	){
			return true;
		}
		return false;
	}

	@Override
	protected SimpleFeature createFeature(IResultItem it, IQueryType queryType) throws Exception{
		if (queryType.getKey().equals(MissionQueryType.KEY)){
			return SurveyResultItemFeature.createTrackFeature((SurveyQueryResultItem)it,  queryColumns, shapefile.getSchema(shapefile.getTypeNames()[0]));
		}else if ( query.getType().getKey().equals(SurveyObservationQueryType.KEY) ||
				    query.getType().getKey().equals(SurveyWaypointQueryType.KEY)){
			return SurveyResultItemFeature.createObservationFeature((SurveyQueryResultItem)it, queryColumns, shapefile.getSchema(shapefile.getTypeNames()[0]));
		}else if (query.getType().getKey().equals(MissionTrackQueryType.KEY)){
			return SurveyResultItemFeature.createTrackFeature((MissionTrackResultItem)it, session, queryColumns, shapefile.getSchema(shapefile.getTypeNames()[0]));
		}
		return null;
	}
	
	@Override
	protected SimpleFeatureType createSchema(IQueryType queryType) throws Exception{
		if (queryType.getKey().equals(MissionQueryType.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.TRACKS_TYPE, SurveyQueryDataSource.getTrackFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
		}else if ( query.getType().getKey().equals(SurveyObservationQueryType.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_TYPE, SurveyQueryDataSource.getWaypointFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
		}else if (query.getType().getKey().equals(SurveyWaypointQueryType.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_TYPE, SurveyQueryDataSource.getWaypointFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
		}else if (query.getType().getKey().equals(MissionTrackQueryType.KEY)){
			return DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.TRACKS_TYPE, SurveyQueryDataSource.getTrackFeatureSchemaDef(this.queryColumns, false)); //$NON-NLS-1$
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
		session = HibernateManager.openSession();
	}
	
	/**
	 * Executes any  tasks required after all
	 * data is written.
	 * @throws Exception
	 */
	protected void finish() throws Exception{
		super.finish();
		session.close();
	}
}

