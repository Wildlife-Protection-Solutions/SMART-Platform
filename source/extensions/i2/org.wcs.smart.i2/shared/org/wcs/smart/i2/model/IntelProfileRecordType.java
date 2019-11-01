package org.wcs.smart.i2.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

public class IntelProfileRecordType {

	private IntelProfileRecordTypePk id = new IntelProfileRecordTypePk();
	
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public IntelProfileRecordTypePk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(IntelProfileRecordTypePk id){
		this.id = id;
	}
	
	@Transient
	public IntelRecordSource getRecordSource() {
		return id.getRecordSource();
	}
	@Transient
	public void setRecordSource(IntelRecordSource entityType) {
		id.setRecordSource(entityType);
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
	private static class IntelProfileRecordTypePk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private IntelRecordSource recordSource;
		private IntelProfile config;

		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="recordsource_uuid")
		public IntelRecordSource getRecordSource() {
			return recordSource;
		}

		public void setRecordSource(IntelRecordSource recordSource) {
			this.recordSource = recordSource;
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
			return Objects.equals(config, ((IntelProfileRecordTypePk)key).config) &&
					Objects.equals(recordSource, ((IntelProfileRecordTypePk)key).recordSource);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(config, recordSource);
		}
	}
}
