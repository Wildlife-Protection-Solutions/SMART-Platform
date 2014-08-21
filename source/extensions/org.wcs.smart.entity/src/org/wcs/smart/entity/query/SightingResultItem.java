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
package org.wcs.smart.entity.query;

import java.util.Date;
import java.util.HashMap;

import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.query.model.IResultItem;

/**
 * Sighting query result item.
 * @author Emily
 *
 */
public class SightingResultItem implements IResultItem{

	private String caId;
	private String caName;
	private String sourceId;
	
	private Date wpDateTime;
	private byte[] waypointUuid;
	private int waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	private String entityId;

	private String waypointObserver;
	
	private String[] observationCategory;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private byte[] observationUuid;
	private byte[] entityUuid;
	private EntityType.Status entityStatus;
	
	private HashMap<String,Object> entityValues = new HashMap<String,Object>();
	

	/**
	 * @param observationUuid the observation uuid
	 */
	public void setObservationUuid(byte[] observationUuid){
		this.observationUuid = observationUuid;
	}
	
	/**
	 * @return the observation uuid
	 */
	public byte[] getObservationUuid(){
		return this.observationUuid;
	}

	public void setEntityAttribute(String attributeKey, Object displayValue){
		entityValues.put(attributeKey, displayValue);
	}
	
	public Object getEntityAttribute(String attributeKey){
		return entityValues.get(attributeKey);
	}
	
	public void setEntityId(String entityId){
		this.entityId = entityId;
	}
	public String getEntityId(){
		return this.entityId;
	}
	
	public void setEntityUuid(byte[] uuid){
		this.entityUuid = uuid;
	}
	
	public byte[] getEntityUuid(){
		return this.entityUuid;
	}
	
	public EntityType.Status getEntityStatus(){
		return entityStatus;
	}
	
	public void setEntityStatus(EntityType.Status status){
		this.entityStatus = status;
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
	public void setWaypointUuid(byte[] uuid){
		this.waypointUuid = uuid;
	}
	/**
	 * 
	 * @return the waypoint uuid
	 */
	public byte[] getWaypointUuid(){
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
	public Date getWaypointDateTime() {
		return wpDateTime;
	}
	/**
	 * @param wpDateTime waypoint date 
	 */
	public void setWaypointDateTime(Date wpDateTime) {
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
	public void setWaypointDistance(float waypointDistance) {
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
	public void setWaypointDirection(float waypointDirection) {
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
	 * the waypoint observer
	 * @return
	 */
	public String getWaypointObserver(){
		return this.waypointObserver;
	}
	
	public void setWaypointObserver(String observer){
		this.waypointObserver = observer;
	}
	
}
