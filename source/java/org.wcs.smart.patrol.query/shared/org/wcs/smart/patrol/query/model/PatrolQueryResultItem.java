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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;

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
public class PatrolQueryResultItem implements IGeometryResultItem, IPatrolQueryResultItem {

	
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
	private String patrolType;
	private UUID patrolTransportTypeUuid;
	private UUID patrolTypeUuid;
	private UUID patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private UUID patrolLegUuid;
	private String transportType;
	private String transportGroup;
	private LocalDate plStartDate;
	private LocalDate plEndDate;
	
	private LocalDateTime minDateTime;
	private LocalDateTime maxDateTime;
	
	private String leader;
	private String pilot;
	private String members;
	
	private List<byte[]> tracks = null;
	
	private Map<String, Object> patrolAttributes = new HashMap<>();

	/**
	 * @return the patrol id
	 */
	public String getPatrolId() {
		return patrolId;
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
	 * @return the patrol-leg members
	 */
	public String getMembers(){
		return this.members;
	}
	/**
	 * @param members the patrol members
	 */
	public void setMembers(String members){
		this.members = members;
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
	public String getPatrolType() {
		return patrolType;
	}
	/**
	 * @param patrolType the patrol type
	 */
	public void setPatrolType(String patrolType) {
		this.patrolType = patrolType;
	}
	
	/**
	 * @return the patrol transport type uuid
	 */
	public UUID getPatrolTransportTypeUuid() {
		return patrolTransportTypeUuid;
	}
	/**
	 * @param patrolType the patrol transport type uuid
	 */
	public void setPatrolTransportTypeUuid(UUID patrolTransportTypeUuid) {
		this.patrolTransportTypeUuid = patrolTransportTypeUuid;
	}
	
	/**
	 * @return the patrol type uuid
	 */
	public UUID getPatrolTypeUuid() {
		return patrolTypeUuid;
	}
	/**
	 * @param patrolType the patrol type uui
	 */
	public void setPatrolTypeUuid(UUID patrolTypeUuid) {
		this.patrolTypeUuid = patrolTypeUuid;
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
	 * @return patrol transport group
	 */
	public String getTransportGroup() {
		return transportGroup;
	}
	/**
	 * @param transportType patrol transport group
	 */
	public void setTransportGroup(String transportGroup) {
		this.transportGroup = transportGroup;
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
	 * Converts the result item to a geometry in the database projection (4326)
	 */
	@Override
	public Geometry asGeometry() {
		
		
			GeometryFactory gf = GeometryFactoryProvider.getFactory();
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
	
	public Object getPatrolAttribute(String keyId) {
		return patrolAttributes.get(keyId);
	}
	
	public void setPatrolAttribute(String keyId, Object value) {
		patrolAttributes.put(keyId, value);
	}
	
	@Override
	public LocalDateTime getPatrolMaxDateTime() {
		return maxDateTime;
	}

	public void setPatrolMaxDateTime(LocalDateTime maxDateTime) {
		this.maxDateTime = maxDateTime;
	}

	@Override
	public LocalDateTime getPatrolMinDateTime() {
		return minDateTime;
	}

	public void setPatrolMinDateTime(LocalDateTime minDateTime) {
		this.minDateTime = minDateTime;
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(patrolUuid, patrolLegUuid);
		
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
	public SimpleFeature toSimpleFeature(SimpleFeatureType ftype, 
			QueryColumn geometryColumn,
			List<QueryColumn> columns) {
		
		List<Object> data = new ArrayList<Object>();
		data.add(geometryColumn.getValue(this));
		data.add(getPatrolId() + "." + System.nanoTime()); //$NON-NLS-1$
		int i = 2;
		for (QueryColumn c : columns){
			if (c.getKey().equalsIgnoreCase(geometryColumn.getKey())) continue;
			if (c.isVisible()){
				data.add(QueryColumnUtils.getValue(this, c, ftype.getDescriptor(i++), Locale.getDefault()));
			}
		}
		return SimpleFeatureBuilder.build(ftype, data, (String)data.get(1));		
	}
	
	@Override
	public boolean equals(Object other){
		if (other == this) return true;
		if (other == null) return false;
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		PatrolQueryResultItem o = (PatrolQueryResultItem) other;
		
		return Objects.equals(getPatrolLegUuid(), o.getPatrolLegUuid()) &&
				Objects.equals(getPatrolUuid(), o.getPatrolUuid());
	}

	
}
