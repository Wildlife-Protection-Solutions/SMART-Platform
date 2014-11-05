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
import java.util.HashMap;
import java.util.List;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryDataSource extends AbstractDataStore{

	public static final String FEATURETYPE_PREFIX = "smart"; //$NON-NLS-1$
	/**
	 * waypoint query data source
	 */
	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	
	/**
	 * mission tracks query data source
	 */
	public static final String WAYPOINT_MISSION_TRACK_TYPE = "WaypointMissionTracks"; //$NON-NLS-1$

	/**
	 * mission tracks query data source
	 */
	public static final String TRACKS_TYPE = "MissionTracks"; //$NON-NLS-1$
	
	private SimpleQuery query;
	
	private HashMap<String, SimpleFeatureType> schemas = new HashMap<String, SimpleFeatureType>();
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public SurveyQueryDataSource(SimpleQuery query){
		this.query = query;
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
	}

	/**
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	public String[] getTypeNames()  {
		if (query instanceof MissionQuery || 
			query instanceof MissionTrackQuery){
			return new String[]{TRACKS_TYPE};
		}else{
			return new String[]{WAYPOINT_TYPE, WAYPOINT_MISSION_TRACK_TYPE};
		}
	}
	
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		if (typeName.equals(WAYPOINT_TYPE)){
			return new SurveyFeatureReader(this.query, getSchema(typeName));
		}else if (typeName.equals(WAYPOINT_MISSION_TRACK_TYPE)){
			return new MissionFeatureReader(this.query, getSchema(typeName));
		}else if (typeName.equals(TRACKS_TYPE)){
			return new MissionFeatureReader(this.query, getSchema(typeName));
		}
		return null;
	}

	/**
	 * Removes the cached schemas 
	 */
	public void resetSchema(String typeName){
		schemas.remove(typeName);
	}
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getSchema(java.lang.String)
	 */
	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		SimpleFeatureType type = schemas.get(typeName);
		if (type == null){
			try {
				if (typeName.equals(WAYPOINT_TYPE)) {
					type = createWaypointSchema();
				}else if (typeName.equals(WAYPOINT_MISSION_TRACK_TYPE)){
					type = createMissionTrackSchema();
				}else if (typeName.equals(TRACKS_TYPE)){
					type = createTrackSchema();
				}
			}catch(SchemaException ex){
				throw new IOException(Messages.SurveyObsQueryDataSource_SchemaError + ex.getLocalizedMessage(), ex);
			}
			schemas.put(typeName, type);
		}
		return type;
	}

	/**
	 * Creates the simple feature type for the query
	 * 
	 * @return the simple feature type for the query
	 * 
	 * @throws SchemaException
	 */
	private SimpleFeatureType createWaypointSchema() throws SchemaException{
		SimpleFeatureType type =  DataUtilities.createType(FEATURETYPE_PREFIX + "." + WAYPOINT_TYPE, getWaypointFeatureSchemaDef(query.getQueryColumns(), true)); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createMissionTrackSchema() throws SchemaException{
		SimpleFeatureType type = DataUtilities.createType(FEATURETYPE_PREFIX + "." + WAYPOINT_MISSION_TRACK_TYPE, getMissionTrackFeatureSchemaDef()); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createTrackSchema() throws SchemaException{
		SimpleFeatureType type = DataUtilities.createType(FEATURETYPE_PREFIX + "." + TRACKS_TYPE, getTrackFeatureSchemaDef(query.getQueryColumns(), true)); //$NON-NLS-1$
		return type;
	}
	
	public static String getWaypointFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String"); //$NON-NLS-1$
		sb.append(QueryColumn.createFeatureDefinitionString(columns, supportsTime));
		sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
		return sb.toString();
	}
	
	public static String getTrackFeatureSchemaDef(List<QueryColumn> columns, boolean supportsTime){
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String"); //$NON-NLS-1$
		sb.append(QueryColumn.createFeatureDefinitionString(columns, supportsTime));
		sb.append(",geom:MultiLineString:srid=4326"); //$NON-NLS-1$
		return sb.toString();
	}
	
	public static String getMissionTrackFeatureSchemaDef(){
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String,"); //$NON-NLS-1$
		sb.append("id:String,"); //$NON-NLS-1$
		sb.append("start:Date,"); //$NON-NLS-1$
		sb.append("end:Date,"); //$NON-NLS-1$
		sb.append("comment:String,"); //$NON-NLS-1$
		sb.append("geom:MultiLineString:srid=4326"); //$NON-NLS-1$
		return sb.toString();
	}
	
}
