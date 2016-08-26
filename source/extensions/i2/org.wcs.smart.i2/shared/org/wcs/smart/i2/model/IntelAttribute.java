package org.wcs.smart.i2.model;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

/**
 * Model class of intelligence attribute for entity types
 * or relationship types
 * 
 */
@Entity
@Table(name="smart.i_attribute")
public class IntelAttribute extends NamedKeyItem{

	public enum IAttributeType{
		NUMERIC,
		STRING, 
		BOOLEAN,
		LIST,
		DATE
	}
	
	private IAttributeType type;
	private ConservationArea ca;

	private Set<IntelAttributeListItem> listItems;
	
	/**
	 * Constructor.
	 */
	public IntelAttribute() {
	}


	/**
	 * Get the type.
	 * 
	 * @return type
	 */
	@Enumerated(EnumType.STRING)
	@Column(name="type")
	public IAttributeType getType() {
		return this.type;
	}
	
	/**
	 * Set the type.
	 * 
	 * @param type
	 *            type
	 */
	public void setType(IAttributeType type) {
		this.type = type;
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
	 * Only valid for list attributes.
	 * 
	 * @return set of valid list elements
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade={CascadeType.ALL}, orphanRemoval=true)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public Set<IntelAttributeListItem> getAttributeList(){
		return this.listItems;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(Set<IntelAttributeListItem> listItems){
		this.listItems = listItems;
	}
}
