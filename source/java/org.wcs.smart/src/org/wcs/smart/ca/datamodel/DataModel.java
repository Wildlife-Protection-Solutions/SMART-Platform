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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Image;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;


/**
 * Extension of a simple data model to support Desktop specific features.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModel extends SimpleDataModel {
		
	private static List<Aggregation> aggregations; //set of valid aggregations
	
	
	/**
	 * Creates a new data for the associated conservation data
	 * with the given root categories.
	 * 
	 * @param ca
	 * @param rootCategories
	 */
	public DataModel(ConservationArea ca, List<Category> rootCategories, List<Attribute> attributes){
		super(ca, rootCategories, attributes);
	}
	
	private DataModel(){
		super(null, Collections.emptyList(), Collections.emptyList());
	}
	
	public static Aggregation getAggregation(String key){
		for (Aggregation agg : getAggregations()) {
			if (agg.getName().equals(key)) {
				return agg;
			}
		}
		return null;
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
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try(Session s = HibernateManager.openSession()){
						s.beginTransaction();
						
						CriteriaQuery<Aggregation> c = s.getCriteriaBuilder().createQuery(Aggregation.class);
						Root<Aggregation> from = c.from(Aggregation.class);
						c.select(from).distinct(true);
						c.orderBy(s.getCriteriaBuilder().asc(from.get("name"))); //$NON-NLS-1$
						
						aggregations = s.createQuery(c).getResultList();

						s.getTransaction().rollback();
					}catch (Exception ex){
						SmartPlugIn.displayLog(Messages.DataModel_Error_LoadAggregations, ex);
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
	 * Validates a data model object name
	 * <p>Names must not be empty, less than DmObject.MAX_NAME_LENGTH characters</p>
	 * 
	 * 
	 * @param name the name to validate.
	 * @param l the language associated with the name
	 * @return <code>null</code> if the name is valid otherwise a string description of the error
	 */
	public static String validateName(String name, Language l){
		return SimpleDataModel.validateName(name,  l,  Locale.getDefault());
	}
	
	/**
	 * Saves a datamodel to the database using the given session.
	 * <p>This method performs the save inside a transaction block
	 * so calling this method much be done outside of a
	 * transaction block.
	 * </p>
	 * 
	 * @param session database connection
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor. Accepts null, indicating that no progress should be
	 * @throws HibernateException if changes cannot be saved
	 */
	public void save(Session session, IProgressMonitor monitor){
		session.beginTransaction();
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DataModel_Progress_SaveDm, attributes.size() + categories.size());
		try {
			for (Attribute att : attributes) {
				if (att.getIcon() != null && att.getIcon().getUuid() == null) session.persist(att.getIcon());
				if (att.getAttributeList() != null) att.getAttributeList().forEach(e->{
					if (e.getIcon() != null && e.getIcon().getUuid() == null) session.persist(e.getIcon());
				});
				
				if (att.getTree() != null) {
					processAttributeTree(att, (node)->{
						if (node.getIcon() != null && node.getIcon().getUuid() == null) {
							node.getIcon().setConservationArea(node.getAttribute().getConservationArea());
							session.persist(node.getIcon());
						}
					});
				}
				progress.subTask(Messages.DataModel_Progress_SaveAttribute + att.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				session.persist(att);
				session.flush();
				session.clear();
				progress.worked(1);
			}

			processCategories(this, (node)->{
				if (node.getIcon() != null && node.getIcon().getUuid() == null) {
					node.getIcon().setConservationArea(node.getConservationArea());
					HibernateManager.saveOrMerge(session,  node.getIcon());
					session.flush();
				}
			});
			session.flush();
			
			for (Category c : categories) {
				progress.subTask(Messages.DataModel_Progress_SaveCategory + c.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				session.persist(c);
				session.flush();
				session.clear();
				progress.worked(1);
			}
			
			DataModelManager.INSTANCE.updateLastModified(session);
			
			session.getTransaction().commit();
		} catch (HibernateException ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		DataModelManager.INSTANCE.fireChangeListeners();
		
	}

	
	/**
	 * Creates a copy of the current data model for the
	 * given conservation area.
	 * 
	 * @param newCa the new conservation area 
	 * @param defaultLang may be null otherwise the language from the
	 * original data model labels to use for the labels of the
	 * default language of the new conservation area
	 * @param icons set of icons associated with the new conservation area - can be null
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor. Accepts null, indicating that no progress should be
	 * @return the cloned data model
	 */
	public DataModel clone(ConservationArea newCa, String defaultLang, Collection<Icon> icons, IProgressMonitor monitor){
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DataModel_ProgressLabel, 2);
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
		progress.subTask(Messages.DataModel_CloneAttributes1);
		if (this.getAttributes() != null){
			clone.attributes = new ArrayList<Attribute>();
			for (Attribute att: this.getAttributes()){
				progress.subTask(Messages.DataModel_CloneAttributes2 + att.findName(ll));
				Attribute attributeClone = att.clone(newCa, icons, defaultLang);
				clone.attributes.add(attributeClone);		
			}
		}
		
		//categories
		clone.categories = new ArrayList<Category>();
		progress.worked(1);
		progress.subTask(Messages.DataModel_CloneCategories);
		for (Category cat: this.getCategories()){
			progress.subTask(Messages.DataModel_CloneSubCategories + cat.findName(ll));
			clone.categories.add(cat.clone(newCa, null, clone.attributes, icons, defaultLang));
		}
		progress.worked(1);
		return clone;
	}
	
	
	/**
	 * 
	 * @param type
	 * @return the image associated with a given attribute type
	 */
	public static Image getAttributeImage(AttributeType type){
		if (type == Attribute.AttributeType.BOOLEAN){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_BOOLEAN_ICON);
		}else if (type == Attribute.AttributeType.TEXT){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_TEXT_ICON);
		}else if (type == Attribute.AttributeType.LIST){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_LIST_ICON);
		}else if (type == Attribute.AttributeType.MLIST){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_MULTI_LIST_ICON);
		}else if (type == Attribute.AttributeType.NUMERIC){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
		}else if (type == Attribute.AttributeType.TREE){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_TREE_ICON);
		}else if (type == Attribute.AttributeType.DATE){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_DATE_ICON);
		}
		return null;
	}
	
	public static void processCategories(DataModel dm, Consumer<Category> consumer) {
		processCategories(dm.getCategories(), consumer);
	}
	
	public static void processCategories(SimpleDataModel dm, Consumer<Category> consumer) {
		processCategories(dm.getCategories(), consumer);
	}
	
	public static void processCategories(List<Category> rootCategories, Consumer<Category> consumer) {
		for (Category c : rootCategories) {
			c.accept(e->{
				consumer.accept(e);
				return true;
			});
		}
	}
	
	public static void processAttributeTree(Attribute attribute, Consumer<AttributeTreeNode> consumer) {
		for (AttributeTreeNode node : attribute.getTree()) {
			node.accept(e->{
				consumer.accept(e);
				return true;
			});
		}
	}
}
