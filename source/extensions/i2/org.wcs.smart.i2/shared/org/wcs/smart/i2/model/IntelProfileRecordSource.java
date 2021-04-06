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
 * Mapping record sources to profiles
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="smart.i_profile_record_source")
public class IntelProfileRecordSource {
	
	private IProfileRecordPk id = new IProfileRecordPk();
	
	public IntelProfileRecordSource() {
		
	}
	@EmbeddedId
	public IProfileRecordPk getId(){
		return this.id;
	}
	public void setId(IProfileRecordPk id){
		this.id = id;
	}
	
	@Transient
	public IntelRecordSource getRecordSource() { return id.getRecordSource(); }
	
	@Transient
	public IntelProfile getProfile() { return id.getProfile(); }
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (other.getClass() != getClass()) return false;
		return id.equals(  ((IntelProfileRecordSource)other).id);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	/**
	 * Primary key object for category attribute association 
	 * 
	 */
	@Embeddable
	public static class IProfileRecordPk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private IntelRecordSource source;
		private IntelProfile profile;
		

		public IProfileRecordPk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="profile_uuid")
		public IntelProfile getProfile() {
			return profile;
		}

		public void setProfile(IntelProfile profile) {
			this.profile = profile;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="record_source_uuid")
		public IntelRecordSource getRecordSource() {
			return source;
		}

		public void setRecordSource(IntelRecordSource source) {
			this.source = source;
		}
		
		@Override
		public boolean equals(Object key) {
			if (key == null) return false;
			if (this == key) return true;
			if (getClass() != key.getClass()) return false;
			IProfileRecordPk p = (IProfileRecordPk)key;
			return Objects.equals(p.source, this.source) &&
					Objects.equals(p.profile, this.profile);
					
		}
		@Override
		public int hashCode() {
			return Objects.hash(source, profile);
		}
	}
}
