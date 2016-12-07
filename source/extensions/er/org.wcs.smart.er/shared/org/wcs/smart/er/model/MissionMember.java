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

import java.io.Serializable;
import java.util.Objects;

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
 * Mission member object.
 * @author Emily
 *
 */
@Entity
@Table(name="smart.mission_member")
@AssociationOverrides({
	@AssociationOverride(name = "id.member", 
		joinColumns = @JoinColumn(name = "employee_uuid")),
	@AssociationOverride(name = "id.mission", 
		joinColumns = @JoinColumn(name = "mission_uuid")) })
public class MissionMember {

	private MissionMemberPk id = new MissionMemberPk();
	private boolean isLeader;
	
	public MissionMember(){
	}
	
	@EmbeddedId
	public MissionMemberPk getId(){
		return this.id;
	}
	public void setId(MissionMemberPk id){
		this.id = id;
	}
	
	@Transient
	public Employee getMember(){
		return id.getMember();
	}
	public void setMember(Employee member){
		id.setMember(member);
	}
	
	@Transient
	public Mission getMission(){
		return id.getMission();
	}
	public void setMission(Mission mission){
		id.setMission(mission);
	}
	
	@Column(name="is_leader")
	public boolean getIsLeader() {
		return isLeader;
	}
	public void setIsLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}


	@Embeddable
	private static class MissionMemberPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Employee member;
		private Mission mission;
		
		public MissionMemberPk(){
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
		@JoinColumn(name="mission_uuid", referencedColumnName="uuid")
		public Mission getMission(){
			return this.mission;
		}
		public void setMission(Mission mission){
			this.mission  = mission;
		}
		
		@Override
		public boolean equals(Object other){
			if (other == this) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			MissionMemberPk o = (MissionMemberPk) other;
			return Objects.equals(mission, o.mission) && Objects.equals(member, o.member);
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(mission, member);
		}
	}
}