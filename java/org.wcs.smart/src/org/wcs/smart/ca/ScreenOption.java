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
package org.wcs.smart.ca;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Options for screens configuration.
 * 
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.screen_option")
public class ScreenOption extends UuidItem {

	public enum ScreenOptionMeta {
//		START_DATE("Start Date"),
//		END_DATE("End Date"),
		TYPE("Patrol Type"),
		TRANSPORT("Transport Type"),
		ARMED("Armed"),
		STATION("Station"),
		TEAM("Team"),
		MANDATE("Patrol Mandate"),
		OBJECTIVE("Patrol Objective"),
		COMMENT("Patrol Comment"),
		MEMBERS("Patrol Members"),
		LEADER("Patrol Leader"),
		PILOT("Patrol Pilot");
		
		private String guiLabel;
		
		private ScreenOptionMeta(String guiLabel) {
			this.guiLabel = guiLabel;
		}
		
		public String getGuiLabel() {
			return guiLabel;
		}
	}

	private ConservationArea ca;
	private ScreenOptionMeta type;
	private boolean visible = true;
	private String stringValue;
	private Boolean booleanValue;
	private byte[] uuidValue;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="type")
	@Enumerated(EnumType.STRING)
	public ScreenOptionMeta getType() {
		return type;
	}
	public void setType(ScreenOptionMeta type) {
		this.type = type;
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

}
