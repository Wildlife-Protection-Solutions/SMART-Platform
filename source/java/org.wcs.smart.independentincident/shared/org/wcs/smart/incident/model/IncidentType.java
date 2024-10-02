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
package org.wcs.smart.incident.model;

import java.util.Locale;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyIconItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 
 * Incident types
 * 
 * @since 8.1.0
 */

@Entity
@Table(name = "incident_type", schema="smart")
public class IncidentType extends NamedKeyIconItem {
	
	private static final long serialVersionUID = 1L;
	
	public static final String MOVE_PATROL_OP = "movepatrol"; //$NON-NLS-1$
	public static final String LINK_PATROL_OP = "linkpatrol"; //$NON-NLS-1$
	/**
	 * The default types for new conservation areas.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum DefaultType {
		INCIDENT("incident"),
		INTEGRATE("integrate"),
		INTEGRATE_LINK("integratelink"),
		INTEGRATE_MOVE("integratemove");
		
		private String key;
		
		DefaultType(String key){
			this.key = key;
		}
		public String getKeyId() {
			return this.key;
		}
	}

	private boolean isActive;
	private ConservationArea ca;
	private IncidentType fallbackType;
	private String options;
	
	public IncidentType(){
	
	}
		
	/**
	 * 
	 * @return conservation area associated with patrol type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="fallback_type_uuid", referencedColumnName="uuid")
	public IncidentType getFallbackType(){
		return this.fallbackType;
	}
	/**
	 * 
	 * @param ca conservation area associated with patrol type
	 */
	public void setFallbackType(IncidentType fallbackType){
		this.fallbackType = fallbackType;
	}
	
	/**
	 * 
	 * @return conservation area associated with patrol type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	/**
	 * 
	 * @param ca conservation area associated with patrol type
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return <code>true</code> if patrol type active, <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive  <code>true</code> if patrol type active, <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	@Column(name = "options")
	public String getOptions() {
		return this.options;
	}
	
	public void setOptions(String options) {
		this.options = options;
	}
	
	@Transient
	public boolean doLinkPatrol() {
		return getOptions() != null && getOptions().equalsIgnoreCase(LINK_PATROL_OP);
		
	}
	@Transient
	public void setLinkPatrol(boolean link) {
		setOptions(link ? LINK_PATROL_OP : null);
	}
	
	@Transient
	public boolean doMovePatrol() {
		return getOptions() != null && getOptions().equalsIgnoreCase(MOVE_PATROL_OP);
	}
	@Transient
	public void setMovePatrol(boolean move) {
		setOptions(move ? MOVE_PATROL_OP : null);
	}
	
	@Transient
	public boolean isSystem() {
		for (DefaultType t : DefaultType.values()) {
			if (t.getKeyId().equalsIgnoreCase(getKeyId())) return true;
		}
		return false;
	}
}
