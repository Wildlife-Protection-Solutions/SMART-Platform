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
package org.wcs.smart.ca.datamodel;

import java.io.Serializable;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;


/**
 * Association between a Category and its attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_cat_att_map")
@AssociationOverrides({
	@AssociationOverride(name = "id.attribute", 
		joinColumns = @JoinColumn(name = "attribute_uuid")),
	@AssociationOverride(name = "id.category", 
		joinColumns = @JoinColumn(name = "category_uuid")) })
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CategoryAttribute {

	private CategoryAttributePk id = new CategoryAttributePk();	
	private boolean isActive;
	private int order;
	
	/**
	 * Create new association
	 */
	public CategoryAttribute(){
		
	}
	/**
	 * Creates a new association
	 * @param category category
	 * @param attribute attribute to associate with the category
	 */
	public CategoryAttribute(Category category, Attribute attribute){
		super();
		setCategory(category);
		setAttribute(attribute);
	}
	/**
	 * 
	 * @return primary key for association
	 */
	@EmbeddedId
	public CategoryAttributePk getId(){
		return id;
	}
	/**
	 * 
	 * @param id primary key for association
	 */
	public void setId(CategoryAttributePk id){
		this.id = id;
	}
	
	/**
	 * 
	 * @return the association attribute 
	 */
	@Transient 
	public Attribute getAttribute(){
		return id.getAttribute();
	}
	/**
	 * @param attribute the association attribute
	 */
	public void setAttribute(Attribute attribute){
		id.setAttribute(attribute);
	}
	
	/**
	 * @return the association category
	 */
	@Transient 
	public Category getCategory(){
		return id.getCategory();
	}
	/**
	 * @param category the association category
	 */
	public void setCategory(Category category){
		id.setCategory(category);
	}
	
	/**
	 * 
	 * @return the order of the attribute in relation 
	 * to its siblings
	 */
	@Column(name = "att_order")
	public int getOrder(){
		return this.order;
	}
	/**
	 * 
	 * @param order the order of the attribute in relation 
	 * to its siblings
	 */
	public void setOrder(int order){
		this.order = order;
	}
	
	/**
	 * 
	 * @return <code>true</code> if association is active; <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive the current state of the association
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}

	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof CategoryAttribute){
			return this.id.equals(((CategoryAttribute)o).id);
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
	private static class CategoryAttributePk implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private Category category;
		private Attribute attribute;
		

		public CategoryAttributePk(){
			
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="category_uuid")
		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
		
		@ManyToOne(cascade = {CascadeType.ALL})
		@JoinColumn(name="attribute_uuid")
		public Attribute getAttribute() {
			return attribute;
		}

		public void setAttribute(Attribute attribute) {
			this.attribute = attribute;
		}
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof CategoryAttributePk)){
				return false;
			}
			CategoryAttributePk p = (CategoryAttributePk)key;
			
			if (p.category == null || this.category == null ||
				p.attribute == null || this.attribute == null ){
				
				if (p.category == null && this.category == null && 
					p.attribute == null && this.attribute == null){
						return true;
				}
				return false;
			}
			
			return p.category.equals(this.category) &&
					p.attribute.equals(this.attribute);
		}
		@Override
		public int hashCode() {
		    int code = 0;
		    if (category != null) {code += category.hashCode();}
		    code *= 31;
		    if (attribute != null) {code += attribute.hashCode(); }
		    return code;
		  }
	}
	

	public CategoryAttribute clone(Category category, Attribute attribute){
		CategoryAttribute clone = new CategoryAttribute();
		
		clone.setOrder(this.getOrder());
		clone.setIsActive(this.getIsActive());
		clone.setCategory(category);
		clone.setAttribute(attribute);
		
		return clone;
	}
}
