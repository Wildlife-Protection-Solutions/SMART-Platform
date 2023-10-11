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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Patrol Leg object
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="patrol_leg", schema="smart")
public class PatrolLeg extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	public static final int ID_MAX_SIZE = 50;
	private Patrol patrol;
	private LocalDate startDate;
	private LocalDate endDate;
	private PatrolTransportType type;
	private PatrolMandate mandate;
	private String id;

	private List<PatrolLegMember> members;
	private List<PatrolLegDay> days;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
	public Patrol getPatrol() {
		return patrol;
	}
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
	}
	
	@Column(name="start_date")
	public LocalDate getStartDate() {
		return startDate;
	}
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}
	@Column(name="end_date")
	public LocalDate getEndDate() {
		return endDate;
	}
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="transport_uuid", referencedColumnName="uuid")
	public PatrolTransportType getType() {
		return type;
	}
	public void setType(PatrolTransportType type) {
		this.type = type;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="mandate_uuid", referencedColumnName="uuid")
	public PatrolMandate getMandate() {
		return mandate;
	}

	public void setMandate(PatrolMandate mandate) {
		this.mandate = mandate;
	}
	
	@Column(name="id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.patrolLeg", orphanRemoval=true, cascade={CascadeType.ALL})
	@BatchSize(size=50)
	public List<PatrolLegMember> getMembers(){
		return this.members;
	}
	public void setMembers(List<PatrolLegMember> members){
		this.members = members;
	}
	
	@OneToMany(fetch= FetchType.LAZY, mappedBy="patrolLeg", orphanRemoval=true, cascade={CascadeType.ALL})
	@OrderBy(clause = "patrol_day")
	@BatchSize(size=50)
	public List<PatrolLegDay> getPatrolLegDays(){
		return this.days;
	}
	public void setPatrolLegDays(List<PatrolLegDay> days){
		this.days = days;
	}

	@Transient
	public void clearPatrolLegMembers(){
		if (this.getMembers() != null){
			for (PatrolLegMember m : getMembers()){
				m.setId(null);
			}
			getMembers().clear();
		}
	}
	
	@Transient
	public PatrolLegMember addPatrolLegMember(Employee employee){
		PatrolLegMember plm = new PatrolLegMember();
		plm.setMember(employee);
		plm.setPatrolLeg(this);
		plm.setIsLeader(false);
		plm.setIsPilot(false);
		
		if (this.members == null){
			this.members = new ArrayList<PatrolLegMember>();
		}
		this.members.add(plm);
		return plm;
		
	}
	@Transient
	public PatrolLegMember getLeader(){
		if (this.members == null){
			return null;
		}
		for (PatrolLegMember member : this.members){
			if (member.getIsLeader()){
				return member;
			}
		}
		return null;
	}
	
	@Transient
	public PatrolLegMember getPilot(){
		if (this.members == null){
			return null;
		}
		for (PatrolLegMember member : this.members){
			if (member.getIsPilot()){
				return member;
			}
		}
		return null;
	}
	
	@Transient
	public void setLeader(PatrolLegMember plm){
		if (this.members == null){
			return;
		}
		for (PatrolLegMember member : this.members){
			member.setIsLeader(false);
		}
		plm.setIsLeader(true);
	}
	
	@Transient
	public void setPilot(PatrolLegMember plm){
		if (this.members == null){
			return;
		}		
		for (PatrolLegMember member : this.members){
			member.setIsPilot(false);
		}
		if (plm != null) plm.setIsPilot(true);
	}
	
	
	/* Only clones id, start/end time, members and type. NOT Patrol Waypoints or waypoints themselves.
	 * 
	 */
	public PatrolLeg simpleClone(){
		PatrolLeg clone = new PatrolLeg();
		clone.setPatrol(patrol);
		clone.setId(id);
		
		//start time
		clone.setStartDate(startDate);
		clone.setEndDate(endDate);
		//type
		clone.setType(type);
		clone.setMandate(mandate);
		//members
		clone.setMembers(new ArrayList<PatrolLegMember>());
		for (PatrolLegMember mem : members){
			PatrolLegMember memClone = mem.clone();
			memClone.setPatrolLeg(clone);
			clone.getMembers().add(memClone);
		}
		return clone;
	}
	
	
	/**
	 * Creates leg days for the given leg.
	 * <p>Will remove any existing leg days
	 * and corresponding data</p>
	 * 
	 */
	@Transient
	public void createLegDays(Session session){
		if (this.days == null){
			this.days = new ArrayList<PatrolLegDay>();
		}
		
		LocalTime startTime = !days.isEmpty() ? days.get(0).getStartTime() : LocalTime.MIN;
		LocalTime endTime = !days.isEmpty() ? days.get(days.size() - 1).getEndTime() : LocalTime.MAX;
		
		//lets make a hash set of existing leg days; 
		//we try to re-use these so associated data is not lost
		HashMap<LocalDate, PatrolLegDay> current = new HashMap<LocalDate, PatrolLegDay>();
		for (PatrolLegDay day : this.days){
			current.put(day.getDate(), day);
		}

		// -- the remaining days
		LocalDate working = getStartDate();
		while (working.isBefore(getEndDate()) || working.isEqual(getEndDate()) ){
			
			PatrolLegDay existing = current.remove(working);
			if (existing != null){
				if (existing.getStartTime() == null) existing.setStartTime(LocalTime.MIN);
				if (existing.getEndTime() == null) existing.setEndTime(LocalTime.MAX);
			}else{
				PatrolLegDay previousDay = new PatrolLegDay();
				previousDay.setDate( working );
				previousDay.setStartTime( LocalTime.MIN );
				previousDay.setEndTime( LocalTime.MAX );
				previousDay.setPatrolLeg(this);
				this.days.add(previousDay);
				
			}
			working = ChronoUnit.DAYS.addTo(working, 1);
		}
	
		//remove old legs that weren't used
		for (PatrolLegDay day : current.values()){
			//we need to make sure we delete all waypoints here
			if (day.getWaypoints() != null){
				for (PatrolWaypoint pw : day.getWaypoints()){
					session.remove(pw.getWaypoint());
				}
				//session.flush();
			}
			this.days.remove(day);
		}
		
		//sort 
		Collections.sort(this.days, new Comparator<PatrolLegDay>() {
			@Override
			public int compare(PatrolLegDay o1, PatrolLegDay o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});

		//if there is only one leg day we are done.
		//otherwise if check the first and last patrol days; if they are < 1 second in length than remove the leg day
		if (this.days.size() > 1){
			PatrolLegDay firstDay = this.days.get(0);
			if (firstDay.getLengthSeconds() <= 1){
				//remove this day
				firstDay.setPatrolLeg(null);
				this.days.remove(firstDay);
				this.days.get(0).setStartTime(LocalTime.MIN);
				setStartDate( this.days.get(0).getDate() );
			}
		}
		if (this.days.size() > 1){
			PatrolLegDay lastDay = this.days.get(this.days.size() - 1);
			if (lastDay.getLengthSeconds() <= 1){
				//remove this day
				lastDay.setPatrolLeg(null);
				this.days.remove(lastDay);
				this.days.get(this.days.size() - 1).setEndTime(LocalTime.MAX);
				setEndDate( this.days.get(this.days.size() - 1).getDate()  );
				
			}
		}
		
		days.get(0).setStartTime(startTime);
		days.get(days.size() - 1).setEndTime(endTime);

	
	}
	
}
