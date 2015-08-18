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
package org.wcs.smart.er.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Mission model object.
 * 
 * @author Emily
 *
 */

@Entity
@Table(name="smart.mission")
public class Mission extends UuidItem{

	public static final int MAX_LENGTH_ID = 128; 
	public static final int MAX_LENGTH_COMMENT = 32700;
	
	private String id;
	private Survey survey;
	private Date start;
	private Date end;
	private String comment;

	private List<MissionDay> days;
	private List<MissionMember> members;
	private List<MissionPropertyValue> properties;
	
	
	public Mission(){
		
	}

	@Column(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="survey_uuid", referencedColumnName="uuid")
	public Survey getSurvey() {
		return survey;
	}

	public void setSurvey(Survey survey) {
		this.survey = survey;
	}

	@Column(name="start_datetime")
	public Date getStartDate() {
		return start;
	}

	public void setStartDate(Date start) {
		this.start = start;
	}

	@Column(name="end_datetime")
	public Date getEndDate() {
		return end;
	}

	public void setEndDate(Date end) {
		this.end = end;
	}

	@Column(name="comment")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="mission", cascade = {CascadeType.ALL})
	@OrderBy("mission_day asc")
	public List<MissionDay> getMissionDays() {
		return this.days;
	}
	
	public void setMissionDays(List<MissionDay> days){
		this.days = days;
	}
		
	/**
	 * The mission property values associated with the
	 * mission.
	 * 
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.mission", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<MissionPropertyValue> getMissionPropertyValues(){
		if (this.properties == null) {
			this.properties = new ArrayList<MissionPropertyValue>();
		}
		return this.properties;
	}
	
	public void setMissionPropertyValues(List<MissionPropertyValue> properties){
		this.properties = properties;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.mission", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<MissionMember> getMembers(){
		return this.members;
	}
	
	public void setMembers(List<MissionMember> members){
		this.members = members;
	}

	@Transient
	public MissionMember getLeader() {
		if (this.members == null) {
			return null;
		}
		for (MissionMember m : this.members) {
			if (m.getIsLeader()) {
				return m;
			}
		}
		return null;
	}

	@Transient
	public void setLeader(MissionMember member) {
		if (this.members == null) {
			return;
		}
		for (MissionMember m : this.members) {
			m.setIsLeader(false);
		}
		member.setIsLeader(true);
	}
	
	/**
	 * 
	 * @return the location of all attachments associated with the mission or
	 * mission waypoints
	 */
	@Transient
	public File getFilestoreLocation(ConservationArea ca){
		return new File(
				ca 
				+ File.separator 
				+ SurveyDesign.SURVEY_FILESTORE_LOC
				+ File.separator
				+ UuidUtils.getDirectoryPath(getUuid()));
	}
}
