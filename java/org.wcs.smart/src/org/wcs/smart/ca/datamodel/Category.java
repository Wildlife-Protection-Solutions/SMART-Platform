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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

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
	 * computes the full key for the category.  This is of
	 * the form <parent key>.<parent key>.<parent key>...<category key>
	 * @return
	 */
	@Transient
	public String getFullKey(){
		if (parent != null){
			return parent.getFullKey() + "." + getKeyId();
		}else{
			return getKeyId();
		}
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
	 * Gets children that are active or inactive  
	 * @param active <code>true</code> for active only children, <code>false</code> for in-active only children
	 * @return list of only active or inactive children
	 */
	@Transient 
	public List<Category> getChildren(boolean active){
		List<Category> tmp = new ArrayList<Category>();
		if (getChildren() != null){
			for (Iterator<Category> iterator = getChildren().iterator(); iterator.hasNext();) {
				Category category = (Category) iterator.next();
				if (category.getIsActive() == active){
					tmp.add(category);
				}
			}
		}
		return tmp;
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
	 * Gets attributes that are active or inactive  
	 * @param active <code>true</code> for active only attributes, <code>false</code> for in-active only attributes
	 * @return list of only active or inactive attributes
	 */
	@Transient 
	public List<CategoryAttribute> getAttributes(boolean active){
		List<CategoryAttribute> tmp = new ArrayList<CategoryAttribute>();
		if (getAttributes() != null){
			for (Iterator<CategoryAttribute> iterator = getAttributes().iterator(); iterator.hasNext();) {
				CategoryAttribute category = (CategoryAttribute) iterator.next();
				if (category.getIsActive() == active){
					tmp.add(category);
				}
			}
		}
		return tmp;
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
	
	/**
	 * 
	 * @return the category name concatenated with
	 * all parent category names.
	 */
	@Transient
	public String getFullCategoryName(){
		if (parent == null){
			return getName();
		}else{
			return getName() + " - " + parent.getFullCategoryName();
		}
	}
	
	/**
	 * finds all attributes, first looking at parent attributes
	 * and working way down to the children
	 * 
	 * @param attributes list to populate
	 */
	@Transient
	public void getAllAttribute(List<Attribute> attributes){
		if (getParent() != null){
			getParent().getAllAttribute(attributes);
			
		}
		if (getAttributes() != null){
			for (CategoryAttribute cat : getAttributes(true)){
				attributes.add(cat.getAttribute());
			}
		}
	}
	
	/**
	 * 
	 * @return <code>true</code> if this category or any of it's parents have attributes; <code>false</code> otherwise
	 */
	@Transient
	public boolean hasAttributes(){
		if (getAttributes() != null && getAttributes().size() > 0){
			return true;
		}else if (parent == null){
			return false;
		}else{
			return parent.hasAttributes();
		}
	}
	
	
	@Override
	public int hashCode(){
		if (uuid != null){
			return Arrays.hashCode(uuid);
		}else{
			return super.hashCode();
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof Category){
			Category s = (Category)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return super.equals(other);
			}else if (s.getUuid() != null && this.getUuid() != null){
				return Arrays.equals(s.getUuid(), this.getUuid());
			}
		}
		return false;
	}
}
