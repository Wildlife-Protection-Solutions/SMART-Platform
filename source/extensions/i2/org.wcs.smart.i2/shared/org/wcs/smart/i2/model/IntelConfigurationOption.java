/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Configuration options for profiles plugin.  Currently only stores the
 * main menu name
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.i_config_option")
public class IntelConfigurationOption extends UuidItem {

	/**
	 * For the menu keys the default value will have a key of "mainmenu" but 
	 * values for other languages will have the key "mainmenu.<langcode>"
	 */
	public static final String MENU_NAME_KEY = "mainmenu"; //$NON-NLS-1$
	
	private ConservationArea ca;
	private String key;
	private String value;
	
	public IntelConfigurationOption() {
		
	}
	
	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	/**
	 * The configuration key; must be unique for the ca
	 * @return
	 */
	@Column(name="keyid")
	public String getKey() {
		return this.key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * The configuration value
	 * @return
	 */
	@Column(name="value")
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
