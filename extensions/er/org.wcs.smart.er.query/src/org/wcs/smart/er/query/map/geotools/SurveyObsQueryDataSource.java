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
import java.util.HashSet;
import java.util.List;

import org.geotools.data.AbstractDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyObsQueryDataSource extends AbstractDataStore{

	/**
	 * waypoint query data source
	 */
	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	
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
	public SurveyObsQueryDataSource(SurveyObservationQuery query){
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
		return new String[]{WAYPOINT_TYPE, TRACKS_TYPE};
	}
	
	
	/**
	 * @see org.geotools.data.AbstractDataStore#getFeatureReader(java.lang.String)
	 */
	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
		if (typeName.equals(WAYPOINT_TYPE)){
			return new SurveyFeatureReader(this.query, getSchema(typeName));
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
				}else if (typeName.equals(TRACKS_TYPE)){
					type = createMissionTrackSchema();
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
		SimpleFeatureType type =  DataUtilities.createType("smart." + WAYPOINT_TYPE, getWaypointFeatureSchemaDef(query.getQueryColumns())); //$NON-NLS-1$
		return type;
	}
	
	private SimpleFeatureType createMissionTrackSchema() throws SchemaException{
		SimpleFeatureType type = DataUtilities.createType("smart." + TRACKS_TYPE, getMissionTrackFeatureSchemaDef()); //$NON-NLS-1$
		return type;
	}
	
	public static String getWaypointFeatureSchemaDef(List<QueryColumn> columns){
		StringBuilder sb = new StringBuilder();
		sb.append("fid:String"); //$NON-NLS-1$
		HashSet<String> names = new HashSet<String>();
		for (int i = 0; i < columns.size(); i++){
			sb.append(","); //$NON-NLS-1$
			String name = columns.get(i).getName();
			name = name.replaceAll(" ", "_");  //$NON-NLS-1$//$NON-NLS-2$
			name = name.replaceAll("[^\\p{L}\\p{Nd}_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
			
			String tempname = name;
			int cnt = 1;
			while(names.contains(tempname)){
				tempname = name + "_" + cnt; //$NON-NLS-1$
				cnt++;
			}
			sb.append(tempname);
			sb.append(":"); //$NON-NLS-1$
			sb.append(columns.get(i).getType().geotoolsType);
		}
		sb.append(",geom:Point:srid=4326"); //$NON-NLS-1$
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
