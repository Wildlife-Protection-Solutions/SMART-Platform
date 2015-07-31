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

import java.io.Serializable;
import java.util.List;

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
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;

/**
 * Call to represent the patrol type.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.patrol_type")
@AssociationOverrides({
	@AssociationOverride(name = "id.conservationArea", 
		joinColumns = @JoinColumn(name = "ca_uuid")),
	@AssociationOverride(name = "id.patrolType", 
		joinColumns = @JoinColumn(name = "patrol_type")) })
public class PatrolType {

	/**
	 * The supported patrol types.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum Type {
		GROUND, 
		MARINE, 
		AIR;
	}

	public static final Integer MAX_TRANSPORT_NAME_LENGTH = 128;
	
	private PatrolTypePk pk;
	private boolean isActive;
	private List<PatrolTransportType> transportTypes;
	
	public PatrolType(){
		pk = new PatrolTypePk();
	}
	
	/**
	 * 
	 * @return patrol type unique identifier
	 */
	@EmbeddedId
	public PatrolTypePk getId(){
		return pk;
	}
	/**
	 * 
	 * @param id patrol type unique identifier
	 */
	public void setId(PatrolTypePk id){
		this.pk = id;
	}
	
	/**
	 * 
	 * @return conservation area associated with patrol type
	 */
	@Transient 
	public ConservationArea getConservationArea(){
		return this.pk.getConservationArea();
	}
	/**
	 * 
	 * @param ca conservation area associated with patrol type
	 */
	public void setConservationArea(ConservationArea ca){
		this.pk.setConservationArea(ca);
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
	 * @return the patrol type
	 */
	@Transient
	public Type getType(){
		return this.pk.getType();
	}
	/**
	 * 
	 * @param type the patrol type
	 */
	public void setType(Type type){
		this.pk.setType(type);
	}
	
	/**
	 * 
	 * @return list of transport types associated with patrol types
	 */
	@OneToMany(fetch = FetchType.LAZY) //, cascade={CascadeType.}, orphanRemoval=true)
	@JoinColumns({
		@JoinColumn(name="patrol_type", referencedColumnName="patrol_type", insertable = false, updatable = false),
		@JoinColumn(name="ca_uuid", referencedColumnName="ca_uuid", insertable = false, updatable = false)
	})
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
	

	/**
	 * Primary key class for hibernate mapping of patrol type.
	 * 
	 */
	@Embeddable
	private static class PatrolTypePk implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private ConservationArea ca;
		private PatrolType.Type pt;

		public PatrolTypePk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
		public ConservationArea getConservationArea() {
			return this.ca;
		}

		public void setConservationArea(ConservationArea ca) {
			this.ca = ca;
		}
		
		@Column(name="patrol_type")
		@Enumerated(EnumType.STRING)
		public PatrolType.Type getType() {
			return pt;
		}

		public void setType(PatrolType.Type type) {
			this.pt = type;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof PatrolTypePk)){
				return false;
			}
			PatrolTypePk p = (PatrolTypePk)key;
			
			
			if (p.ca == null || this.ca == null ||
				p.pt == null || this.pt == null ){
				
				if (p.ca == null && this.ca == null && 
					p.pt == null && this.pt == null){
						return true;
				}
				return false;
			}
			return p.ca.equals(this.ca) && p.pt.equals(this.pt);
			
		}
	}
}
