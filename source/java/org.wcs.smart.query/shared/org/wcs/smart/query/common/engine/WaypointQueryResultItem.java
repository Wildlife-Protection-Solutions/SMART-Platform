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
package org.wcs.smart.query.common.engine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.udig.WaypointSimpleFeature;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumnUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * A class to hold the results of a waypoint 
 * query.   
 * 
 * @author Emily
 * @since 1.0.0
 */
public class WaypointQueryResultItem implements IGeometryResultItem, IWaypointQueryResultItem, IAdaptable{
//	/**
//	 * Waypoint geometry field name
//	 */
//	public static final String GEOMCOLUMN_KEY = "wp:geometry"; //$NON-NLS-1$
		
	private String caId;
	private String caName;
	private UUID caUuid;
	private String sourceId;
	
	private LocalDateTime wpDateTime;
	private UUID waypointUuid;
	private String waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	private String waypointObserver;
	
	private String lastModifiedBy;
	private LocalDateTime lastModified;
	
	
	/**
	 * the waypoint last modified date
	 * @param lastModified
	 */
	public void setLastModifiedDate(LocalDateTime lastModified) {
		this.lastModified = lastModified;
	}
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
	 * @return waypoint soure id
	 */
	public String getSourceId() {
		return sourceId;
	}
	/**
	 * @param sourceId waypoint soure
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
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
	public String getWaypointId() {
		return waypointId;
	}
	/**
	 * @param waypointId waypoint id
	 */
	public void setWaypointId(String waypointId) {
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
	 * Sets the RAW waypoint location value
	 * @param waypointX waypoint y (longitude)
	 */
	public void setWaypointX(double waypointX) {
		this.waypointX = waypointX;
	}
	
	
	/**
	 * Sets the RAW waypoint location value 
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
	public Geometry asGeometry() {		
		return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(getWaypointX(null), getWaypointY(null)));		
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(Waypoint.class) && getWaypointUuid() != null){
			Waypoint wp = new Waypoint();
			wp.setUuid(getWaypointUuid());
			return (T)wp;
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
	public SimpleFeature toSimpleFeature(SimpleFeatureType ftype, 
			QueryColumn geometryColumn,
			List<QueryColumn> columns) {
		
		List<Object> data = new ArrayList<Object>();
		data.add(geometryColumn.getValue(this));
		data.add(getWaypointId() + "." + System.nanoTime()); //$NON-NLS-1$ 
		int i = 2;
		for (QueryColumn c : columns){
			if (c.getKey().equalsIgnoreCase(geometryColumn.getKey())) continue;
			if (c.isVisible()){
				data.add(QueryColumnUtils.getValue(this, c, ftype.getDescriptor(i++), Locale.getDefault()));
			}
		}
		return new WaypointSimpleFeature(SimpleFeatureBuilder.build(ftype, data, (String)data.get(1)), getWaypointUuid());
		
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (getClass() != other.getClass()) return false;
		return Objects.equals(getWaypointUuid(), ((WaypointQueryResultItem)other).getWaypointUuid());
	}
	
	@Override
	public int hashCode() {
		return getWaypointUuid().hashCode();
	}
}
