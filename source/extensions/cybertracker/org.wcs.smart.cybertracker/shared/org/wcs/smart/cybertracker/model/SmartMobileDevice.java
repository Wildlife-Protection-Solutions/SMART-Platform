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
package org.wcs.smart.cybertracker.model;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IconItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.icon.Icon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Managing individual smart mobile devices.
 * 
 * @since 8.0.0
 */
@Entity
@Table(name = "ct_device", schema="smart")
public class SmartMobileDevice extends UuidItem implements IconItem{

	private static final long serialVersionUID = 1L;
	
	public static final int MAX_DEVICE_ID_LENGTH = 128;
	
	private String name;
	private ConservationArea ca;
	private String deviceId;
	private Icon icon;
	
	@Column(name = "device_id")
	public String getDeviceId() {
		return this.deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "icon_uuid", referencedColumnName = "uuid")
	public Icon getIcon() {
		return this.icon;
	}
	
	public void setIcon(Icon icon) {
		this.icon = icon;
	}
	
	@Column(name = "name")
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ca_uuid", referencedColumnName = "uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}

	/**
	 * Sets the Conservation Area.
	 * 
	 * @param ca
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
}
