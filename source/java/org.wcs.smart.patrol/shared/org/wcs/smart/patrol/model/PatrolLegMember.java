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

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;

/**
 * Patrol leg member
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol_leg_members")
@AssociationOverrides({
	@AssociationOverride(name = "id.member", 
		joinColumns = @JoinColumn(name = "employee_uuid")),
	@AssociationOverride(name = "id.patrolLeg", 
		joinColumns = @JoinColumn(name = "patrol_leg_uuid")) })
public class PatrolLegMember {

	private PatrolLegMemberPk id = new PatrolLegMemberPk();
	private boolean isLeader;
	private boolean isPilot;
	
	public PatrolLegMember(){
	}
	
	@EmbeddedId
	public PatrolLegMemberPk getId(){
		return this.id;
	}
	public void setId(PatrolLegMemberPk id){
		this.id = id;
	}
	
	@Column(name="is_leader")
	public boolean getIsLeader(){
		return this.isLeader;
	}
	public void setIsLeader(boolean isLeader){
		this.isLeader = isLeader;
	}
	
	@Column(name="is_pilot")
	public boolean getIsPilot(){
		return this.isPilot;
	}
	public void setIsPilot(boolean isPilot){
		this.isPilot = isPilot;
	}
	
	
	@Transient
	public Employee getMember(){
		return id.getMember();
	}
	public void setMember(Employee member){
		id.setMember(member);
	}
	
	@Transient
	public PatrolLeg getLeg(){
		return id.getPatrolLeg();
	}
	public void setPatrolLeg(PatrolLeg leg){
		id.setPatrolLeg(leg);
	}
	
	/**
	 * Clones the patrol leg member setting the member, pilot, and leader
	 * attributes.  DOES not set or clone the patrol leg attribute.
	 */
	public PatrolLegMember clone (){
		PatrolLegMember clone = new PatrolLegMember();
		clone.setMember(this.getMember());
		clone.setIsLeader(getIsLeader());
		clone.setIsPilot(getIsPilot());
		return clone;
	}
	
	@Embeddable
	private static class PatrolLegMemberPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Employee member;
		private PatrolLeg leg;
		
		public PatrolLegMemberPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="employee_uuid", referencedColumnName="uuid")
		public Employee getMember(){
			return this.member;
		}
		public void setMember(Employee member){
			this.member = member;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="patrol_leg_uuid", referencedColumnName="uuid")
		public PatrolLeg getPatrolLeg(){
			return this.leg;
		}
		public void setPatrolLeg(PatrolLeg leg){
			this.leg  = leg;
		}
	}
}
