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
package org.wcs.smart.plan.model;

import java.io.File;
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

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Station;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SmartUtils;

/**
 * Represents a patrol object
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.plan")
public class Plan {

	private byte[] uuid;
	private ConservationArea ca;
	private Station station;
	private Team team;
	private String id;
	private String name;
	private String description;
	private Date startDate;
	private Date endDate;
	private String type;
	
	private List<PlanTarget> targets;

	private Integer unavailableEmployees;
	private Integer activeEmployees;

	private List<Plan> children = new ArrayList<Plan>();
	private Plan parent = null;
	private Plan template = null;
	
	public Plan(){
		
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name = "id")
	public String getId(){
		return this.id;
	}
	public void setId(String id){
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="station_uuid", referencedColumnName="uuid")
	public Station getStation() {
		return station;
	}

	public void setStation(Station station) {
		this.station = station;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="team_uuid", referencedColumnName="uuid")
	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
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
		if (other != null && other instanceof Plan){
			Plan s = (Plan)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return s.hashCode() == hashCode();
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}
	
	/**
	 * 
	 * <p>
	 * To get full file names you must prepend this with the conservation area file store location.
	 * </p>
	 * <code>
	 * ConservationArea.getFileDataStoreLocation() + File.separator + Patrol.getPatrolDatastorePath();
	 * </code>
	 * @return the file store location for the patrol relative to the conservation area file store
	 */
	@Transient
	public String getPlanDatastorePath(){
		return "Plan" + File.separator + SmartUtils.getDirectoryPath(uuid);
	}
	
	@Column(name = "type")
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@Column(name = "description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public void setUnavailableEmployees(Integer unavailableEmployees){
		this.unavailableEmployees = unavailableEmployees; 
	}
	@Column(name = "unavailable_employees")
	public Integer getUnavailableEmployees(){
		return this.unavailableEmployees; 
	}
	
	public void setActiveEmployees(Integer activeEmployees){
		this.activeEmployees = activeEmployees;
	}
	@Column(name = "active_employees")
	public Integer getActiveEmployees(){
		return activeEmployees;
	}

	@Transient
	public void addTarget(PlanTarget t){
		if(targets == null){
			targets = new ArrayList<PlanTarget>();
		}
		targets.add(t);
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="plan", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<PlanTarget> getTargets() {
		return targets;
	}
	public void setTargets(List<PlanTarget> targets) {
		this.targets = targets;
	}
	
	/**
	 * 
	 * @return the children plans
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@BatchSize(size=200)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<Plan> getChildren(){
		return this.children;
	}
	
	public void setChildren(List<Plan> children){
		this.children = children;
	}
	
	/**
	 * 
	 * @return parent plan or null if not parent
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_uuid")
	public Plan getParent(){
		return this.parent;
	}
	
	public void setParent(Plan parent){
		this.parent = parent;
	}

	@Transient
	public Plan getTemplatePlan() {
		return template;
	}
	public void setTemplatePlan(Plan p) {
		template = p;
	}
}
