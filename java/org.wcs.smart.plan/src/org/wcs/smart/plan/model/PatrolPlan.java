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

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.patrol.model.Patrol;

/**
 * Link between a patrol and a plan.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.patrol_plan")
@AssociationOverrides({
	@AssociationOverride(name = "id.patrol", 
		joinColumns = @JoinColumn(name = "patrol_uuid")),
	@AssociationOverride(name = "id.plan", 
		joinColumns = @JoinColumn(name = "plan_uuid")) })
public class PatrolPlan {
	
	private PatrolPlanPk id = new PatrolPlanPk();
	
	public PatrolPlan(){
	}
	
	
	@EmbeddedId
	public PatrolPlanPk getId(){
		return this.id;
	}
	
	public void setId(PatrolPlanPk id){
		this.id = id;
	}
	
	@Transient
	public Plan getPlan(){
		return this.id.getPlan();
	}
	public void setPlan(Plan plan){
		this.id.setPlan(plan);
	}
	
	@Transient
	public Patrol getPatrol(){
		return this.id.getPatrol();
	}
	public void setPatrol(Patrol patrol){
		this.id.setPatrol(patrol);
	}
	
	@Embeddable
	protected static class PatrolPlanPk implements Serializable{
		
		private static final long serialVersionUID = 1L;
		
		
		private Patrol patrol;
		private Plan plan;
		
		protected PatrolPlanPk(){
			
		}
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
		public Patrol getPatrol(){
			return this.patrol;
		}
		public void setPatrol(Patrol patrol){
			this.patrol = patrol;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="plan_uuid", referencedColumnName="uuid")
		public Plan getPlan(){
			return this.plan;
		}
		public void setPlan(Plan plan){
			this.plan = plan;
		}
	}

}
