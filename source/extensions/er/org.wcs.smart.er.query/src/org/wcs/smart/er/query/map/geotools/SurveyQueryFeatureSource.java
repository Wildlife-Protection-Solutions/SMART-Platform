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
package org.wcs.smart.er.query.map.geotools;

import java.io.IOException;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryFeatureSource extends ContentFeatureSource{


	public SurveyQueryFeatureSource(ContentEntry entry) {
		super(entry, Query.ALL);
	}

	public SurveyQueryDataSource getSource() {
		return (SurveyQueryDataSource)entry.getDataStore();
	}



	/**
	 * Creates the simple feature type for the query
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */



	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		return null;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		return -1;
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		if (entry.getTypeName().equals(SurveyQueryDataSource.WAYPOINT_TYPE)){
			return new SurveyFeatureReader(getSource().getQuery(), getSchema(), getSource().getColumns());
		}else if (entry.getTypeName().equals(SurveyQueryDataSource.WAYPOINT_MISSION_TRACK_TYPE)){
			return new MissionFeatureReader(getSource().getQuery(), getSchema(), getSource().getColumns());
		}else if (entry.getTypeName().equals(SurveyQueryDataSource.TRACKS_TYPE)){
			return new MissionFeatureReader(getSource().getQuery(), getSchema(), getSource().getColumns());
		}
		return null;
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		try {
			if (entry.getTypeName().equals(SurveyQueryDataSource.WAYPOINT_TYPE)) {
				return createWaypointSchema();
			}else if (entry.getTypeName().equals(SurveyQueryDataSource.WAYPOINT_MISSION_TRACK_TYPE)){
				return createMissionTrackSchema();
			}else if (entry.getTypeName().equals(SurveyQueryDataSource.TRACKS_TYPE)){
				return createTrackSchema();
			}
		}catch(SchemaException ex){
			throw new IOException(Messages.SurveyObsQueryDataSource_SchemaError + ex.getLocalizedMessage(), ex);
		}
		return null;
	}
	
	
	
	
	private SimpleFeatureType createWaypointSchema() throws SchemaException{
		
		SimpleFeatureType type =  DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_TYPE, getWaypointFeatureSchemaDef(getSource().getColumns(), true, false)); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createMissionTrackSchema() throws SchemaException{
		SimpleFeatureType type = DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.WAYPOINT_MISSION_TRACK_TYPE, getMissionTrackFeatureSchemaDef()); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createTrackSchema() throws SchemaException{
		SimpleFeatureType type = DataUtilities.createType(SurveyQueryDataSource.FEATURETYPE_PREFIX + "." + SurveyQueryDataSource.TRACKS_TYPE, getTrackFeatureSchemaDef(getSource().getColumns(), true, false)); //$NON-NLS-1$
		return type;
	}
	
	public static String getWaypointFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:Point:srid=4326,fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}
	
	public static String getTrackFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime, boolean forShape){
		StringBuilder sb = new StringBuilder();
		sb.append("the_geom:MultiLineString:srid=4326,fid:String"); //$NON-NLS-1$
		sb.append(QueryColumnUtils.createFeatureDefinitionString(columns, supportsTime, forShape));
		return sb.toString();
	}
	
	public static String getMissionTrackFeatureSchemaDef(){
		StringBuilder sb = new StringBuilder();
		sb.append("geom:MultiLineString:srid=4326,"); //$NON-NLS-1$
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("id:String,"); //$NON-NLS-1$
		sb.append("start:Date,"); //$NON-NLS-1$
		sb.append("end:Date,"); //$NON-NLS-1$
		sb.append("comment:String"); //$NON-NLS-1$
		return sb.toString();
	}
}
