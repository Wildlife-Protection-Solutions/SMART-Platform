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
package org.wcs.smart.patrol.query.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.common.engine.IGeometryResultItem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * A class to hold the results of a waypoint 
 * query.  Each class contains the results for
 * a single observation.  The observation contains
 * a single category and all attributes.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolQueryResultItem implements IGeometryResultItem{

	/**
	 * Waypoint geometry field name
	 */
	public static final String WAYPOINT_GEOMCOLUMN_KEY = "wp:geometry"; //$NON-NLS-1$
	
	/**
	 * Track geometry field name
	 */
	public static final String TRACK_GEOMCOLUMN_KEY = "track:geometry"; //$NON-NLS-1$
	
	private static final GeometryFactory gf = new GeometryFactory();
	private String caId;
	private String caName;
	
	private String patrolId;
	private Date patrolStartDate;
	private Date patrolEndDate;
	private String station;
	private String team;
	private String objective;
	private String mandate;
	private PatrolType.Type patrolType;
	private UUID patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private String transportType;
	private Date plStartDate;
	private Date plEndDate;
	private Date wpDateTime;
	private Date waypointTime;
	private String leader;
	private String pilot;
	
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
	
	private UUID observationUuid;
	private List<byte[]> tracks = null;
	

	/**
	 * @return the patrol id
	 */
	public String getPatrolId() {
		return patrolId;
	}
	
	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationUuid(UUID observationUuid){
		this.observationUuid = observationUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public UUID getObservationUuid(){
		return this.observationUuid;
	}
	
	/**
	 * @return the patrol-leg leader
	 */
	public String getLeader(){
		return this.leader;
	}
	/**
	 * @param leader the patrol leader
	 */
	public void setLeader(String leader){
		this.leader = leader;
	}
	/**
	 * @return the patrol-leg pilot
	 */
	public String getPilot(){
		return this.pilot;
	}
	/**
	 * @param leader the pilot leader
	 */
	public void setPilot(String pilot){
		this.pilot = pilot;
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
	
	public void setAttributes(HashMap<String, Object> attributes) {
		this.attributes = attributes;
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
	 * Adds an attribute to the observation results 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addAttribute(String key, Object value){
		attributes.put(key, value);
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
	 * @param patrolId patrol id
	 */
	public void setPatrolId(String patrolId) {
		this.patrolId = patrolId;
	}
	/**
	 * @return patrol start date
	 */
	public Date getPatrolStartDate() {
		return patrolStartDate;
	}
	/**
	 * @param patrolStartDate  patrol start date 
	 */
	public void setPatrolStartDate(Date patrolStartDate) {
		this.patrolStartDate = patrolStartDate;
	}
	/**
	 * @return patrol end date
	 */
	public Date getPatrolEndDate() {
		return patrolEndDate;
	}
	/**
	 * @param patrolEndDate patrol end date
	 */
	public void setPatrolEndDate(Date patrolEndDate) {
		this.patrolEndDate = patrolEndDate;
	}
	
	
	/**
	 * @return patrol station name
	 */
	public String getStation() {
		return station;
	}
	
	/**
	 * @param station patrol station name
	 */
	public void setStation(String station) {
		this.station = station;
	}
	
	/**
	 * @return patrol team name 
	 */
	public String getTeam() {
		return team;
	}
	/**
	 * @param team patrol team name
	 */
	public void setTeam(String team) {
		this.team = team;
	}
	
	/**
	 * @return patrol objective 
	 */
	public String getObjective() {
		return objective;
	}
	/**
	 * @param objective patrol objective
	 */
	public void setObjective(String objective) {
		this.objective = objective;
	}
	
	/**
	 * @return patrol mandate
	 */
	public String getMandate() {
		return mandate;
	}
	/**
	 * @param mandate the patrol mandate
	 */
	public void setMandate(String mandate) {
		this.mandate = mandate;
	}
	
	/**
	 * @return the patrol type 
	 */
	public PatrolType.Type getPatrolType() {
		return patrolType;
	}
	/**
	 * @param patrolType the patrol type
	 */
	public void setPatrolType(PatrolType.Type patrolType) {
		this.patrolType = patrolType;
	}
	
	/**
	 * @return the patrol uuid
	 */
	public UUID getPatrolUuid() {
		return patrolUuid;
	}
	/**
	 * @param patrolUuid the patrol uuid
	 */
	public void setPatrolUuid(UUID patrolUuid) {
		this.patrolUuid = patrolUuid;
	}
	/**
	 * @return if the patrol is armed or not
	 */
	public boolean isArmed() {
		return armed;
	}
	/**
	 * @param armed if the patrol is armed or not
	 */
	public void setArmed(boolean armed) {
		this.armed = armed;
	}
	/**
	 * @return patrol leg id
	 */
	public String getPatrolLegId() {
		return patrolLegId;
	}
	/**
	 * @param patrolLegId patrol leg id
	 */
	public void setPatrolLegId(String patrolLegId) {
		this.patrolLegId = patrolLegId;
	}
	/**
	 * @return patrol transport type
	 */
	public String getTransportType() {
		return transportType;
	}
	/**
	 * @param transportType patrol transport type
	 */
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}
	/**
	 * @return waypoint date 
	 */
	public Date getWpDateTime() {
		return wpDateTime;
	}
	/**
	 * @param wpDateTime waypoint date 
	 */
	public void setWpDateTime(Date wpDateTime) {
		this.wpDateTime = wpDateTime;
	}
	/**
	 * @return waypoint time
	 */
	public Date getWaypointTime() {
		return waypointTime;
	}
	/**
	 * @param wpTime waypoint time
	 */
	public void setWaypointTime(Date wpTime) {
		this.waypointTime = wpTime;
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
	 * @return waypoint x (longitude) position
	 */
	public double getWaypointX() {
		return waypointX;
	}
	/**
	 * @param waypointX waypoint y (longitude)
	 */
	public void setWaypointX(double waypointX) {
		this.waypointX = waypointX;
	}
	
	
	/**
	 * @return the waypoint y (latitude)
	 */
	public double getWaypointY() {
		return waypointY;
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
	public void setPatrolLegStartDate(Date date){
		this.plStartDate = date;
	}
	public void setPatrolLegEndDate(Date date){
		this.plEndDate = date;
	}
	
	public Date getPatrolLegStartDate(){
		return this.plStartDate;
	}
	public Date getPatrolLegEndDate(){
		return this.plEndDate;
	}

	public List<byte[]> getTrack(){
		return this.tracks;
	}
	public void addTrack(byte[] track){
		if (track == null || track.length == 0){
			return;
		}
		if (this.tracks == null){
			this.tracks = new ArrayList<byte[]>();
		}
		this.tracks.add(track);
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
	 * the waypoint observer
	 * @return
	 */
	public String getWaypointObserver(){
		return this.waypointObserver;
	}
	
	public void setWaypointObserver(String observer){
		this.waypointObserver = observer;
	}

	@Override
	public Geometry asGeometry(String columnName) {
		if (columnName == WAYPOINT_GEOMCOLUMN_KEY){
			return gf.createPoint(new Coordinate(getWaypointX(), getWaypointY()));
		}else if (columnName == TRACK_GEOMCOLUMN_KEY){
			if (getTrack() == null || getTrack().isEmpty()){
				return gf.createMultiLineString(new LineString[]{});
			}else {
				try {
					WKBReader reader = new WKBReader();
					List<byte[]> tracks = getTrack();
					List<LineString> lss = new ArrayList<LineString>();
					for (int i = 0; i < tracks.size(); i ++){
						Geometry g = reader.read(tracks.get(i));
						if (g instanceof LineString){
							lss.add((LineString)g);
						}else if (g instanceof MultiLineString){
							MultiLineString mg = (MultiLineString)g;
							for (int j = 0; j < mg.getNumGeometries(); j ++){
								lss.add((LineString)mg.getGeometryN(j));
							}
						}
					}
					return gf.createMultiLineString(lss.toArray(lss.toArray(new LineString[lss.size()])));
				} catch (ParseException e) {
					Logger.getLogger(PatrolQueryResultItem.class.getName()).log(Level.WARNING, "Error parsing track geometry.", e); //$NON-NLS-1$
					return gf.createMultiLineString(new LineString[]{});
				}
			}
		}
		return null;
	}
}
