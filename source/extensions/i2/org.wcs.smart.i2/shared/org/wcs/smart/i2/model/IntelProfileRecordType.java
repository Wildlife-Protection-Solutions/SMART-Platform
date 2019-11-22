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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * 
 * Mapping of record type to profile
 * @author Emily
 * @since 7.0
 *
 */
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
