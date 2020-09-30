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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.util.ReprojectUtils;

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
public class SurveyQueryResultItem implements IGeometryResultItem, IAdaptable{
	
	/**
	 * Waypoint geometry field name
	 */
	public static final String WAYPOINT_GEOMCOLUMN_KEY = "wp:geometry"; //$NON-NLS-1$
	/**
	 * Track geometry field name
	 */
	public static final String TRACK_GEOMCOLUMN_KEY = "track:geometry"; //$NON-NLS-1$
	
	private String caId;
	private String caName;
	
	private String surveyDesign;
	private LocalDate surveyDesignStart;
	private LocalDate surveyDesignEnd;
	
	private String surveyId;
	private LocalDate surveyStart;
	private LocalDate surveyEnd;
	
	private String missionId;
	private LocalDateTime missionStart;
	private LocalDateTime missionEnd;
	private UUID missionUuid;
	private String missionLeader;
	
	private UUID samplingUnitUuid;
	private String samplingUnitId;
	
	private LocalDateTime wpDateTime;
	
	private UUID waypointUuid;
	private int waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	private String waypointObserver;
	
	private String[] observationCategory;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private HashMap<String, Object> missionProperties = new HashMap<String, Object>();
	private HashMap<String, Object> suAttributes = new HashMap<String, Object>();
	
	private String lastModifiedBy;
	private LocalDateTime lastModified;
	
	private UUID groupUuid;
	private UUID observationUuid;
	
