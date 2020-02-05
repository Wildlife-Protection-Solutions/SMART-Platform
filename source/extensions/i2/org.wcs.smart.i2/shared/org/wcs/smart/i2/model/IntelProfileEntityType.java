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
package org.wcs.smart.i2.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Mapping of entity types to profiles
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.i_profile_entity_type")
public class IntelProfileEntityType {

	private IntelProfileEntityTypePk id = new IntelProfileEntityTypePk();
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public IntelProfileEntityTypePk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(IntelProfileEntityTypePk id){
		this.id = id;
	}
	
	@Transient
	public IntelEntityType getEntityType() {
		return id.getEntityType();
	}
	@Transient
	public void setEntityType(IntelEntityType entityType) {
		id.setEntityType(entityType);
	}
	@Transient
	public IntelProfile getProfile() {
		return id.getProfile();
	}
	@Transient
	public void setProfile(IntelProfile config) {
		id.setProfile(config);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (other.getClass() != getClass()) return false;
		return id.equals(  ((IntelProfileEntityType)other).id);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Embeddable
	private static class IntelProfileEntityTypePk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private IntelEntityType entityType;
		private IntelProfile config;

		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="entity_type_uuid")
		public IntelEntityType getEntityType() {
			return entityType;
		}

		public void setEntityType(IntelEntityType entityType) {
			this.entityType = entityType;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="profile_uuid")
		public IntelProfile getProfile() {
			return config;
		}

		public void setProfile(IntelProfile config) {
			this.config = config;
		}
		
		@Override
		public boolean equals(Object key) {
			if (key == null) return false;
			if (key == this) return true;
			if (key.getClass() == getClass()) return true;
			return Objects.equals(config, ((IntelProfileEntityTypePk)key).config) &&
					Objects.equals(entityType, ((IntelProfileEntityTypePk)key).entityType);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(config, entityType);
		}
	}
}
