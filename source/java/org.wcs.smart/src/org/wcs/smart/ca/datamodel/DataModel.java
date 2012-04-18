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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Conservation area data model.
 * @author Emily
 * @since 1.0.0
 */
public class DataModel {


	private ConservationArea ca;	//the conservation area of the data model
	private List<Category> categories;	//the root categories for the data model
	private Set<Attribute> attributes; // all attributes associated with datamodel	
	
	private static List<Aggregation> aggregations; //set of valid aggregations
	
	
	/**
	 * Creates a new data for the associated conservation data
	 * with the given root categories.
	 * 
	 * @param ca
	 * @param rootCategories
	 */
	public DataModel(ConservationArea ca, List<Category> rootCategories, List<Attribute> attributes){
		this.ca = ca;
		this.categories = new ArrayList<Category>();
		this.categories.addAll(rootCategories);
		
		this.attributes = new HashSet<Attribute>();
		this.attributes.addAll(attributes);
	}
	
	private DataModel(){
		
	}
	
	/**
	 * 
	 * @return all valid aggregations
	 */
	public static List<Aggregation> getAggregations(){
		if (aggregations == null){
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				aggregations = s.createCriteria(Aggregation.class).addOrder(Order.asc("name")).list();
			}catch (Exception ex){
				SmartPlugIn.displayLog(null, "Cannot load aggregations from database.", ex);
				return null;
			}finally{
				if (s.getTransaction().isActive()){
					s.getTransaction().rollback();
				}
				s.close();
			}
		}
		return aggregations;
	}
	/**
	 * 
	 * @return conservation area associated with data model
	 */
	public ConservationArea getConservationArea(){
		return this.ca;
	}
	/**
	 * 
	 * @param ca conservation area associated with the data model
	 */
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	/**
	 * 
	 * @return list of root categories
	 */
	public List<Category> getCategories(){
		return this.categories;
	}
	
	/**
	 * 
	 * @return list of only active root categories
	 */
	public List<Category> getActiveCategories(){
		ArrayList<Category> tmp = new ArrayList<Category>();
		if (getCategories() != null){
			for (Iterator<Category> iterator = getCategories().iterator(); iterator.hasNext();) {
				Category cat = (Category) iterator.next();
				if (cat.getIsActive()){
					tmp.add(cat);
				}
			}
		}
		return tmp;
	}
	/**
	 * Adds a category to the list of root categores.
	 * 
	 * @param category
	 */
	public void addRootCategories(Category category){
		this.categories.add(category);
	}
	
	/**
	 * Adds the given attribute to the given category.
	 * 
	 * <p>
	 * If category is null, then it will be added to the attribute
	 * list but not any category.
	 * </p>
	 * 
	 * @param att attribute to add
	 * @param cat category to add attribute to; can be null
	 * @return the newly created {@link CategoryAttribute} association of
	 * null if no category provided
	 */
	public CategoryAttribute addAttribute(Attribute att, Category cat){
		attributes.add(att);
		if (cat != null){
			if (cat.getAttributes() == null){
				cat.setAttributes(new ArrayList<CategoryAttribute>());
			}
			for (CategoryAttribute catatt: cat.getAttributes()){
				if (catatt.getAttribute().equals(att)){
					//attribute already exists
					return null;
				}
			}
			CategoryAttribute ca = new CategoryAttribute();
			ca.setAttribute(att);
			ca.setCategory(cat);
			ca.setIsActive(true);
			ca.setOrder(cat.getAttributes().size());
			cat.getAttributes().add(ca);
		
			return ca;
		}
		return null;
		
	}
	
	/**
	 * Finds all categories associated with the provided attribute.
	 * 
	 * @param att
	 * @return
	 */
	public Set<CategoryAttribute> findAttribute(Attribute att){
		HashSet<CategoryAttribute> results = new HashSet<CategoryAttribute>();
		for (Category cat: getCategories()){
			searchCategory(cat, att, results);
		}
		return results;
	}
	/**
	 * Searches a category its children for all categories that are associated
	 * with the given attribute.
	 * 
	 * @param cat category to search
	 * @param att attribute to find
	 * @param results map to add results to
	 */
	private void searchCategory (Category cat, Attribute att, HashSet<CategoryAttribute> results){
		if (cat.getAttributes() != null){
			for (CategoryAttribute rel : cat.getAttributes()){
				if (rel.getAttribute().equals(att)){
					results.add(rel);
				}
			}
		}
		if (cat.getChildren() != null){
			for (Category child : cat.getChildren()){
				searchCategory(child, att, results);
			}
		}
	}
	/**
	 * Moves an attribute to a new position in the sibling list.
	 * 
	 * @param toMove the attribute to move
	 * @param toMoveTo the attribute to move it to
	 * @param moveBefore if it should be moved before or after the <b>toMove</b> parameter
	 */
	public void moveAttributePosition(CategoryAttribute toMove, CategoryAttribute toMoveTo, boolean moveBefore){
		if (toMove.equals(toMoveTo)){
			return;
		}
		if (toMove.getCategory() != null){
			toMove.getCategory().getAttributes().remove(toMove);
			if (moveBefore){
				toMove.getCategory().getAttributes().add(toMove.getCategory().getAttributes().indexOf(toMoveTo), toMove);
			}else{
				toMove.getCategory().getAttributes().add(toMove.getCategory().getAttributes().indexOf(toMoveTo) + 1, toMove);
			}
			
			for (int i = 0; i < toMove.getCategory().getAttributes().size(); i ++){
				toMove.getCategory().getAttributes().get(i).setOrder(i);
			}
		}
	}
	
	/**
	 * Moves an category to a new position in the sibling list.
	 * 
	 * @param toMove the category to move
	 * @param toMoveTo the category to move it to
	 * @param moveBefore if it should be moved before or after the <b>toMove</b> parameter
	 */
	public void moveCategoryPosition(Category toMove, Category toMoveTo, boolean moveBefore){
		if (toMove.equals(toMoveTo)){
			return;
		}
		List<Category> list = null;
		if (toMove.getParent() != null){
			list = toMove.getParent().getChildren();
		}else{
			list = categories;
		}
		
		list.remove(toMove);
		if (moveBefore){
			list.add(list.indexOf(toMoveTo), toMove);
		}else{
			list.add(list.indexOf(toMoveTo)+1, toMove);
		}
			
		for (int i = 0; i < list.size(); i ++){
			list.get(i).setCategoryOrder(i);
		}
	}
	
	/**
	 * Disables or enables a given category/attribute association.
	 * <p>
	 * If enabling all parent categories will also be enabled.
	 * </p>
	 * @param cat @{link CategoryAttribute} association to disable/enable 
	 * @param enabled <code>true</code> to enable, <code>false</code> to disable.
	 */
	public void disableAttribute(CategoryAttribute cat, boolean enabled){
		cat.setIsActive(enabled);
		if (enabled){
			//enable the parent categories as well
			disableCategory(cat.getCategory(), enabled);
		}
	}
	/**
	 * Disables or enables a given category.
	 * <p>
	 * If disabling then all child categories and attributes will be disabled.
	 * </p>
	 * <p>
	 * If enabling then all parent categories will also be enabled.
	 * </p>
	 * @param cat @{link Category} to enable
	 * @param enabled <code>true</code> to enable, <code>false</code> to disable
	 */
	public void disableCategory(Category cat, boolean enabled){
		if (!enabled){
			//disable category and all children
		
			cat.setIsActive(enabled);
			if (cat.getChildren() != null){
				for(Category child: cat.getChildren()){
					disableCategory(child, enabled);
				}
			}
			if (cat.getAttributes()!=null){
				for (CategoryAttribute att: cat.getAttributes()){
					disableAttribute(att, enabled);
				}
			}
		}else{
			//enable category and parent
			if (cat.getIsActive()){
				return ;
			}
			cat.setIsActive(enabled);
			if (cat.getParent() != null){
				disableCategory(cat.getParent(), enabled);
			}
			//enable all attributes
			if (cat.getAttributes()!=null){
				for (CategoryAttribute att: cat.getAttributes()){
					disableAttribute(att, enabled);
				}
			}
		}
	}
	
	/**
	 * 
	 * @return all attributes associated with any category
	 * in the data model.
	 */
	public Set<Attribute> getAttributes(){
		return this.attributes;
	}
	
	/**
	 * Generates a key for a dm object from a name.
	 * 
	 * @param value the name provided
	 * @param otherValues list of other dm objects that the key must be different from
	 * 
	 * @return valid key
	 */
	public static String generateKey (String value, Collection<? extends DmObject> otherValues){
		String raw = value.toLowerCase().replaceAll("[^a-z0-9_]", "");
		if (raw.isEmpty()){
			raw = "object";
		}
	
		int count = 0;
		String key = raw;
		if (raw.length() > DmObject.MAX_KEY_LENGTH){
			key = raw.substring(0, DmObject.MAX_KEY_LENGTH);
		}

		while(checkKeyExists(key, otherValues)){
			count ++;
			String cnt = String.valueOf(count);
			if (raw.length() + cnt.length() > DmObject.MAX_KEY_LENGTH){
				key = raw.substring(0, DmObject.MAX_KEY_LENGTH - cnt.length() ) + cnt;
			}else{
				key = raw + String.valueOf(count);
			}
			
		}
		return key;
	}
	/*
	 * determines if a key exists in 
	 * a set of objects
	 */
	private static boolean checkKeyExists(String key, Collection<? extends DmObject> otherValues){
		if (otherValues == null){
			return false;
		}
		for (DmObject other : otherValues){
			if (key.equals(other.getKeyId())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Validates a data model object key.
	 * <p>Keys must not be empty, less than DmObject.MAX_KEY_LENGTH characters,
	 * and different from their siblings.</p>
	 * 
	 * @param key the key to validate.
	 * @param otherValues set of {@link DmObject} the key value must be different from
	 * @return <code>null</code> if the key is valid otherwise a string description of the error
	 */
	public static String validateKey(String key, Collection<? extends DmObject> otherValues){
		if (key == null || key.isEmpty()){
			return "The key cannot be empty.";
		}
		if (key.length() > DmObject.MAX_KEY_LENGTH ){
			return "Key must be less than " +  DmObject.MAX_KEY_LENGTH + " characters.";
		}
		if (key.matches(".*[^a-z0-9_].*")){
			return "The can only contain lower case characters a-z, underscore (_), and digits 0-9.";
		}
		if (checkKeyExists(key, otherValues)){
			return "The key is not unique.";
		}
		return null;
	}
	
	
	/**
	 * Saves a datamodel to the database using the given session
	 * 
	 * @param session database connection
	 */
	public void save(Session session){
		for (Attribute att: attributes){
			session.saveOrUpdate(att);
		}
		for (Category c: categories){
			session.saveOrUpdate(c);
		}
	}
	
	
	/**
	 * Creates a copy of the current data model for the
	 * given conservation area.
	 * 
	 * @param newCa the new conservation area 
	 * @param defaultLang may be null otherwise the language from the
	 * original data model labels to use for the labels of the
	 * default language of the new conservation area
	 * @return the cloned data model
	 */
	public DataModel clone(ConservationArea newCa, String defaultLang){
		DataModel clone = new DataModel();
		
		clone.setConservationArea(newCa);
		
		//attributes
		if (this.getAttributes() != null){
			clone.attributes = new HashSet<Attribute>();
			for (Attribute att: this.getAttributes()){
				clone.attributes.add(att.clone(newCa,defaultLang));
			}
		}
		
		//categories
		clone.categories = new ArrayList<Category>();
		for (Category cat: this.getCategories()){
			clone.categories.add(cat.clone(newCa, null, clone.attributes, defaultLang));
		}
		
		return clone;
	}
	
}
