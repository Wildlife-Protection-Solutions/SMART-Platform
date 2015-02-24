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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;


/**
 * Conservation area data model.
 * @author Emily
 * @since 1.0.0
 */
public class DataModel {

	public static final String HKEY_SEPERATOR = "."; //$NON-NLS-1$
	
	private ConservationArea ca;	//the conservation area of the data model
	private List<Category> categories;	//the root categories for the data model
	private List<Attribute> attributes; // all attributes associated with datamodel	
	
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
		
		this.attributes = new ArrayList<Attribute>();
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
			//done in a job so it has it's own database connection
			//otherwise i might close an existing connection when it should be.
			Job loadAttributesJob = new Job(Messages.DataModel_LoadAttribute_JobName) {
				
				@SuppressWarnings("unchecked")
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
						aggregations = s.createCriteria(Aggregation.class).addOrder(Order.asc("name")).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list(); //$NON-NLS-1$
						s.getTransaction().rollback();
					}catch (Exception ex){
						SmartPlugIn.displayLog(Messages.DataModel_Error_LoadAggregations, ex);
					}finally{
						s.close();
					}
					return Status.OK_STATUS;
				}
			};
				
			loadAttributesJob.schedule();
			try{
				loadAttributesJob.join();
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.DataModel_Error_LoadAggregations, ex);
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
	 * Adds the given attribute which is not yet
	 * in the datamodel to the datamodel and the given category.
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
	public CategoryAttribute addNewAttribute(Attribute att, Category cat) throws Exception{
		attributes.add(att);
		return addExistingAttribute(att, cat);
		
	}
	
	/**
	 * Adds an attribute already defined in the datamodel to the 
	 * given category
	 * 
	 * @param att
	 * @param cat
	 * @return
	 */
	public CategoryAttribute addExistingAttribute(Attribute att, Category cat ) throws Exception{
		if (cat != null) {
			if (cat.getAttributes() == null) {
				cat.setAttributes(new ArrayList<CategoryAttribute>());
			}
			for (CategoryAttribute catatt : cat.getAttributes()) {
				if (catatt.getAttribute().equals(att)) {
					throw new Exception(MessageFormat.format(Messages.DataModel_AttributeAlreadyExists, new Object[]{att.getName(), cat.getName()}));
				}
			}
			//ensure category does not exist in any child categories
			List<Category> toCheck = new ArrayList<Category>();
			toCheck.addAll(cat.getChildren());
			while(toCheck.size() > 0){
				Category c = toCheck.remove(0);
				toCheck.addAll(c.getChildren());
				if (c.getAttributes() != null){
					for (CategoryAttribute ca : c.getAttributes()){
						if (ca.getAttribute().equals(att)){
							throw new Exception(MessageFormat.format(Messages.DataModel_AttributeAlreadyExistsChild, new Object[]{c.getName(), att.getName()}));
						}
					}
				}
			}
			//ensure category does not exist in parent category
			Category c = cat.getParent();
			while(c != null){
				if (c.getAttributes() != null){
					for(CategoryAttribute ca : c.getAttributes()){
						if (ca.getAttribute().equals(att)){
							throw new Exception(MessageFormat.format(Messages.DataModel_AttributeAlreadyExistsParent, new Object[]{c.getName(), att.getName()}));
						}
					}
				}
				c = c.getParent();
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
	public List<Attribute> getAttributes(){
		return this.attributes;
	}
	
	/**
	 * Validates a data model object name
	 * <p>Names must not be empty, less than DmObject.MAX_NAME_LENGTH characters</p>
	 * 
	 * 
	 * @param name the name to validate.
	 * @param l the language associated with the name
	 * @return <code>null</code> if the name is valid otherwise a string description of the error
	 */
	public static String validateName(String name, Language l){
		if (!SmartUtils.isSimpleString(name.trim(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, DmObject.MAX_NAME_LENGTH,0)){
			return MessageFormat.format(
					Messages.NameKeyComposite_Error_InvalidName,
					new Object[]{l.getDisplayName(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc});

		}
		if (l.isDefault() && name.trim().length() == 0){
			return Messages.DataModel_NameRequired;
		}
		
		return null;
	}
	
	/**
	 * Saves a datamodel to the database using the given session.
	 * <p>This method performs the save inside a transaction block
	 * so calling this method much be done outside of a
	 * transaction block.
	 * </p>
	 * 
	 * @param session database connection
	 * 
	 * @throws HibernateException if changes cannot be saved
	 */
	public void save(Session session, IProgressMonitor m){
		session.beginTransaction();
		m.beginTask(Messages.DataModel_Progress_SaveDm, attributes.size() + categories.size());
		try {
			for (Attribute att : attributes) {
				m.subTask(Messages.DataModel_Progress_SaveAttribute + att.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				session.save(att);
				session.flush();
				session.clear();
				m.internalWorked(1);
			}

			for (Category c : categories) {
				m.subTask(Messages.DataModel_Progress_SaveCategory + c.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				session.save(c);
				session.flush();
				session.clear();
				m.internalWorked(1);
			}
			m.done();
			session.getTransaction().commit();
		} catch (HibernateException ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		DataModelManager.getInstance().fireChangeListeners();
		
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
	public DataModel clone(ConservationArea newCa, String defaultLang, IProgressMonitor monitor){
		DataModel clone = new DataModel();
		
		clone.setConservationArea(newCa);
		Language ll = ca.getDefaultLanguage();
		if (SmartDB.getCurrentConservationArea() != null){
			for (Language l : ca.getLanguages()){
				if (l.getCode().equals(SmartDB.getCurrentLanguage().getCode())){
					ll = l;
					break;
				}
			}
		}
		
		//attributes
		monitor.beginTask(Messages.DataModel_ProgressLabel, 2);
		monitor.subTask(Messages.DataModel_CloneAttributes1);
		if (this.getAttributes() != null){
			clone.attributes = new ArrayList<Attribute>();
			for (Attribute att: this.getAttributes()){
				monitor.subTask(Messages.DataModel_CloneAttributes2 + att.findName(ll));
				clone.attributes.add(att.clone(newCa,defaultLang));
			}
		}
		
		//categories
		clone.categories = new ArrayList<Category>();
		monitor.worked(1);
		monitor.subTask(Messages.DataModel_CloneCategories);
		for (Category cat: this.getCategories()){
			monitor.subTask(Messages.DataModel_CloneSubCategories + cat.findName(ll));
			clone.categories.add(cat.clone(newCa, null, clone.attributes, defaultLang));
		}
		monitor.worked(1);
		monitor.done();
		return clone;
	}
	
}
