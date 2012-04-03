package org.wcs.smart.query.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

public class QueryResultItem {

	private String patrolId;
	private Date patrolStartDate;
	private Date patrolEndDate;
	private String station;
	private String team;
	private String objective;
	private int objectiveRating;
	private String mandate;
	private PatrolType.Type patrolType;
	private byte[] patrolUuid;
	private boolean armed;
	private String patrolLegId;
	private String transportType;
	private Date wpDateTime;
	private Date waypointTime;
	
	private int waypointId;
	private double waypointX;
	private double waypointY;
	private Float waypointDistance;
	private Float waypointDirection;
	private String waypointComment;
	
	private String[] observationCategory;
	private String[] attributeKeys = new String[]{};
	private Object[] attributeValues = new Object[]{};
	
	private byte[] observationUuid;
	
	
	public String getPatrolId() {
		return patrolId;
	}
	
	public void setObservationUuid(byte[] observationUuid){
		this.observationUuid = observationUuid;
	}
	public byte[] getObservationUuid(){
		return this.observationUuid;
	}
	public String[] getCategories(){
		return this.observationCategory;
	}
	
	public void setCategory(Category cat){
		ArrayList<String> values = new ArrayList<String>();
		values.add(cat.getName());
		Category parent = cat.getParent();
		while(parent != null){
			values.add(parent.getName());
			parent = parent.getParent();
		}
		Collections.reverse(values);
		
		observationCategory = values.toArray(new String[values.size()]);
	}
	
	public String getAttributeValue(String attributeKey){
		for (int i = 0; i < attributeKeys.length; i ++){
			if (attributeKeys[i].equals(attributeKey)){
				return attributeValues[i].toString();
			}
		}
		return "";
	}
	
	public void addAttribute(String key, Object value){
		attributeKeys = Arrays.copyOf(attributeKeys, attributeKeys.length + 1);
		attributeValues = Arrays.copyOf(attributeValues, attributeValues.length + 1);
	
		attributeKeys[attributeKeys.length - 1] = key;
		attributeValues[attributeValues.length - 1] = value;
		
		/*
		attributeKeys[attributeKeys.length - 1] = attribute.getAttribute().getKeyId();
		switch(attribute.getAttribute().getType()){
		case NUMERIC:
			attributeValues[attributeValues.length -1] = attribute.getNumberValue();
			break;
		case BOOLEAN:
			attributeValues[attributeValues.length -1] = attribute.getNumberValue() > 0.5;
			break;
		case TEXT:
			attributeValues[attributeValues.length -1] = attribute.getStringValue();
			break;
		case TREE:
			attributeValues[attributeValues.length -1] = attribute.getAttributeTreeNode().getName();
			break;
		case LIST:
			attributeValues[attributeValues.length -1] = attribute.getAttributeListItem().getName();
			break;
		}
		*/
		
	}
	
	public void setPatrolValues(Patrol p){
		this.patrolId = p.getId();
		this.patrolEndDate = p.getEndDate();
		this.patrolStartDate = p.getStartDate();
		this.patrolType = p.getPatrolType();
		this.armed = p.isArmed();
		
		if (p.getStation() != null){
			this.station = p.getStation().getName();
		}else{
			this.station = "";
		}
		if (p.getTeam() != null){
			this.team = p.getTeam().getName();
		}else{
			this.team = "";
		}
		if (p.getMandate() != null){
			this.mandate = p.getMandate().getName();
		}else{
			this.mandate = "";
		}
		this.objective = p.getObjective();
		this.objectiveRating = p.getObjectiveRating();
	}
	
	
	public void setPatrolLegValues(PatrolLeg pl){
		this.patrolLegId = pl.getId();
		this.transportType =pl.getType().getName();
	}
	public void setPatrolLegDayValues(PatrolLegDay pld){
		this.wpDateTime = pld.getDate();
	}
	public void setWaypointValues(Waypoint wp){
		this.waypointDirection = wp.getDirection();
		this.waypointDistance = wp.getDistance();
		this.waypointId = wp.getId();
		this.waypointX = wp.getX();
		this.waypointY = wp.getY();
		this.waypointComment = wp.getComment();
		this.waypointTime = wp.getTime();
		
	}
	
	public void setPatrolId(String patrolId) {
		this.patrolId = patrolId;
	}
	public Date getPatrolStartDate() {
		return patrolStartDate;
	}
	public void setPatrolStartDate(Date patrolStartDate) {
		this.patrolStartDate = patrolStartDate;
	}
	public Date getPatrolEndDate() {
		return patrolEndDate;
	}
	public void setPatrolEndDate(Date patrolEndDate) {
		this.patrolEndDate = patrolEndDate;
	}
	public String getStation() {
		return station;
	}
	public void setStation(String station) {
		this.station = station;
	}
	public String getTeam() {
		return team;
	}
	public void setTeam(String team) {
		this.team = team;
	}
	public String getObjective() {
		return objective;
	}
	public void setObjective(String objective) {
		this.objective = objective;
	}
	public int getObjectiveRating() {
		return objectiveRating;
	}
	public void setObjectiveRating(int objectiveRating) {
		this.objectiveRating = objectiveRating;
	}
	public String getMandate() {
		return mandate;
	}
	public void setMandate(String mandate) {
		this.mandate = mandate;
	}
	public PatrolType.Type getPatrolType() {
		return patrolType;
	}
	public void setPatrolType(PatrolType.Type patrolType) {
		this.patrolType = patrolType;
	}
	public byte[] getPatrolUuid() {
		return patrolUuid;
	}
	public void setPatrolUuid(byte[] patrolUuid) {
		this.patrolUuid = patrolUuid;
	}
	public boolean isArmed() {
		return armed;
	}
	public void setArmed(boolean armed) {
		this.armed = armed;
	}
	public String getPatrolLegId() {
		return patrolLegId;
	}
	public void setPatrolLegId(String patrolLegId) {
		this.patrolLegId = patrolLegId;
	}
	public String getTransportType() {
		return transportType;
	}
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}
	public Date getWpDateTime() {
		return wpDateTime;
	}
	public void setWpDateTime(Date wpDateTime) {
		this.wpDateTime = wpDateTime;
	}
	public Date getWaypointTime() {
		return waypointTime;
	}
	public void setWaypointTime(Date wpTime) {
		this.waypointTime = wpTime;
	}
	public int getWaypointId() {
		return waypointId;
	}
	public void setWaypointId(int waypointId) {
		this.waypointId = waypointId;
	}
	public double getWaypointX() {
		return waypointX;
	}
	public void setWaypointX(double waypointX) {
		this.waypointX = waypointX;
	}
	public double getWaypointY() {
		return waypointY;
	}
	public void setWaypointY(double waypointY) {
		this.waypointY = waypointY;
	}
	public Float getWaypointDistance() {
		return waypointDistance;
	}
	public void setWaypointDistance(float waypointDistance) {
		this.waypointDistance = waypointDistance;
	}
	public Float getWaypointDirection() {
		return waypointDirection;
	}
	public void setWaypointDirection(float waypointDirection) {
		this.waypointDirection = waypointDirection;
	}
	public String getWaypointComment() {
		return waypointComment;
	}
	public void setWaypointComment(String wpComment) {
		this.waypointComment = wpComment;
	}
	public QueryResultItem(){
		
	}
	
	
	
}
