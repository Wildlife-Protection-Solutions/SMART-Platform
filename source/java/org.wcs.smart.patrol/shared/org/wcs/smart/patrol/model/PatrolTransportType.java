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

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyIconItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Class to represent a sub-transportation 
 * type of the patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "patrol_transport", schema="smart")
public class PatrolTransportType extends NamedKeyIconItem{
	
	private static final long serialVersionUID = 1L;
	
	public static final String LIBRARY_ICON_KEY = "transportation"; //$NON-NLS-1$

	private boolean isActive = true;
	private ConservationArea ca;
	private PatrolType patrolType;
	
	/**
	 * 
	 * @return <code>true</code> if patrol transport type active; <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive <code>true</code> if patrol transport type active; <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	/**
	 * 
	 * @return conservation area associated with transport type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}

	/**
	 * 
	 * @param ca conservation area associated with transport type
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return conservation area associated with patrol type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_type_uuid", referencedColumnName="uuid")
	public PatrolType getPatrolType(){
		return this.patrolType;
	}
	/**
	 * 
	 * @param ca conservation area associated with patrol type
	 */
	public void setPatrolType(PatrolType patrolType){
		this.patrolType = patrolType;
	}
	
}
