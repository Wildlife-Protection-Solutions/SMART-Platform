/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker.patrol.model;


import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.patrol.model.PatrolLeg;

/**
 * Links cybertracker patrolid/deviceid to SMART patrol.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.ct_patrol_link")
public class CtPatrolLink {

	private UUID ctUuid;
	
	private PatrolLeg patrolLeg;
	
	private String deviceId;
	
	private Integer lastObservation;
	
	private Date groupStartTime;
	
	@Id
	@Column(name="ct_uuid")
	public UUID getCtUuid(){
		return this.ctUuid;
	}
	
	public void setCtUuid(UUID ctUuid){
		this.ctUuid = ctUuid;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="patrol_leg_uuid")
	public PatrolLeg getPatrolLeg(){
		return this.patrolLeg;
	}
	
	public void setPatrolLeg(PatrolLeg patrolLeg){
		this.patrolLeg = patrolLeg;
	}
	
	@Column(name="ct_device_id")
	public String getDeviceId(){
		return this.deviceId;
	}
	
	public void setDeviceId(String deviceId){
		this.deviceId = deviceId;
	}
	
	@Column(name="last_observation_cnt")
	public Integer getLastObservationCnt(){
		return this.lastObservation;
	}
	
	public void setLastObservationCnt(Integer lastObservation){
		this.lastObservation = lastObservation;
	}
	
	@Column(name="group_start_time")
	public Date getGroupStartTime(){
		return groupStartTime;
	}
	
	public void setGroupStartTime(Date startTime){
		this.groupStartTime = startTime;
	}
}