	private List<LineString> tracks;
	
	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationUuid(UUID observationUuid){
		this.observationUuid = observationUuid;
	}
	
	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationGroupUuid(UUID groupUuid){
		this.groupUuid = groupUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public UUID getObservationGroupUuid(){
		return this.groupUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public UUID getObservationUuid(){
		return this.observationUuid;
	}
	
	/**
	 * the waypoint last modified date
	 * @param lastModified
	 */
	public void setLastModifiedDate(LocalDateTime lastModified) {
		this.lastModified = lastModified;
	}
	/**
	 * 
	 * @return the waypoint last modified date
	 */
	public LocalDateTime getLastModifiedDate() {
		return this.lastModified;
	}
	
	/**
	 * @param lastmodified employee uuid or null
	 */
	public void setLastModifiedBy(String lastModifiedBy){
		this.lastModifiedBy = lastModifiedBy;
	}
	
	/**
	 * @return the last modified employee uuid or null
	 */
	public String getLastModifiedBy(){
		return this.lastModifiedBy;
	}
	
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
	 * Each item is associated with a single category.  This
	 * returns an array of the names of the category and
	 * all the parent categories:
	 *   - {parent1, parent2, category}
	 * 
	 * @return an array of the category names of the category & parent categories
	 */
	public String[] getCategories(){
		return this.observationCategory;
	}
	
	/**
	 * @param cat sets the category
	 */
	public void setCategory(String[] categoryLabels){
		this.observationCategory = categoryLabels;
	}
	
	/**
	 * Finds the attribute value of the associated attribute
	 * key.
	 * 
	 * @param attributeKey the attribute key
	 * @return the value associated with the attribute given key
	 */
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	/**
	 * Sets the observation attribute values
	 * @param attributes
	 */
	public void setAttributes(HashMap<String, Object> attributes){
		this.attributes = attributes;
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
	 * sets the waypoint uuid
	 * @param uuid
	 */
	public void setWaypointUuid(UUID uuid){
		this.waypointUuid = uuid;
	}
	/**
	 * 
	 * @return the waypoint uuid
	 */
	public UUID getWaypointUuid(){
		return this.waypointUuid;
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
	 * @return waypoint date 
	 */
	public LocalDateTime getWaypointDateTime() {
		return wpDateTime;
	}
	/**
	 * @param wpDateTime waypoint date 
	 */
	public void setWaypointDateTime(LocalDateTime wpDateTime) {
		this.wpDateTime = wpDateTime;
	}

	/**
	 * @return waypoint id
	 */
	public int getWaypointId() {
		return waypointId;
	}
	/**
	 * @param waypointId waypoint id
	 */
	public void setWaypointId(int waypointId) {
		this.waypointId = waypointId;
	}

	/**
	 * Return the raw x location associated with the waypoint in the 
	 * provided projection. 
	 * @param crs optional if null the lat/long value will be returned
	 * @return
	 */
	public double getWaypointRawX(CoordinateReferenceSystem crs) {
		if (crs == null) return waypointX;
		return ReprojectUtils.transform(waypointX, waypointY, crs).getX();
	}
	
	/**
	 * Return the raw y location associated with the waypoint in the 
	 * provided projection. 
	 * @param crs optional if null the lat/long value will be returned
	 * @return
	 */
	public double getWaypointRawY(CoordinateReferenceSystem crs) {
		if (crs == null) return waypointY;
		return ReprojectUtils.transform(waypointX, waypointY, crs).getY();
	}
	
	
	/**
	 * @param crs options - if null the lat/long value will be returned.
	 * 
	 * @return waypoint x (longitude) position  If a distance/direction value is
	 * associated with this waypoint then this returns the projected value otherwise
	 * it will return the original value.
	 * 
	 */
	public double getWaypointX(CoordinateReferenceSystem crs) {
		if (waypointDistance == null || waypointDirection == null) return getWaypointRawX(crs);
		
		Coordinate prj = Waypoint.projectPoint(new Coordinate(waypointX, waypointY), waypointDistance, waypointDirection); 		
		if (crs == null) return prj.x;
		return ReprojectUtils.transform(prj.x, prj.y, crs).getX();
	}
	
	/**
	 * @param crs options - if null the lat/long value will be returned.
	 * 
	 * @return waypoint y (latitude) position  If a distance/direction value is
	 * associated with this waypoint then this returns the projected value otherwise
	 * it will return the original value.
	 * 
	 */
	public double getWaypointY(CoordinateReferenceSystem crs) {
		if (waypointDistance == null || waypointDirection == null) return getWaypointRawY(crs);
		
		Coordinate prj = Waypoint.projectPoint(new Coordinate(waypointX, waypointY), waypointDistance, waypointDirection); 		
		if (crs == null) return prj.y;
		return ReprojectUtils.transform(prj.x, prj.y, crs).getY();
	}
	
	/**
	 * @param waypointX waypoint y (longitude)
	 */
	public void setWaypointX(double waypointX) {
		this.waypointX = waypointX;
	}

	/**
	 * @param waypointY the waypoint y (latitude)
	 */
	public void setWaypointY(double waypointY) {
		this.waypointY = waypointY;
	}
	
	/**
	 * @return waypoint distance observation
	 */
	public Float getWaypointDistance() {
		return waypointDistance;
	}
	/**
	 * @param waypointDistance
	 */
	public void setWaypointDistance(Float waypointDistance) {
		this.waypointDistance = waypointDistance;
	}
	
	/**
	 * @return the waypoint direction of observation
	 */
	public Float getWaypointDirection() {
		return waypointDirection;
	}
	/**
	 * @param waypointDirection direction of observation
	 */
	public void setWaypointDirection(Float waypointDirection) {
		this.waypointDirection = waypointDirection;
	}
	
	/**
	 * @return waypoint comment
	 */
	public String getWaypointComment() {
		return waypointComment;
	}
	/**
	 * @param wpComment wyapoint comment
	 */
	public void setWaypointComment(String wpComment) {
		this.waypointComment = wpComment;
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
	 * survey design start
	 * @return
	 */
	public LocalDate getSurveyDesignStart() {
		return surveyDesignStart;
	}

	public void setSurveyDesignStart(LocalDate surveyDesignStart) {
		this.surveyDesignStart = surveyDesignStart;
	}

	/**
	 * survey design end
	 * @return
	 */
	public LocalDate getSurveyDesignEnd() {
		return surveyDesignEnd;
	}

	public void setSurveyDesignEnd(LocalDate surveyDesignEnd) {
		this.surveyDesignEnd = surveyDesignEnd;
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
	 * survey start
	 * @return
	 */
	public LocalDate getSurveyStart() {
		return surveyStart;
	}

	public void setSurveyStart(LocalDate surveyStart) {
		this.surveyStart = surveyStart;
	}

	/**
	 * survey end
	 * @return
	 */
	public LocalDate getSurveyEnd() {
		return surveyEnd;
	}

	public void setSurveyEnd(LocalDate surveyEnd) {
		this.surveyEnd = surveyEnd;
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
	public LocalDateTime getMissionStart() {
		return missionStart;
	}

	public void setMissionStart(LocalDateTime missionStart) {
		this.missionStart = missionStart;
	}

	/**
	 * mission end
	 * @return
	 */
	public LocalDateTime getMissionEnd() {
		return missionEnd;
	}

	public void setMissionEnd(LocalDateTime missionEnd) {
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
	 * the waypoint observer
	 * @return
	 */
	public String getWaypointObserver(){
		return this.waypointObserver;
	}
	
	public void setWaypointObserver(String observer){
		this.waypointObserver = observer;
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
	public Geometry asGeometry(String columnName) {
		if (columnName.equals(WAYPOINT_GEOMCOLUMN_KEY)){
			return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(getWaypointX(null), getWaypointY(null)));
		}else if (columnName.equals(TRACK_GEOMCOLUMN_KEY)){
			if (getTracks() == null){
				return GeometryFactoryProvider.getFactory().createMultiLineString(new LineString[]{});
			}
			return GeometryFactoryProvider.getFactory().createMultiLineString(getTracks().toArray(new LineString[getTracks().size()]));
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(Waypoint.class) && getWaypointUuid() != null){
			Waypoint wp = new Waypoint();
			wp.setUuid(getWaypointUuid());
			return (T)wp;
		}
		if (adapter.equals(WaypointObservation.class) && getObservationUuid() != null){
			WaypointObservation wo = new WaypointObservation();
			wo.setUuid(getObservationUuid());
			return (T)wo;
		}
		return null;
	}
}
