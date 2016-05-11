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
package org.wcs.smart.cybertracker.model;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Mapping between {@link ConfigurableModel} and {@link CyberTrackerPropertiesProfile}.<br><br>
 * 
 * NOTE: This class is mapped as one to one with {@link ConfigurableModel}, but we cannot
 * have a reference to {@link CyberTrackerPropertiesProfile} in a {@link ConfigurableModel}
 * class itself, as this should be valid only if CyberTracker plugin is installed.
 * 
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name = "smart.cm_ct_properties_profile")
public class ConfigurableModelCtPropertiesProfile {
	
	private ConfigurableModelCtPropertiesProfilePk id = new ConfigurableModelCtPropertiesProfilePk();

	private CyberTrackerPropertiesProfile profile;

	@EmbeddedId
	public ConfigurableModelCtPropertiesProfilePk getId() {
		return this.id;
	}
	public void setId(ConfigurableModelCtPropertiesProfilePk id) {
		this.id = id;
	}
	
	@Transient
	public ConfigurableModel getModel() {
		return id.model;
	}
	public void setModel(ConfigurableModel model) {
		this.id.model = model;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="profile_uuid", referencedColumnName="uuid")
	public CyberTrackerPropertiesProfile getProfile() {
		return profile;
	}
	public void setProfile(CyberTrackerPropertiesProfile profile) {
		this.profile = profile;
	}

	@Override
	public boolean equals(Object other){
		if (other instanceof ConfigurableModelCtPropertiesProfile){
			return ((ConfigurableModelCtPropertiesProfile)other).id.equals(this.id);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Primary key for {@link ConfigurableModelCtPropertiesProfile}
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	@Embeddable
	protected static class ConfigurableModelCtPropertiesProfilePk implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private ConfigurableModel model;

		@OneToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name="cm_uuid", referencedColumnName="uuid")
		public ConfigurableModel getModel() {
			return model;
		}
		public void setModel(ConfigurableModel model) {
			this.model = model;
		}
		
		@Override
		public boolean equals(Object other){
			if (other instanceof ConfigurableModelCtPropertiesProfilePk){
				return ((ConfigurableModelCtPropertiesProfilePk)other).getModel().equals(this.model);
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return model.hashCode();
		}
	}
	
}
