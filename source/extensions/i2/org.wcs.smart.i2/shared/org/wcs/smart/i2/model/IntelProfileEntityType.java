package org.wcs.smart.i2.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

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
