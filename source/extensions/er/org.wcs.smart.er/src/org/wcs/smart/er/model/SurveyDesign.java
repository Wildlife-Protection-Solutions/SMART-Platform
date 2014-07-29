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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Core survey object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.survey_design")
public class SurveyDesign extends NamedKeyItem{
	
	/**
	 * Survey states.
	 */
	public enum State {
		ACTIVE ("Active"), 
		INACTIVE ("Inactive");

		private String guiName;
	
		private State(String guiName){
			this.guiName = guiName;
		}
		
		public String getGuiName(){
			return this.guiName;
		}
	
	}
	public static final String SURVEY_FILESTORE_LOC = "survey"; //$NON-NLS-1$
	
	private ConservationArea ca;
	
	private Date startDate;
	
	private Date endDate;
	
	private boolean trackDistanceDirection;
	
	private String description;
	
	private ConfigurableModel cm;
	
	private State state;
	
//	private List<Survey> surveys;
	
	public SurveyDesign(){
		
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	@Column(name="start_date")
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Column(name="end_date")
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Column(name="distance_direction")
	public boolean getTrackDistanceDirection() {
		return trackDistanceDirection;
	}

	public void setTrackDistanceDirection(boolean trackDistanceDirection) {
		this.trackDistanceDirection = trackDistanceDirection;
	}

	@Column(name="description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="configurable_model_uuid", referencedColumnName="uuid")
	public ConfigurableModel getConfigurableModel() {
		return cm;
	}

	public void setConfigurableModel(ConfigurableModel cm) {
		this.cm = cm;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="state")
	public State getState(){
		return this.state;
	}
	
	public void setState(State state){
		this.state = state;
	}
//	@OneToMany(fetch = FetchType.LAZY, mappedBy="survey", cascade={CascadeType.ALL}, orphanRemoval = true)
//	@OrderBy(clause="start_date")
//	public List<Survey> getSurveys(){
//		return this.surveys;
//	}
//	public void setSurveys(List<Survey> surveys){
//		this.surveys = surveys;
//		
//	}
}
