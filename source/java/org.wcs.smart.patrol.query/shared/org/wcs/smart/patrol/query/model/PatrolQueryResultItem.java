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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IAdaptable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.util.ReprojectUtils;

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
public class PatrolQueryResultItem implements IGeometryResultItem, IAdaptable{

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
	private UUID caUuid;
	
	private String patrolId;
	private LocalDate patrolStartDate;
	private LocalDate patrolEndDate;
	private String station;
	private String team;
	private String objective;
	private String mandate;
	private PatrolType.Type patrolType;
	private UUID patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private UUID patrolLegUuid;
	private String transportType;
	private LocalDate plStartDate;
	private LocalDate plEndDate;
	private LocalDate wpDateTime;
	private LocalTime waypointTime;
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
	
	private String lastModifiedBy;
	private LocalDateTime lastModified;
	
	private UUID groupUuid;
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
	public LocalDate getPatrolStartDate() {
		return patrolStartDate;
	}
	/**
	 * @param patrolStartDate  patrol start date 
	 */
	public void setPatrolStartDate(LocalDate patrolStartDate) {
		this.patrolStartDate = patrolStartDate;
	}
	/**
	 * @return patrol end date
	 */
	public LocalDate getPatrolEndDate() {
		return patrolEndDate;
	}
	/**
	 * @param patrolEndDate patrol end date
	 */
	public void setPatrolEndDate(LocalDate patrolEndDate) {
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
		if (station == null) {
			this.station = ""; //$NON-NLS-1$
			return;
		}
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
		if (team == null) {
			this.team = ""; //$NON-NLS-1$
			return;
		}
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
		if (mandate == null) {
			this.mandate = ""; //$NON-NLS-1$
			return;
		}
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
	 * (optional)
	 * @return the patrol leg uuid 
	 */
	public UUID getPatrolLegUuid() {
		return patrolLegUuid;
	}
	/**
	 * sets the patrol leg uuid (optional)
	 * @param patrolUuid the patrol uuid
	 */
	public void setPatrolLegUuid(UUID legUuid) {
		this.patrolLegUuid = legUuid;
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
	public LocalDate getWpDateTime() {
		return wpDateTime;
	}
	/**
	 * @param wpDateTime waypoint date 
	 */
	public void setWpDateTime(LocalDate wpDateTime) {
		this.wpDateTime = wpDateTime;
	}
	/**
	 * @return waypoint time
	 */
	public LocalTime getWaypointTime() {
		return waypointTime;
	}
	/**
	 * @param wpTime waypoint time
	 */
	public void setWaypointTime(LocalTime wpTime) {
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
	public void setPatrolLegStartDate(LocalDate date){
		this.plStartDate = date;
	}
	public void setPatrolLegEndDate(LocalDate date){
		this.plEndDate = date;
	}
	
	public LocalDate getPatrolLegStartDate(){
		return this.plStartDate;
	}
	public LocalDate getPatrolLegEndDate(){
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
	 * Sets the ca uuid
	 * @param caId
	 */
	public void setConservationAreaUuid(UUID uuid) {
		this.caUuid = uuid;
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
	 * the conservation area uuid
	 * @return
	 */
	public UUID getConservationAreaUuid(){
		return this.caUuid;
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

	
	/**
	 * Converts the result item to a geometry in the database projection (4326)
	 */
	@Override
	public Geometry asGeometry(String columnName) {
		GeometryFactory gf = GeometryFactoryProvider.getFactory();
		if (columnName.equalsIgnoreCase(WAYPOINT_GEOMCOLUMN_KEY)){
			return gf.createPoint(new Coordinate(getWaypointX(null), getWaypointY(null)));
		}else if (columnName.equalsIgnoreCase(TRACK_GEOMCOLUMN_KEY)){
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
						}else if (g instanceof GeometryCollection) {
							GeometryCollection gc = (GeometryCollection)g;
							for (int j = 0; j < gc.getNumGeometries(); j ++) {
								Geometry x = gc.getGeometryN(j);
								if (x instanceof LineString) {
									lss.add((LineString)x);
								}else if (x instanceof MultiLineString) {
									MultiLineString mg = (MultiLineString)x;
									for (int k = 0; k < mg.getNumGeometries(); k ++){
										lss.add((LineString)mg.getGeometryN(k));
									}
								}
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
	
	@Override
	public int hashCode(){
		return Objects.hash(patrolUuid, patrolLegUuid, waypointUuid, observationUuid);
		
	}
	
	@Override
	public boolean equals(Object other){
		if (other == this) return true;
		if (other == null) return false;
		if (!other.getClass().equals(getClass())) return false;
		PatrolQueryResultItem o = (PatrolQueryResultItem) other;
		
		return Objects.equals(patrolLegUuid, o.patrolLegUuid) &&
				Objects.equals(patrolUuid, o.patrolUuid) &&
				Objects.equals(waypointUuid, o.waypointUuid) && 
				Objects.equals(observationUuid, o.observationUuid);
	}
}
