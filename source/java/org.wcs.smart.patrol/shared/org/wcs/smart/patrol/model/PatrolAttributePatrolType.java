/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Link patrol attributes to track (patrol) types
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
@Entity
@Table(name = "patrol_attribute_patrol_type", schema="smart")
public class PatrolAttributePatrolType implements Serializable {

	private static final long serialVersionUID = 1L;

	private PatrolAttributePatrolTypePk id = new PatrolAttributePatrolTypePk();	
	private boolean isActive;

	
	public PatrolAttributePatrolType(){
	}
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public PatrolAttributePatrolTypePk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(PatrolAttributePatrolTypePk id){
		this.id = id;
	}
	
	/**
	 * 
	 * @return <code>true</code> if patrol type active, <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive  <code>true</code> if patrol type active, <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	

	
	/**
	 * 
	 * @return the patrol type 
	 */
	@Transient 
	public PatrolType getPatrolType(){
		return id.getPatrolType();
	}
	/**
	 * @param type the patrol type
	 */
	public void setPatrolType(PatrolType type){
		id.setPatrolType(type);
	}
	
	/**
	 * @return the patrol attribute
	 */
	@Transient 
	public PatrolAttribute getPatrolAttribute(){
		return id.getPatrolAttribute();
	}
	/**
	 * @param attribute the patrol attribute
	 */
	public void setPatrolAttribute(PatrolAttribute attribute){
		id.setPatrolAttribute(attribute);
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof PatrolAttributePatrolType){
			return this.id.equals(((PatrolAttributePatrolType)o).id);
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
	private static class PatrolAttributePatrolTypePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private PatrolType type;
		private PatrolAttribute attribute;
		

		public PatrolAttributePatrolTypePk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="patrol_type_uuid")
		public PatrolType getPatrolType() {
			return type;
		}

		public void setPatrolType(PatrolType type) {
			this.type = type;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="patrol_attribute_uuid")
		public PatrolAttribute getPatrolAttribute() {
			return attribute;
		}
		public void setPatrolAttribute(PatrolAttribute attribute) {
			this.attribute = attribute;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof PatrolAttributePatrolTypePk)){
				return false;
			}
			PatrolAttributePatrolTypePk p = (PatrolAttributePatrolTypePk)key;
			
			return Objects.equals(p.attribute, this.attribute) && Objects.equals(p.type, this.type);
		}
		@Override
		public int hashCode() {
			return Objects.hash(attribute, type);    
		}
	}
}
