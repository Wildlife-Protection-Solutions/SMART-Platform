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

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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
	private Date time;
	private float direction;
	private float distance;
	private String comment;
	
	private List<WaypointAttachment> attachments;
	private List<WaypointObservation> observations;
	
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

	@Column(name="datetime")
	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@Column(name="direction")
	public float getDirection() {
		return direction;
	}

	public void setDirection(float direction) {
		this.direction = direction;
	}

	@Column(name="distance")
	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Column(name="wp_comment")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@OneToMany(fetch = FetchType.LAZY)
	public List<WaypointAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<WaypointAttachment> attachments) {
		this.attachments = attachments;
	}

	@OneToMany(fetch = FetchType.LAZY)
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	public void setObservations(List<WaypointObservation> observations){
		this.observations = observations;
	}
}
