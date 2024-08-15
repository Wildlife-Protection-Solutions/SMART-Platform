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
package org.wcs.smart.patrol.model;

import java.util.List;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyIconItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Call to represent the patrol type.
 *
 * In SMART 8.1.0 this was re-purposed to represent Track Type
 * not a patrol type. Example Track Types may be "patrol", "animal",
 * "plane" etc. See ticket: https://app.assembla.com/spaces/smart-cs/tickets/3607/details
 * 
 * In the application GUI this update included changing all the location of "Patrol Type"
 * to "Track Type", however the code base still refers to as patrolType (getPatrolType() etc.);
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */

@Entity
@Table(name = "patrol_type", schema="smart")
public class PatrolType extends NamedKeyIconItem {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final Integer MAX_TRANSPORT_NAME_LENGTH = 128;
	
	//Min and max values for max_speed are the same as in CyberTracker
	public static final int MAX_SPEED_MIN_VALUE = 0;
	public static final int MAX_SPEED_MAX_VALUE = 10000;


	public static final String LIBRARY_ICON_KEY = "transportation"; //$NON-NLS-1$
	
	/*
	 * Every CA must have a type of MIXED which is create/managed by the system
	 * and cannot be edited by the user
	 */
	/**
	 * The default types for new conservation areas.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum DefaultType {
		GROUND, 
		MARINE, 
		AIR,
		MIXED;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(this, Locale.getDefault());
		}

		public int getDefaultMaxSpeed() {
			switch (this) {
			case GROUND: return 120;
			case MARINE: return 70;
			case AIR: return 500;
			case MIXED: return MAX_SPEED_MAX_VALUE;
			}
			return MAX_SPEED_MAX_VALUE;
		}
		
		public String getKeyId() {
			return this.name().toLowerCase();
		}
		
		public boolean requiresPilot(){
			return (this == MARINE || this == AIR || this == MIXED);
		}
		
		public String getIconKey() {
			switch(this) {
			case AIR: return "patrol_pilot_airplane"; //$NON-NLS-1$
			case GROUND: return "foot"; //$NON-NLS-1$
			case MARINE: return "patrol_pilot_boat"; //$NON-NLS-1$
			default: return null;
			}
		}
	}

	private boolean isActive;
	private boolean requiresPilot;
	private Integer maxSpeed;
	private List<PatrolTransportType> transportTypes;
	private ConservationArea ca;
	
	public PatrolType(){
	}
	
	@Transient
	public boolean isMixed() {
		return this.getKeyId().equals(DefaultType.MIXED.getKeyId());
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
	
	/**
	 * 
	 * @return <code>true</code> if patrol type requires a pilot, <code>false</code> otherwise
	 */
	@Column(name = "requires_pilot")
	public boolean getRequiresPilot(){
		return this.requiresPilot;
	}
	/**
	 * 
	 * @param isActive  <code>true</code> if patrol type requires a pilot, <code>false</code> otherwise
	 */
	public void setRequiresPilot(boolean requiresPilot){
		this.requiresPilot = requiresPilot;
	}
	
	/**
	 * The maximum speed in km/h that should be used
	 * to validate gps observations for this patrol type
	 * 
	 * @return
	 */
	@Column(name = "max_speed")
	public Integer getMaxSpeed() {
		return maxSpeed;
	}
	
	public void setMaxSpeed(Integer maxSpeed) {
		this.maxSpeed = maxSpeed;
	}
	
	/**
	 * 
	 * @return list of transport types associated with patrol types
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="patrolType", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<PatrolTransportType> getTransportTypes(){
		return this.transportTypes;
	}
	
	/**
	 * 
	 * @param ttypes list of transport types associated with patrol types
	 */
	public void setTransportTypes(List<PatrolTransportType> ttypes){
		this.transportTypes = ttypes;
	}
	
}
