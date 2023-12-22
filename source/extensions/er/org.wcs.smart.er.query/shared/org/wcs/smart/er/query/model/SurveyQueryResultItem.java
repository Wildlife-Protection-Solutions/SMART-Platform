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
package org.wcs.smart.er.query.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

/**
 * A class to hold the results of a survey waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes. Also
 * included are all mission attributes in the query.
 * 
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryResultItem implements ISurveyQueryResultItem, IGeometryResultItem {
	
	
	/**
	 * Track geometry field name
	 */
	public static final String TRACK_GEOMCOLUMN_KEY = "track:geometry"; //$NON-NLS-1$
	
	private String caId;
	private String caName;
	private UUID caUuid;

	private String surveyDesign;
	private String surveyId;
	
	private String missionId;
	private LocalDate missionStart;
	private LocalDate missionEnd;
	private UUID missionUuid;
	private String missionLeader;
	
	private HashMap<String, Object> missionProperties = new HashMap<String, Object>();
	
	private List<LineString> tracks;
	/**
	 * Sets the mission leader attribute
	 * 
	 * @param leader
	 */
	public void setMissionLeader(String leader){
		this.missionLeader = leader;
	}
	
	/**
	 * Get the mission leader
	 * 
	 * @return
	 */
	public String getMissionLeader(){
		return this.missionLeader;
	}
	
		
	/**
	 * Finds the mission property with the associated
	 * attribute key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getMissionPropertyValue(String attributeKey){
		return missionProperties.get(attributeKey);
	}
	
	/**
	 * Adds a mission property to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addMissionPropertyValue(String key, Object value){
		missionProperties.put(key, value);
	}
	
	
	
	/**
	 * sets the mission uuid
	 * @param uuid
	 */
	public void setMissionUuid(UUID uuid){
		this.missionUuid = uuid;
	}
	/**
	 * 
	 * @return the mission uuid
	 */
	public UUID getMissionUuid(){
		return this.missionUuid;
	}
	
	
	/**
	 * Sets the conservation area uuid
	 * @param uuid
	 */
	public void setConservationAreaUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	
	/**
	 * Gets the conservation area uuid
	 * @return
	 */
	public UUID getConservationAreaUuid() {
		return this.caUuid;
	}
	
	/**
	 * Sets the ca id
	 * @param caId
	 */
	public void setConservationAreaId(String caId){
		this.caId = caId;
	}
	
	/**
	 * Sets the ca name
	 * @param caName
	 */
	public void setConservationAreaName(String caName){
		this.caName = caName;
	}
	
	/**
	 * 
	 * @return this conservation area id
	 */
	public String getConservationAreaId(){
		return this.caId;
	}
	/**
	 * the conservation area name
	 * @return
	 */
	public String getConservationAreaName(){
		return this.caName;
	}

	/**
	 * 
	 * @return the survey design name
	 */
	public String getSurveyDesign() {
		return surveyDesign;
	}

	/**
	 * 
	 * @param surveyDesign the survey design name
	 */
	public void setSurveyDesign(String surveyDesign) {
		this.surveyDesign = surveyDesign;
	}

	/**
	 * survey id
	 * @return
	 */
	public String getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(String surveyId) {
		this.surveyId = surveyId;
	}

	/**
	 * mission id
	 * @return
	 */
	public String getMissionId() {
		return missionId;
	}

	public void setMissionId(String missionId) {
		this.missionId = missionId;
	}

	/**
	 * mission start
	 * @return
	 */
	public LocalDate getMissionStart() {
		return missionStart;
	}

	public void setMissionStart(LocalDate missionStart) {
		this.missionStart = missionStart;
	}

	/**
	 * mission end
	 * @return
	 */
	public LocalDate getMissionEnd() {
		return missionEnd;
	}

	public void setMissionEnd(LocalDate missionEnd) {
		this.missionEnd = missionEnd;
	}
	
	
	public List<LineString> getTracks(){
		return this.tracks;
	}
	public void addTracks(LineString track){
		if (this.tracks == null){
			tracks = new ArrayList<LineString>();
		}
		tracks.add(track);
	}

	@Override
	public Geometry asGeometry() {
		if (getTracks() == null){
			return GeometryFactoryProvider.getFactory().createMultiLineString(new LineString[]{});
		}
		return GeometryFactoryProvider.getFactory().createMultiLineString(getTracks().toArray(new LineString[getTracks().size()]));
	}

	/**
	 * Converts a query result item to a feature.
	 * The feature type must have been generated 
	 * from the same set of query table columns.
	 * 
	 * @param it the query result item 
	 * @param columns the columns that make up the feature type
	 * @param ftype the feature type 
	 * @return created feature 
	 */
	
	@Override
	public SimpleFeature toSimpleFeature(SimpleFeatureType type, QueryColumn geometryColumn,
			List<QueryColumn> columns) {
		
		List<Object> data = new ArrayList<Object>();
			
		data.add(geometryColumn.getValue(this));
		data.add(getMissionId() + "." + System.nanoTime()); //$NON-NLS-1$
		int i = 2;
		for (QueryColumn c : columns){
			if (c.isVisible() && !c.getKey().equalsIgnoreCase(geometryColumn.getKey())){
				data.add(QueryColumnUtils.getValue(this, c, type.getDescriptor(i++), Locale.getDefault()));
			}
		}			
		return SimpleFeatureBuilder.build(type, data, (String)data.get(1));
		
	}
	
	
}
