/*
 * Copyright (C) 2026 Wildlife Conservation Society
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
package org.wcs.smart.er.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.Survey;

/**
 * Class for managing metadata for merging missions
 */
public class MissionMergeMetadata {
	
	private String missionId;
	private Survey survey;
	private String comment;
	private LocalDate startDate;
	private LocalDate endDate;
	private List<String> missionIdOps;
	private List<Survey> surveyOps;
	private Employee leader;
	private List<Employee> employeeOps;
	private Map<MissionAttribute, List<MissionPropertyValue>> attributeOps;
	private List<MissionPropertyValue> attributeValues;
	
	
	public MissionMergeMetadata() {
	}
	
	public void setMissionIdOps(List<String> ops) {
		this.missionIdOps = ops;
	}
	
	public List<String> getMissionIdOps(){
		return this.missionIdOps;
	}
	
	public void setSurveyOps(List<Survey> ops) {
		this.surveyOps = ops;
	}
	
	public List<Survey> getSurveyOps(){
		return this.surveyOps;
	}

	public void setMissionDates(LocalDate start, LocalDate end) {
		this.startDate = start;
		this.endDate = end;
	}
	public LocalDate getStartDate() {
		return this.startDate;
	}

	public LocalDate getEndDate() {
		return this.endDate;
	}

	public void setEmployeeOps(List<Employee> ops, Employee leader) {
		this.employeeOps = ops;
		this.leader = leader;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	public List<Employee> getEmployeeOps(){
		return this.employeeOps;
	}
	
	public Employee getLeader() {
		return this.leader;
	}
	
	public void setAttributeOps(Map<MissionAttribute, List<MissionPropertyValue>> ops) {
		this.attributeOps = ops;
	}
	public Map<MissionAttribute, List<MissionPropertyValue>> getAttributesOps(){
		return this.attributeOps;
	}

	public void setSelectedValues(Survey survey, String id, Employee leader, List<MissionPropertyValue> values) {
		this.missionId  = id;
		this.survey = survey;
		this.leader = leader;		
		this.attributeValues = values;
	}
	
	public Survey getSurvey() {
		return this.survey;
	}
	public List<MissionPropertyValue> getAttributeValues(){
		return this.attributeValues;
	}
	public String getMissionId() {
		return missionId;
	}
	public String getComment() {
		return this.comment;
	}

}
