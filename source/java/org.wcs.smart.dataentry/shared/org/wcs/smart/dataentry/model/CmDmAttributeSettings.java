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
package org.wcs.smart.dataentry.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.datamodel.Attribute;

/**
 * Settings for attributes that are shared between several {@link CmAttribute} objects.
 * Typical example is a display mode option for default configuration of a particular
 * list attribute or default configuration of a tree nodes at root level of a tree attribute.
 *  
 * @author elitvin
 * @since 4.0.0
 */
@Entity
@Table(name="smart.cm_dm_attribute_settings")
@AssociationOverrides({
	@AssociationOverride(name = "id.dmAttribute", 
		joinColumns = @JoinColumn(name = "dm_attribute_uuid")),
	@AssociationOverride(name = "id.model", 
		joinColumns = @JoinColumn(name = "cm_uuid")) })
public class CmDmAttributeSettings {

	private CmDmAttributeSettingsPk id = new CmDmAttributeSettingsPk();
	private DisplayMode displayMode;

	@EmbeddedId
	public CmDmAttributeSettingsPk getId() {
		return this.id;
	}
	public void setId(CmDmAttributeSettingsPk id) {
		this.id = id;
	}

	/**
	 * {@link DisplayMode} makes sense only for list and tree attributes
	 */
	@Column(name="display_mode")
	@Enumerated(EnumType.STRING)
	public DisplayMode getDisplayMode() {
		return displayMode;
	}
	public void setDisplayMode(DisplayMode displayMode) {
		this.displayMode = displayMode;
	}
	
	@Transient
	public Attribute getDmAttribute() {
		return getId().getDmAttribute();
	}
	public void setDmAttribute(Attribute dmAttribute) {
		getId().setDmAttribute(dmAttribute);
	}
	
	@Transient
	public ConfigurableModel getModel() {
		return getId().getModel();
	}
	public void setModel(ConfigurableModel model) {
		getId().setModel(model);
	}

	@Transient
	public static final CmDmAttributeSettings createDefaultSettings(ConfigurableModel model, Attribute attribute) {
		CmDmAttributeSettings settings = new CmDmAttributeSettings();
		settings.setModel(model);
		settings.setDmAttribute(attribute);
		settings.setDisplayMode(DisplayMode.DEFAULT_DISPLAY_MODE);
		return settings;
	}
	
	/**
	 * Primary key for {@link CmDmAttributeSettings}
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	@Embeddable
	protected static class CmDmAttributeSettingsPk implements Serializable {

		private static final long serialVersionUID = 3475863788513127745L;
		
		private ConfigurableModel model;
		private Attribute dmAttribute;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="dm_attribute_uuid", referencedColumnName="uuid")
		public Attribute getDmAttribute() {
			return dmAttribute;
		}
		public void setDmAttribute(Attribute dmAttribute) {
			this.dmAttribute = dmAttribute;
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
		public ConfigurableModel getModel() {
			return model;
		}
		public void setModel(ConfigurableModel model) {
			this.model = model;
		}
		
		@Override
		public boolean equals(Object other){
			if (other == this) return true;
			if (other == null) return false;
			if (getClass() != other.getClass()) return false;
			CmDmAttributeSettingsPk o = (CmDmAttributeSettingsPk) other;
			return Objects.equals(dmAttribute, o.dmAttribute) && Objects.equals(model, o.model);
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(model, dmAttribute);
		}
	}
	
}
