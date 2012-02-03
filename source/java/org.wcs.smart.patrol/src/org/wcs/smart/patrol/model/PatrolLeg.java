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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;

/**
 * Patrol Leg object
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol_leg")
public class PatrolLeg {
	
	private byte[] uuid;
	private Patrol patrol;
	private Date startDate;
	private Date endDate;
	private PatrolTransportType type;
	private int id;

	private List<PatrolLegMember> members;
	private List<PatrolLegDay> days;
	
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
	@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
	public Patrol getPatrol() {
		return patrol;
	}
	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
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
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="transport_uuid", referencedColumnName="uuid")
	public PatrolTransportType getType() {
		return type;
	}
	public void setType(PatrolTransportType type) {
		this.type = type;
	}
	
	@Column(name="id")
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	@OneToMany(fetch = FetchType.LAZY,mappedBy="id.patrolLeg")
	public List<PatrolLegMember> getMembers(){
		return this.members;
	}
	public void setMembers(List<PatrolLegMember> members){
		this.members = members;
	}
	
	@OneToMany(fetch= FetchType.LAZY)
	@JoinColumn(name="patrol_leg_uuid", referencedColumnName="uuid")
	public List<PatrolLegDay> getPatrolLegDays(){
		return this.days;
	}
	public void setPatrolLegDays(List<PatrolLegDay> days){
		this.days = days;
	}

	@Transient
	public void clearPatrolLegMembers(){
		if (this.members != null){
			this.members.clear();
		}
	}
	
	@Transient
	public void addPatrolLegMember(Employee employee){
		PatrolLegMember plm = new PatrolLegMember();
		plm.setMember(employee);
		plm.setPatrolLeg(this);
		plm.setIsLeader(false);
		plm.setIsPilot(false);
		
		if (this.members == null){
			this.members = new ArrayList<PatrolLegMember>();
		}
		this.members.add(plm);
		
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
		plm.setIsPilot(true);
	}
	
	/**
	 * Creates leg days for the given leg.
	 * <p>Will remove any existing leg days
	 * and corresponding data</p>
	 * 
	 */
	@Transient
	public void createLegDays(){
		if (this.days == null){
			this.days = new ArrayList<PatrolLegDay>();
		}
		this.days.clear();
		
		
		GregorianCalendar calStart = SmartPlugIn.convertDate(getStartDate());
		GregorianCalendar calEnd= SmartPlugIn.convertDate(getEndDate());
		
		PatrolLegDay pld = new PatrolLegDay();
		pld.setPatrolLeg(this);
		pld.setDate( calStart.getTime() );
		
		this.days.add(pld);
		calStart.add(Calendar.DAY_OF_MONTH, 1);
		
		calStart = new GregorianCalendar(calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH));
		while (calStart.before(calEnd)){
			pld = new PatrolLegDay();
			pld.setDate( new GregorianCalendar(calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)).getTime() );
			pld.setPatrolLeg(this);
			this.days.add(pld);
			calStart.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		//set the last patrol leg day to the end date
		pld.setDate(calEnd.getTime());
		
	}
}
