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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Survey model object.  A survey belongs to a 
 * survey design and contains a collection of 
 * missions.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="survey", schema="smart")
public class Survey extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	public static final int ID_MAX_LENGTH = 128;
	
	private String id;
	private SurveyDesign design;
	private List<Mission> missions;

	private LocalDateTime createdDate;

	public Survey() {
		
	}
	
	/**
	 * Survey identified
	 * @return
	 */
	
	@Column(name="id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	/**
	 * associated survey design
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="survey_design_uuid", referencedColumnName="uuid")
	public SurveyDesign getSurveyDesign() {
		return design;
	}
	public void setSurveyDesign(SurveyDesign design) {
		this.design = design;
	}
	
	/**
	 * associated survey design
	 * @return
	 */
	@OrderBy("startDate DESC, id ASC")
	@OneToMany(fetch = FetchType.LAZY, mappedBy="survey", orphanRemoval = true)
	public List<Mission> getMissions() {
		return missions;
	}
	public void setMissions(List<Mission> missions) {
		this.missions = missions;
	}
		
	
	/**
	 * Gets the created datetime in UTC
	 * @return
	 */
	@Column(name="date_created")
	public LocalDateTime getCreatedDate() {
		return createdDate;
	}

	/**
	 * @param the last modified date time in UTC
	 */
	public void setCreatedDate(LocalDateTime createdDate) {
		this.createdDate = createdDate;
	}
	
	@Transient
	public LocalDateTime getCreatedDateAtLocal() {
		LocalDateTime utc = getCreatedDate();
		if (utc == null) return null;
		return utc.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	@Transient
	public void initCreatedDate() {
		setCreatedDate(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
	}
	
}
