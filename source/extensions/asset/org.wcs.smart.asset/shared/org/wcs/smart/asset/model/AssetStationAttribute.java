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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Model class for asset station attribute. Identifies which attributes should be
 * collected for asset stations.
 * 
 * @author egouge
 */
@Entity
@Table(name="smrat.asset_station_attribute")
public class AssetStationAttribute {

	private AssetAttribute attribute;
	private int seqOrder;

	/**
	 * Constructor.
	 */
	public AssetStationAttribute() {
	}

	/**
	 * Get the asset_attribute.
	 * 
	 * @return asset_attribute
	 */
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="attribute_uuid", referencedColumnName="uuid")
	public AssetAttribute getAttributeAssetAttribute() {
		return this.attribute;
	}

	
	/**
	 * Set the asset_attribute.
	 * 
	 * @param attributeAssetAttribute
	 *            asset_attribute
	 */
	public void setAttributeAssetAttribute(AssetAttribute attributeAssetAttribute) {
		this.attribute = attributeAssetAttribute;
	}

	
	/**
	 * Get the seq_order.
	 * 
	 * @return seq_order
	 */
	@Column(name="seq_order")
	public int getOrder() {
		return this.seqOrder;
	}
	
	/**
	 * Set the seq_order.
	 * 
	 * @param seqOrder
	 *            seq_order
	 */
	public void setOrder(int seqOrder) {
		this.seqOrder = seqOrder;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		if (attribute == null) return super.hashCode();
		return attribute.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (attribute == null) return false;
		return attribute.equals(obj);

	}

}
