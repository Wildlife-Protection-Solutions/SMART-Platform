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
package org.wcs.smart.patrol.model;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;

/**
 * Waypoint object
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.waypoint")
public class Waypoint {
	
	
	private byte[] uuid;
	private PatrolLegDay patrolLegDay;
	private int id;
	private double x;
	private double y;
	private Time time;
	private Float direction;
	private Float distance;
	private String comment;
	
	private List<WaypointAttachment> attachments;
	private List<WaypointObservation> observations;
	
	/*
	 * transient temporary field to hold
	 * imported date if waypoint imported from
	 * other sources
	 */
	private Date importedDate;
	
	public Waypoint(){
		
	}
	
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid() {
		return uuid;
	}
	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}
	
	@ManyToOne
	@JoinColumn(name="leg_day_uuid")
	public PatrolLegDay getPatrolLegDay() {
		return patrolLegDay;
	}

	public void setPatrolLegDay(PatrolLegDay patrolLegDay) {
		this.patrolLegDay = patrolLegDay;
	}

	@Column(name="id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="x")
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	@Column(name="y")
	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	@Column(name="time")
	public Time getTime() {
		return time;
	}

	public void setTime(Time time) {
		this.time = time;
	}

	@Column(name="direction")
	public Float getDirection() {
		return direction;
	}

	public void setDirection(Float direction) {
		this.direction = direction;
	}

	@Column(name="distance")
	public Float getDistance() {
		return distance;
	}

	public void setDistance(Float distance) {
		this.distance = distance;
	}

	@Column(name="wp_comment")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="waypoint", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<WaypointAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<WaypointAttachment> attachments) {
		this.attachments = attachments;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="waypoint", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	public void setObservations(List<WaypointObservation> observations){
		this.observations = observations;
	}
	
	@Transient
	public Date getImportedDate(){
		return this.importedDate;
	}
	public void setImportedDate(Date importedDate){
		this.importedDate = importedDate;
	}
	
	
	public Waypoint clone(){
		
		Waypoint wp = new Waypoint();
		
		
		if (this.attachments != null){
			//TODO: this may be a problem
			wp.setAttachments(new ArrayList<WaypointAttachment>());
		}
		
		wp.setComment(this.comment);
		wp.setDirection(this.direction);
		wp.setDistance(this.distance);
		wp.setId(this.id);
		if (this.importedDate != null){
			wp.setImportedDate((Date)this.importedDate.clone());
		}
		
		wp.setPatrolLegDay(this.patrolLegDay);
		wp.setTime( (Time)this.time.clone());
		wp.setX(this.x);
		wp.setY(this.y);
		
		if (this.observations != null && this.observations.size() > 0){
			wp.setObservations(new ArrayList<WaypointObservation>());
			for (WaypointObservation wobp : this.observations){
				
				WaypointObservation cloned = wobp.clone();
				cloned.setUuid(null);
				cloned.setWaypoint(wp);
				wp.getObservations().add(cloned);
			}
		}
		
		return wp;
	}
	
	@Override
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof Waypoint){
			Waypoint s = (Waypoint)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return s.hashCode() == hashCode();
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}
}
