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
package org.wcs.smart.ca;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Association between a Category and its attributes.
 * 
 * @author Emily
 * @since 7.0.0
 */
@Entity
@Table(name="employee_team_member", schema="smart")
public class EmployeeTeamMember implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private EmployeeTeamMemeberPk id = new EmployeeTeamMemeberPk();	
	
	/**
	 * Create new association
	 */
	public EmployeeTeamMember(){
	}
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public EmployeeTeamMemeberPk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(EmployeeTeamMemeberPk id){
		this.id = id;
	}
	
	/**
	 * 
	 * @return the associated employee team 
	 */
	@Transient 
	public EmployeeTeam getTeam(){
		return id.getTeam();
	}
	/**
	 * @param team the employee team
	 */
	public void setTeam(EmployeeTeam team){
		id.setTeam(team);
	}
	
	/**
	 * @return the association employee
	 */
	@Transient 
	public Employee getEmployee(){
		return id.getEmployee();
	}
	/**
	 * @param employee the employee
	 */
	public void setEmployee(Employee employee){
		id.setEmployee(employee);
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof EmployeeTeamMember){
			return this.id.equals(((EmployeeTeamMember)o).id);
		}
		return false;
	}

	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Primary key object for employee team association 
	 * 
	 */
	@Embeddable
	private static class EmployeeTeamMemeberPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private Employee employee;
		private EmployeeTeam team;
		

		public EmployeeTeamMemeberPk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="employee_uuid")
		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="team_uuid")
		public EmployeeTeam getTeam() {
			return team;
		}
		public void setTeam(EmployeeTeam team) {
			this.team = team;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof EmployeeTeamMemeberPk)){
				return false;
			}
			EmployeeTeamMemeberPk p = (EmployeeTeamMemeberPk)key;
			
			return Objects.equals(p.employee, this.employee) && Objects.equals(p.team, this.team);
		}
		@Override
		public int hashCode() {
			return Objects.hash(employee, team);    
		}
	}
	
}
