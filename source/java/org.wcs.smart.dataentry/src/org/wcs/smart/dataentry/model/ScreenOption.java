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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;


/**
 * Options for screens configuration.
 * 
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.screen_option")
public class ScreenOption extends UuidItem {

	private ConservationArea ca;
	private String type;
	private String resource;
	private boolean visible = true;
	private String stringValue;
	private Boolean booleanValue;
	private byte[] uuidValue;
	private List<ScreenOptionUuid> uuidList;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="type")
//	@Enumerated(EnumType.STRING)
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@Column(name="resource")
	public String getResource() {
		return resource;
	}
	public void setResource(String resource) {
		this.resource = resource;
	}

	@Column(name="is_visible")
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
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
	public byte[] getUuidValue() {
		return uuidValue;
	}
	public void setUuidValue(byte[] uuidValue) {
		this.uuidValue = uuidValue;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="screenOption", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<ScreenOptionUuid> getUuidList() {
		if (uuidList == null)
			uuidList = new ArrayList<ScreenOptionUuid>();
		return uuidList;
	}
	public void setUuidList(List<ScreenOptionUuid> uuidList) {
		this.uuidList = uuidList;
	}

	
}
