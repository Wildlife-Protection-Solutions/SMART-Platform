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
package org.wcs.smart.asset.model;

import java.util.List;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Model class for asset attribute. Attribute definitions for all attributes used in the Asset Module.
 * 
 * @author egouge
 */
@Entity
@Table(name="asset_attribute", schema="smart")
public class AssetAttribute  extends NamedKeyItem{
	
	private static final long serialVersionUID = 1L;
	
	public enum AttributeType{
		NUMERIC("n"), //$NON-NLS-1$
		TEXT("s"),  //$NON-NLS-1$
		BOOLEAN("b"), //$NON-NLS-1$
		LIST("l"), //$NON-NLS-1$
		DATE("d"), //$NON-NLS-1$
		POSITION("p"); //$NON-NLS-1$
		
		public String key;
		
		AttributeType(String key){
			this.key = key;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(this, l);
		}
	}
	
	private AttributeType type;
	private ConservationArea ca;

	private List<AssetAttributeListItem> listItems;
	
	/**
	 * Constructor.
	 */
	public AssetAttribute() {
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
	public List<AssetAttributeListItem> getAttributeList(){
		return this.listItems;
	}
	
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<AssetAttributeListItem> listItems){
		this.listItems = listItems;
	}

}
