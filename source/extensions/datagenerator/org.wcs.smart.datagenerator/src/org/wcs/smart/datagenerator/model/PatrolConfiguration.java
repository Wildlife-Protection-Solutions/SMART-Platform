/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.Area;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Configuration information for generating sample patrol data.
 *  
 * @author Emily
 *
 */
public class PatrolConfiguration {

	private int numberOfPatrols = 10;
	private LocalDate startDate = LocalDate.now();
	private LocalDate endDate = startDate;
	private int daysPerPatrolMin = 1;
	private int daysPerPatrolMax = 5;
	private int employeesPerPatrolMin = 3;
	private int employeesPerPatrolMax = 10;
	
	private int waypointsPerDayMin = 5;
	private int waypointsPerDayMax = 20;
	private int observationsPerWaypointMin = 0;
	private int observationsPerWaypointMax = 5;
	
	private Area.AreaType bboxArea = null;
	private Envelope bboxEvelope = new Envelope(0, 1, 0, 1);
	
	private List<ObservationConfiguration> mappings = new ArrayList<>();
	
	public PatrolConfiguration() {
	}
	
	private int cachedWeight = -1;
	public int getTotalWeight() {
		if (cachedWeight < 0) {
			int t = 0;
			for (ObservationConfiguration m : mappings) t+=m.getWeight();
			this.cachedWeight = t;
		}
		return cachedWeight;
	}
	
	public void updateWeight(ObservationConfiguration mapping, int newWeight) {
		mapping.setWeight(newWeight);
		cachedWeight = -1;
	}
	
	public void removeMapping(ObservationConfiguration toRemove) {
		this.mappings.remove(toRemove);
		this.cachedWeight = -1;
	}
	
	public void addMapping(ObservationConfiguration toAdd) {
		this.mappings.add(toAdd);
		this.cachedWeight = -1;
	}
	
	public int getNumberOfPatrols() {
		return numberOfPatrols;
	}

	public void setNumberOfPatrols(int numberOfPatrols) {
		this.numberOfPatrols = numberOfPatrols;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public int getDaysPerPatrolMin() {
		return daysPerPatrolMin;
	}

	public void setDaysPerPatrolMin(int daysPerPatrolMin) {
		this.daysPerPatrolMin = daysPerPatrolMin;
	}

	public int getDaysPerPatrolMax() {
		return daysPerPatrolMax;
	}

	public void setDaysPerPatrolMax(int daysPerPatrolMax) {
		this.daysPerPatrolMax = daysPerPatrolMax;
	}

	public int getEmployeesPerPatrolMin() {
		return employeesPerPatrolMin;
	}

	public void setEmployeesPerPatrolMin(int employeesPerPatrolMin) {
		this.employeesPerPatrolMin = employeesPerPatrolMin;
	}

	public int getEmployeesPerPatrolMax() {
		return employeesPerPatrolMax;
	}

	public void setEmployeesPerPatrolMax(int employeesPerPatrolMax) {
		this.employeesPerPatrolMax = employeesPerPatrolMax;
	}

	public int getWaypointsPerDayMin() {
		return waypointsPerDayMin;
	}

	public void setWaypointsPerDayMin(int waypointsPerDayMin) {
		this.waypointsPerDayMin = waypointsPerDayMin;
	}

	public int getWaypointsPerDayMax() {
		return waypointsPerDayMax;
	}

	public void setWaypointsPerDayMax(int waypointsPerDayMax) {
		this.waypointsPerDayMax = waypointsPerDayMax;
	}

	public int getObservationsPerWaypointMin() {
		return observationsPerWaypointMin;
	}

	public void setObservationsPerWaypointMin(int observationsPerWaypointMin) {
		this.observationsPerWaypointMin = observationsPerWaypointMin;
	}

	public int getObservationsPerWaypointMax() {
		return observationsPerWaypointMax;
	}

	public void setObservationsPerWaypointMax(int observationsPerWaypointMax) {
		this.observationsPerWaypointMax = observationsPerWaypointMax;
	}

	public Envelope getBboxEnvelope() {
		return bboxEvelope;
	}

	public void setBboxEnvelope(Envelope bbox) {
		this.bboxEvelope = bbox;
		this.bboxArea = null;
	}
	
	public Area.AreaType getBboxArea(){
		return this.bboxArea;
	}
	
	public void setBboxArea(Area.AreaType type) {
		this.bboxArea = type;
		this.bboxEvelope = null;
	}

	/**
	 * Do not modify this list directly, instead use addMapping or removeMapping 
	 * functions.
	 * @return
	 */
	public List<ObservationConfiguration> getMappings() {
		return mappings;
	}

	public void setMappings(List<ObservationConfiguration> mappings) {
		this.mappings = mappings;
	}
}
