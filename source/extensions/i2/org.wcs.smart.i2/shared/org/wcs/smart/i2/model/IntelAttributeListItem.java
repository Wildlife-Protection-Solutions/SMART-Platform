package org.wcs.smart.i2.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.NamedKeyItem;

/**
 * Model class of attribute list item for intelligence attributes.
 * 
 */
@Entity
@Table(name="i_attribute_list_item")
public class IntelAttributeListItem extends NamedKeyItem {

	private IntelAttribute attribute;

	/**
	 * Constructor.
	 */
	public IntelAttributeListItem() {
	}

	/**
	 * 
	 * @param attribute the attribute this list item is associated with
	 */
	public void setAttribute(IntelAttribute attribute){
		this.attribute = attribute;
	}
	
	/**
	 * 
	 * @return the attribute this list item is associated with
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	public IntelAttribute getAttribute(){
		return this.attribute;
	}

}
