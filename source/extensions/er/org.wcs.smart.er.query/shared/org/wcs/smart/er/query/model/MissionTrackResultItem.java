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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * A class to hold the results of a mission track 
 * query.   
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MissionTrackResultItem implements IGeometryResultItem, ISurveyQueryResultItem, ISamplingUnitResultItem{

	public static final String TRACK_GEOMCOLUMN_KEY = "TrackGeomtry"; //$NON-NLS-1$
	
	private String caId;
	private String caName;
	private UUID caUuid;
	
	private String surveyDesign;
	
	private String surveyId;
	
	private String missionId;
	private LocalDate missionStart;
	private LocalDate missionEnd;
	private UUID missionUuid;
	
	private UUID samplingUnitUuid;
	private String samplingUnitId;
	private Double smaplingUnitBuffer;
	
	private UUID trackUuid;
	private LocalDate trackDate;
	private MissionTrack.TrackType trackType;
	private String trackId;
	private Double trackLength;
	
	private byte[] geometry;
	
	private HashMap<String, Object> missionProperties = new HashMap<String, Object>();
	private HashMap<String, Object> suAttributes = new HashMap<String, Object>();
	
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
	 * Finds the properties value of the associated sampling
	 * unit attribute key.
	 * 
	 * @param attributeKey sampling unit attribute key
	 * @return the value associated with the given key
	 */
	public Object getSamplingUnitAttributeValue(String attributeKey){
		return suAttributes.get(attributeKey);
	}
	
	/**
	 * Adds an sampling unit property value to the result 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addSamplingUnitAttributeValue(String key, Object value){
		suAttributes.put(key, value);
	}
	
	/**
	 * Sets the track uuid
	 * @param trackUuid
	 */
	public void setTrackUuid(UUID trackUuid){
		this.trackUuid = trackUuid;
	}
	
	/**
	 * @return the track uuid
	 */
	public UUID getTrackUuid(){
		return this.trackUuid;
	}
	
	/**
	 * Sets the track date attribute
	 * 
	 * @param leader
	 */
	public void setTrackDate(LocalDate trackDate){
		this.trackDate = trackDate;
	}
	
	/**
	 * Get the track date
	 * 
	 * @return
	 */
	public LocalDate getTrackDate(){
		return this.trackDate;
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
	 * sets the sampling unit uuid
	 * @param uuid
	 */
	public void setSamplingUnitUuid(UUID uuid){
		this.samplingUnitUuid = uuid;
	}
	
	/**
	 * 
	 * @return the sampling unit uuid
	 */
	public UUID getSamplingUnitUuid(){
		return this.samplingUnitUuid;
	}
	
	/**
	 * Sets the ca uuid
	 * @param uuid
	 */
	public void setConservationAreaUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	
	/**
	 * The ca uuid
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

	/**
	 * sampling unit id
	 * @return
	 */
	public String getSamplingUnitId() {
		return samplingUnitId;
	}

	public void setSamplingUnitId(String samplingUnitId) {
		this.samplingUnitId = samplingUnitId;
	}

	/**
	 * sampling unit buffer
	 * @return
	 */
	public Double getSmaplingUnitBuffer() {
		return smaplingUnitBuffer;
	}

	public void setSamplingUnitBuffer(Double smaplingUnitBuffer) {
		this.smaplingUnitBuffer = smaplingUnitBuffer;
	}
	
	public MissionTrack.TrackType getTrackType(){
		return this.trackType;
	}
	
	public void setTrackType(MissionTrack.TrackType trackType){
		this.trackType = trackType;
	}
	
	public String getTrackId(){
		return this.trackId;
	}
	
	public void setTrackId(String trackId){
		this.trackId = trackId;
	}
	
	public Double getTrackLength(){
		return this.trackLength;
	}
	public void setTrackLength(Double length){
		this.trackLength = length;
	}

	public byte[] getGeometry(){
		return this.geometry;
	}
	public void setGeometry(byte[] geometry){
		this.geometry = geometry;
	}
	
	private static final WKBReader reader = new WKBReader();
	
	@Override
	public Geometry asGeometry() {
		
		if (getGeometry() == null) return null;
		try{
			return reader.read(getGeometry());
		}catch (Exception ex){
			Logger.getLogger(MissionTrackResultItem.class.getName()).log(Level.WARNING, "Error parsing geometry.", ex); //$NON-NLS-1$
		}
		return null;
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
		data.add(getMissionId() + "." + UuidUtils.uuidToString(getTrackUuid())); //$NON-NLS-1$
		int i = 2;
		for (QueryColumn c : columns){
			if (c.isVisible() && !c.getKey().equalsIgnoreCase(geometryColumn.getKey())){
				data.add(QueryColumnUtils.getValue(this, c, type.getDescriptor(i++), Locale.getDefault()));
			}
		}		
		return SimpleFeatureBuilder.build(type, data, (String)data.get(1));
	}
	
	@Override
	public String getMissionLeader() {
		throw new UnsupportedOperationException();
	}

}
