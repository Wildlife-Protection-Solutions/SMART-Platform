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
package org.wcs.smart.cybertracker.survey.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Links cybertracker patrolid/deviceid to SMART mission.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="ct_mission_link", schema="smart")
public class CtMissionLink {

	private UUID ctUuid;
	private Mission mission;
	private String deviceId;
	private Integer lastObservation;
	private LocalDateTime groupStartTime;
	
	private SurveyDesign surveyDesign;
	
	private SamplingUnit lastSamplingUnit;
	
	private List<CtMissionWpLink> wplinks;

	
	@Id
	@Column(name="ct_uuid")
	public UUID getCtUuid(){
		return this.ctUuid;
	}
	
	public void setCtUuid(UUID ctUuid){
		this.ctUuid = ctUuid;
	}
	
	@OneToMany(cascade= {CascadeType.ALL}, orphanRemoval = true)
	@JoinColumn(name="ct_mission_link_uuid", referencedColumnName="ct_uuid")
	public List<CtMissionWpLink> getWaypointLinks(){
		return this.wplinks;
	}
	public void setWaypointLinks(List<CtMissionWpLink> links) {
		this.wplinks = links;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="mission_uuid")
	public Mission getMission(){
		return this.mission;
	}
	
	public void setMission(Mission mission){
		this.mission = mission;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="su_uuid")
	public SamplingUnit getSamplingUnit(){
		return this.lastSamplingUnit;
	}
	
	public void setSamplingUnit(SamplingUnit su){
		this.lastSamplingUnit = su;
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
	public LocalDateTime getGroupStartTime(){
		return groupStartTime;
	}
	
	public void setGroupStartTime(LocalDateTime startTime){
		this.groupStartTime = startTime;
	}
	
	/**
	 * For linking to missions to associated survey designs
	 * @return
	 */
	@Transient
	public SurveyDesign getNewSurveyDesign(){
		return this.surveyDesign;
	}
	public void setNewSurveyDesign(SurveyDesign sd){
		this.surveyDesign = sd;
		
	}
}
