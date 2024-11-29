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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.icon.Icon;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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
@Table(name = "dm_category", schema="smart")
//@Cache(region="org.wcs.smart.ca.datamodel.Category", usage = CacheConcurrencyStrategy.READ_WRITE)
public class Category extends DmObject implements HkeyObject{
	
	private static final long serialVersionUID = 1L;
	
	private static final String FULL_NAME_SEPARATOR = " - "; //$NON-NLS-1$
	private boolean isMultiple;			//if multiple observations can be recorded
	private ConservationArea ca; 		//conservation area of category
	private List<Category> children;	//children categories
	
	private Category parent;			//parent category
	private int categoryOrder;			//order of the category in relation to its siblings
	private List<CategoryAttribute> attributes;	//list of attributes
	
	private boolean isActive;			//if active.
	private String categoryHkey;	//the full key of the category "parent.parent.parent.me"
	
	
	/*
	 * CREATE FUNCTION smart.trimHkeyToLevel(level integer, hkey long varchar) returns varchar(32672)
LANGUAGE JAVA
deterministic 
external name 'org.wcs.smart.ca.datamodel.Category.trimHkeyToLevel'
PARAMETER STYLE JAVA
NO SQL 
RETURNS NULL ON NULL INPUT;
	 */
	public static String trimHkeyToLevel(int level, String hkey){
		if (hkey == null || hkey.length() == 0) return null;
		//all hkeys should end with '.'; if not return null
		if (hkey.charAt(hkey.length() -1) != '.'){
			return null;
		}
		if (level < 0) return null;
		
		int index = -1;
		for (int i = 0; i <= level; i ++){
			if (index == hkey.length() -1){
				return null;
			}
			index = hkey.indexOf('.', index+1);
			if (index < 0){
				return null;
			}
		}
		return hkey.substring(0, index+1);
	}
	
