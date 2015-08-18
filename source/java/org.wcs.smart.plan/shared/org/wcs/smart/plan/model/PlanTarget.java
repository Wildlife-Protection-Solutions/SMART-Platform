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


import java.util.Locale;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;

/**
 * Represents a PlanTarget object, implemented by various types of targets: Numeric, Spatial and Administrative
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.plan_target")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="Category" , discriminatorType=DiscriminatorType.STRING)
public abstract class PlanTarget{

	public static final int MAX_NAME_LENGTH = 32;

	private String name;
	private Plan plan;
	private UUID uuid;

	@Transient
	private PlanTargetStatus currentStatus = null;
	
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}


	@ManyToOne(fetch = FetchType.LAZY)
	public Plan getPlan() {
		return plan;
	}
	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	@Transient
	public int hashCode() {
		if (getUuid() != null) {
			return getUuid().hashCode();
		}
		return super.hashCode();
	}

	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof PlanTarget){
			PlanTarget s = (PlanTarget)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return super.equals(s);
			}else if (s.getUuid() != null && this.getUuid() != null){
				return s.getUuid().equals(this.getUuid());
			}
		}
		return false;
	}
	
	@Column(name = "name")
	public String getName(){
		return name;
	}
	public void setName(String name){
		this.name = name;
	}
	

	
	/**
	 * This should only be called from the subclasses which will actually 
	 * instantiate a new object and pass it in here.
	 *  
	 * @param pt
	 * @return
	 */
	public PlanTarget clone(PlanTarget pt){
		pt.name = this.name;
		pt.plan = this.plan;
		return pt;
	}

	
	/**
	 * 
	 * @return the last computed target status
	 */
	@Transient
	public PlanTargetStatus getCurrentStatus(){
		return this.currentStatus;
	}
//	
//	/**
//	 * Recomputes the target status.  Creates a new session.  This
//	 * should not be called within a session.  Use refreshStatus(session)
//	 * if you have an active session.
//	 */
//	@Transient
//	public void refreshStatus(){
//		this.currentStatus = PlanTargetEngine.getInstance().computeTargetStatus(this);
//	}
//
	/**
	 * Recomputes the target status using
	 * the given session
	 */
	@Transient
	public void refreshStatus(Locale locale, Session session){
		this.currentStatus = (new PlanTargetEngine(locale)).computeTargetStatus(this, session);
	}
	
	/**
	 * Clears the targets current status
	 */
	@Transient
	public void clearCurrentStatus(){
		this.currentStatus = null;
	}


	/**
	 * Clone the give plan target.
	 */
	public abstract PlanTarget clone();
	
    /**
    *
    * @return String representation of the target
    */
   @Transient
   public abstract String getSummary(Locale l);

}
