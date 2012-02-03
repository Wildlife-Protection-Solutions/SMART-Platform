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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.ConservationArea;

/**
 * Conservation Area data model category object.
 * <p>
 * Category objects form tree structures where a given 
 * category many of a parent and may have one or more
 * children.
 * </p>
 * <p>Categories may also have one or more attributes.</p>
 * <p>Categories may active or in-active which represents if the
 * category can still have data recorded with it..</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_category")
public class Category extends DmObject{
	
	private boolean isMultiple;			//if multiple observations can be recorded
	private ConservationArea ca; 		//conservation area of category
	private List<Category> children;	//children categories
	private Category parent;			//parent category
	private int categoryOrder;			//order of the category in relation to its siblings
	private List<CategoryAttribute> attributes;	//list of attributes
	private boolean isActive;			//if active.
	
	/**
	 * Creates a new category
	 */
	public Category(){
		super();
	}
	
	/**
	 * 
	 * @return if multiple observations can be recorded for this category
	 */
	@Column(name = "is_multiple")
	public boolean getIsMultiple(){
		return isMultiple;
	}
	/**
	 * 
	 * @param isMultiple if multiple observations can be recorded for this category
	 */
	public void setIsMultiple(boolean isMultiple){
		this.isMultiple = isMultiple;
	}
	
	/**
	 * 
	 * @return the order the category should appear relative to its siblings
	 */
	@Column(name = "cat_order")
	public int getCategoryOrder(){
		return this.categoryOrder;
	}
	/**
	 * 
	 * @param categoryOrder the order the category should appear relative to its siblings
	 */
	public void setCategoryOrder(int categoryOrder){
		this.categoryOrder = categoryOrder;
	}
	
	/**
	 * 
	 * @return conservation area associated with category
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	/**
	 * 
	 * @param ca conservation area associated with category
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return all children categories; <code>null</code> if leaf node
	 */
	@OneToMany(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_category_uuid")
	@OrderBy(clause = "cat_order")
	public List<Category> getChildren(){
		return this.children;
	}
	/**
	 * 
	 * @param children children categories
	 */
	public void setChildren(List<Category> children){
		this.children = children;
	}
	
	/**
	 * 
	 * @return parent category;  <code>null</code> if root node
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_category_uuid", insertable=false, updatable=false)
	public Category getParent(){
		return this.parent;
	}
	/**
	 * 
	 * @param parent the parent category
	 */
	public void setParent(Category parent){
		this.parent = parent;
	}

	/**
	 * 
	 * @return list of attributes associated directly 
	 * with category
	 */
	@OneToMany(fetch = FetchType.LAZY,mappedBy="id.category")
	@OrderBy(clause = "att_order")
	public List<CategoryAttribute> getAttributes(){
		return this.attributes;
	}
	/**
	 * 
	 * @param attributes list of attribute associated directory 
	 * with category
	 */
	public void setAttributes(List<CategoryAttribute> attributes){
		this.attributes = attributes;
	}
	/**
	 * 
	 * @return <code>true</code> if currently active; <code>false</code> otherwise
	 */
	@Column(name = "is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	/**
	 * 
	 * @param isActive <code>true</code> if currently active; <code>false</code> otherwise
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	/**
	 * Creates a clones of the category.
	 * 
	 * @param newCa the new conservation area to associated with the cloned category
	 * @param parent the parent category
	 * @param clonedAttributes a list of cloned attributes to be used when associating attributes with a category
	 * @return cloned cateogry
	 */
	public Category clone(ConservationArea newCa, Category parent, Set<Attribute> clonedAttributes, String defaultLang){
		
		Category clone = new Category();
		clone.copyValues(this, newCa, ca, defaultLang);
		
		clone.setCategoryOrder(this.getCategoryOrder());
		clone.setChildren(children);
		clone.setConservationArea(newCa);
		clone.setIsActive(this.getIsActive());
		clone.setIsMultiple(this.getIsMultiple());
		clone.setParent(parent);
		
		if (this.getAttributes() != null){
			clone.setAttributes(new ArrayList<CategoryAttribute>());
			for (CategoryAttribute attribute : this.getAttributes()){
				//find attribute
				Attribute newAttribute = null;
				for (Attribute clonedAttribute: clonedAttributes){
					if (clonedAttribute.getKeyId().equals(attribute.getAttribute().getKeyId())){
						newAttribute = clonedAttribute;
						break;
					}
				}
				
				if (newAttribute !=  null){
					clone.getAttributes().add(attribute.clone(clone, newAttribute));
				}
				
			}
		}
		if (this.getChildren() != null){
			clone.setChildren(new ArrayList<Category>());
			for (Category child : getChildren()){
				clone.getChildren().add(child.clone(newCa,clone, clonedAttributes, defaultLang));
			}
		}
		
		//clone language labels
		
		return clone;
	}
}