	/**
	 * Functions for supporting computing hkey length.
	 * This function is wrapped in the derby database.
	 * 
	 * @param hkey the hkey
	 * @return the length of the hkey
	 */
	/*
	 * CREATE FUNCTION smart.hkeyLength(hkey long varchar) returns integer
	 * LANGUAGE JAVA
	 * deterministic 
	 * external name 'org.wcs.smart.ca.datamodel.Category.hkeyLength'
	 * PARAMETER STYLE JAVA
	 * NO SQL 
	 * RETURNS NULL ON NULL INPUT;
	 */
	public static Integer hkeyLength(String hkey){
		if (hkey == null) return null;
		if (!hkey.endsWith(HkeyObject.HKEY_SEPERATOR)) return null;
		int count = 0;
		for (int i = 0; i < hkey.length(); i ++){
			if (hkey.charAt(i) == HkeyObject.HKEY_SEPERATOR.charAt(0)){
				count ++;
			}
		}
		return count - 1;
	}
	
	
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
	@OneToMany(fetch = FetchType.LAZY, mappedBy="parent", cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy("cat_order")
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
	 * The full key of the category.  This key includes
	 * the key of all the parents.
	 * <parent>.<parent>. ... .<mykey>
	 * 
	 * 
	 * @return the category heriarchical key
	 */
	@Column(name="hkey")
	public String getHkey(){
		return this.categoryHkey;
	}
	public void setHkey(String hkey){
		this.categoryHkey = hkey;
	}
	
	
	/**
	 * 
	 * @return parent category;  <code>null</code> if root node
	 */
	@ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.ALL})
	@JoinColumn(name="parent_category_uuid", referencedColumnName="uuid")
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
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.category", cascade={CascadeType.ALL}, orphanRemoval = false)
	@OrderBy("att_order")
	public List<CategoryAttribute> getAllAttributes(){
		return this.attributes;
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
	 * 
	 * @param attributes list of attribute associated directory 
	 * with category
	 */
	public void setAllAttributes(List<CategoryAttribute> attributes){
		this.attributes = attributes;
	}
	
	@Transient
	public List<CategoryAttribute> getAllActiveAttributes(){
		return this.getAllAttributes().stream().filter(f->f.getIsActive()).collect(Collectors.toList());
	}
	
	@Transient
	public List<CategoryAttribute> getRootAttributes(){
		return getAllAttributes().stream().filter(f->f.getIsRoot()).collect(Collectors.toList());
	}
	
	/**
	 * 
	 * @return all children categories; <code>null</code> if leaf node
	 */
	@Transient
	public List<Category> getActiveChildren(){
		return getChildren().stream().filter(f->f.getIsActive()).collect(Collectors.toList());
	}
	
	
	/**
	 * Updates the hkey of this object and
	 * children objects.
	 */
	@Transient
	public void updateHkey(){
		setHkey(computeHkey());
		if (getChildren() == null) return;
		getChildren().forEach(k->k.updateHkey());
	}
	
	
	/**
	 * Computes the hkey for the given category.
	 * 
	 * @return the hkey for this category.
	 */
	private String computeHkey(){
		if (parent == null){
			return this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
		}
		return parent.computeHkey() + this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
	}
	
		
	/**
	 * 
	 * @return the category name concatenated with
	 * all parent category names.
	 */
	@Transient
	public String getFullCategoryName(){
		return getFullCategoryName(false);
	}
	
	/**
	 * 
	 * @return the category name concatenated with
	 * all parent category names.
	 */
	@Transient
	public String getFullCategoryName(boolean rootfirst){
		if (parent == null){
			return getName();
		}else{
			if (rootfirst) {
				return parent.getFullCategoryName(rootfirst) + FULL_NAME_SEPARATOR + getName() ;

			}else {
				return getName() + FULL_NAME_SEPARATOR + parent.getFullCategoryName(rootfirst);
			}
		}
	}
	
	/**
	 * 
	 * @return the category name concatenated with
	 * all parent category names for a given language
	 */
	@Transient
	public String getFullCategoryName(Language lang){
		String l = findNameNull(lang);
		if (l == null){
			l = findName(ca.getDefaultLanguage());
		}
		if (parent == null){
			return l;
		}else{
			return l + FULL_NAME_SEPARATOR + parent.getFullCategoryName(lang);
		}
	}
	
	
	/**
	 * 
	 * @return <code>true</code> if this category or any of it's parents have attributes; <code>false</code> otherwise
	 */
	@Transient
	public boolean hasAttributes(){
		return getAllAttributes() != null && getAllAttributes().size() > 0;
	}
	
	@Transient
	public void accept(ICategoryVisitor visitor) {
		if (!visitor.visit(this)) return;
		if (getChildren() == null) return;
		for (Category kid : getChildren()){
			kid.accept(visitor);
		}
	}
	
	/**
	 * Adds the attribute to this category and all children category
	 * 
	 * @param attribute
	 */
	@Transient
	public CategoryAttribute addAttribute(Attribute attribute) {
		if (getAllAttributes() == null) setAllAttributes(new ArrayList<>());
		
		//do not add if this attribute already exists
		for (CategoryAttribute ca : getAllAttributes()) {
			if (ca.getAttribute().equals(attribute)) throw new IllegalStateException("Attribute already exists in category"); //$NON-NLS-1$
		}
		
		CategoryAttribute root = new CategoryAttribute(this, attribute);
		root.setOrder(getAllAttributes().size()+1);
		root.setIsRoot(true);
		root.setIsActive(true);
		getAllAttributes().add(root);
		
		List<Category> toProcess = new ArrayList<>(getChildren());
		while(!toProcess.isEmpty()) {
			Category kid = toProcess.remove(0);
			toProcess.addAll(kid.getChildren());
			if (kid.getAllAttributes() == null) kid.setAllAttributes(new ArrayList<>());
			
			//if already exists don't re-add it
			boolean found = false;
			for (CategoryAttribute caa : kid.getAllAttributes()) {
				if (caa.getAttribute().equals(attribute)) {
					//no longer root
					caa.setIsRoot(false);
					found = true;
					break;
				}
			}
			if (!found) {
				CategoryAttribute ca = new CategoryAttribute(kid, attribute);
				ca.setOrder(kid.getAllAttributes().size()+1);
				ca.setIsRoot(false);
				ca.setIsActive(true);
				kid.getAllAttributes().add(ca);
			}
			
		}
		return root;
	}
	
	/**
	 * Sets the active state for this attribute in this category
	 * and all children.
	 * @param attribute
	 */
	@Transient
	public void setAttributeActive(Attribute attribute, boolean isActive) {		
		List<Category> toProcess = new ArrayList<>();
		toProcess.add(this);
		while(!toProcess.isEmpty()) {
			Category kid = toProcess.remove(0);
			toProcess.addAll(kid.getChildren());
			
			CategoryAttribute ca = kid.findAttribute(attribute);
			if (ca == null) continue;
			if (!isActive || ca.getCategory().getIsActive()) {
				//if the category is disabled we don't wan to active attribute
				ca.setIsActive(isActive);
			}
		}
	}
	
	/**
	 * @returns true if this category contains the provided attribute.
	 */
	@Transient
	public boolean hasAttribute(Attribute attribute){
		return findAttribute(attribute) != null;
	}

	/**
	 * Searches the allAttributes array for the given attribute.
	 * 
	 * @param attribute
	 * @return the attribute or null if not found
	 */
	@Transient
	public CategoryAttribute findAttribute(Attribute attribute) {
		for (CategoryAttribute att : getAllAttributes()) {
			if (att.getAttribute().equals(attribute)) {
				return att;
			}
		}
		return null;
	}
	
	/**
	 * Creates a clones of the category.
	 * 
	 * @param newCa the new conservation area to associated with the cloned category
	 * @param parent the parent category
	 * @param clonedAttributes a list of cloned attributes to be used when associating attributes with a category
	 * @return cloned cateogry
	 */
	public Category clone(ConservationArea newCa, Category parent, List<Attribute> clonedAttributes, Collection<Icon> iconSet, String defaultLang){
		
		Category clone = new Category();
		clone.copyValues(this, newCa, defaultLang);
		
		clone.setCategoryOrder(this.getCategoryOrder());
		clone.setChildren(children);
		clone.setConservationArea(newCa);
		clone.setIsActive(this.getIsActive());
		clone.setIsMultiple(this.getIsMultiple());
		clone.setParent(parent);
		clone.updateHkey();
		clone.updateIcon(this, iconSet);
		clone.setAllAttributes(new ArrayList<>());
		
		if (this.getAllAttributes() != null){			
			for (CategoryAttribute attribute : this.getAllAttributes()){
				//find attribute
				Attribute newAttribute = null;
				for (Attribute clonedAttribute: clonedAttributes){
					if (clonedAttribute.getKeyId().equals(attribute.getAttribute().getKeyId())){
						newAttribute = clonedAttribute;
						break;
					}
				}
				
				if (newAttribute !=  null){
					clone.getAllAttributes().add(attribute.clone(clone, newAttribute));
				}
				
			}
		}
		
		if (this.getChildren() != null){
			clone.setChildren(new ArrayList<Category>());
			for (Category child : getChildren()){
				clone.getChildren().add(child.clone(newCa,clone, clonedAttributes, iconSet, defaultLang));
			}
		}
		
		//clone language labels
		
		return clone;
	}
}
