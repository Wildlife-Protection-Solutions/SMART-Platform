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

import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


/**
 * Options for screens configuration.
 * 
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "ct_metadata_value", schema="smart")
public class MetadataFieldValue extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	public static final int MAX_STRING_LENGTH = 8192;
			
	private ConservationArea ca;
	
	//key identifying the metadata field
	private String key;

	//the package this metadata is associated with
	private AbstractCtPackage ctpackage;
	
	//if the item should be visible or not
	private boolean visible = true;
	
	private boolean isRequired = true;

	//default value
	private String stringValue;
	private Boolean booleanValue;
	private UUID uuidValue;
	private List<MetadataFieldUuidValue> uuidList;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="keyid")
	public String getMetadataKey() {
		return key;
	}
	public void setMetadataKey(String type) {
		this.key = type;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="package_uuid", referencedColumnName="uuid")
	public AbstractCtPackage getCtPackage() {
		return ctpackage;
	}
	public void setCtPackage(AbstractCtPackage ctpackage) {
		this.ctpackage = ctpackage;
	}
	
	@Column(name="is_visible")
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	@Column(name="is_required")
	public boolean isRequired() {
		return isRequired;
	}
	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}
	
	@Column(name="string_value")
	public String getStringValue() {
		return stringValue;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

	@Column(name="boolean_value")
	public Boolean getBooleanValue() {
		return booleanValue;
	}
	public void setBooleanValue(Boolean value) {
		booleanValue = value;
	}
	
	@Column(name="uuid_value")
	public UUID getUuidValue() {
		return uuidValue;
	}
	public void setUuidValue(UUID uuidValue) {
		this.uuidValue = uuidValue;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="metadata", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<MetadataFieldUuidValue> getUuidList() {
		return uuidList;
	}
	
	public void setUuidList(List<MetadataFieldUuidValue> uuidList) {
		this.uuidList = uuidList;
	}

	
}
