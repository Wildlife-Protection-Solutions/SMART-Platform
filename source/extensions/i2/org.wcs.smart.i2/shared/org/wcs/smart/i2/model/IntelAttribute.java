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

import java.util.List;
import java.util.Locale;

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
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.i2.IIntelligenceLabelProvider;

/**
 * Model class of intelligence attribute for entity types
 * or relationship types
 * 
 */
@Entity
@Table(name="smart.i_attribute")
public class IntelAttribute extends NamedKeyItem{

	public enum AttributeType{
		NUMERIC("n"),
		TEXT("s"), 
		BOOLEAN("b"),
		LIST("l"),
		DATE("d");
		
		public String key;
		
		AttributeType(String key){
			this.key = key;
		}
		
		public String getGuiName(Locale l){
			IIntelligenceLabelProvider provider = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class);
			return provider.getLabel(this, l);
		}
	}
	
	private AttributeType type;
	private ConservationArea ca;

	private List<IntelAttributeListItem> listItems;
	
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
	public AttributeType getType() {
		return this.type;
	}
	
	/**
	 * Set the type.
	 * 
	 * @param type
	 *            type
	 */
	public void setType(AttributeType type) {
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
	public List<IntelAttributeListItem> getAttributeList(){
		return this.listItems;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<IntelAttributeListItem> listItems){
		this.listItems = listItems;
	}
}
