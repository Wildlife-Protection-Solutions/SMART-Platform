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

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.NamedKeyIconItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Class to represent a sub-transportation 
 * type of the patrol.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "patrol_transport_group", schema="smart")
public class PatrolTransportGroup extends NamedKeyIconItem{
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * The default types for new conservation areas.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum DefaultType {
		GROUND, 
		MARINE, 
		AIR;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(this, Locale.getDefault());
		}
		
		public String getKeyId() {
			return this.name().toLowerCase();
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
	
	public final static PatrolTransportGroup MIXED;
	
	static {
		MIXED = new PatrolTransportGroup() {
			private String name = null;
			public String getName() {
				if (this.name == null) {
					name = SmartContext.INSTANCE.getClass(IPatrolLabelProvider.class).getLabel(IPatrolLabelProvider.MIXED_KEY, Locale.getDefault());	
				}
				return name;
					
			}
		};
		
		
	}
	
	private PatrolType patrolType;
	private List<PatrolTransportType> transportTypes;
	
	/**
	 * 
	 * @return conservation area associated with patrol type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_type_uuid", referencedColumnName="uuid")
	public PatrolType getPatrolType(){
		return this.patrolType;
	}
	/**
	 * 
	 * @param ca conservation area associated with patrol type
	 */
	public void setPatrolType(PatrolType patrolType){
		this.patrolType = patrolType;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="transportGroup", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<PatrolTransportType> getTransportTypes(){
		return this.transportTypes;
	}
	
	public void setTransportTypes(List<PatrolTransportType> transportTypes) {
		this.transportTypes = transportTypes;
	}

	
	/**
	 * joins the group name with the type name 
	 * @return
	 */
	@Transient
	public String getGroupTypeLabel() {
		return MessageFormat.format("{0} ({1})", getName(), getPatrolType().getName()); //$NON-NLS-1$
	}
}
