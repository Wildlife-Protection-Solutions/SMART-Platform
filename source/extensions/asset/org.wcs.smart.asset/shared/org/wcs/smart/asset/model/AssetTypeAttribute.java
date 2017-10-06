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

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * Model class of asset_type_attribute.  Maps
 * asset types to the attributes that should be collected
 * for that type.
 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset_type_attribute")
public class AssetTypeAttribute extends AbstractAssetTypeAttributeMapping{
	
	private AssetTypeAttributePk id = new AssetTypeAttributePk();	
	
	public AssetTypeAttribute(){		
	}
	
	@EmbeddedId
	public AssetTypeAttributePk getId(){
		return this.id;
	}
	public void setId(AssetTypeAttributePk id){
		this.id = id;
	}
	
	@Transient
	@Override
	public AssetType getAssetType() {
		return id.getAssetType();
	}

	@Override
	public void setAssetType(AssetType entity) {
		id.setAssetType(entity);
	}
	
	@Transient
	@Override
	public AssetAttribute getAttribute() {
		return id.getAttribute();
	}

	@Override
	public void setAttribute(AssetAttribute attribute) {
		id.setAttribute(attribute);
	}
	
	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof AssetTypeAttribute){
			return this.id.equals(((AssetTypeAttribute)o).id);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	/**
	 * Primary key object for category attribute association 
	 * 
	 */
	@Embeddable
	private static class AssetTypeAttributePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private AssetType assetType;
		private AssetAttribute attribute;
		

		public AssetTypeAttributePk(){
			
		}
		
		@ManyToOne
		@JoinColumn(name="asset_type_uuid")
		public AssetType getAssetType() {
			return assetType;
		}

		public void setAssetType(AssetType assetType) {
			this.assetType = assetType;
		}
		
		@ManyToOne
		@JoinColumn(name="attribute_uuid")
		public AssetAttribute getAttribute() {
			return attribute;
		}

		public void setAttribute(AssetAttribute attribute) {
			this.attribute = attribute;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof AssetTypeAttributePk)){
				return false;
			}
			AssetTypeAttributePk p = (AssetTypeAttributePk)key;
			return Objects.equals(attribute, p.attribute) && Objects.equals(assetType, p.assetType);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(attribute, assetType);
		  }
	}

}
