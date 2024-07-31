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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
	public DataModel(ConservationArea ca, 
			List<Category> rootCategories, 
			List<Attribute> attributes){
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
		if (getCategories() == null) return Collections.emptyList();		
		return getCategories().stream().filter(f->f.getIsActive()).collect(Collectors.toList());		
	}
	
	/**
	 * Adds a category to the list of root categories.
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
	 * @return the newly created {@link CategoryAttribute} association or
	 * null if no category provided. The root CategoryAttribute is returned, not any of the children
	 * objects created.
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
		if (cat == null) return null ;
		return cat.addAttribute(att);	
	}
	
	/**
	 * Finds all categories associated with this attribute
	 * as the root attribute
	 * 
	 * @param att
	 * @return
	 */
	public Set<CategoryAttribute> findRootAttribute(Attribute att){
		HashSet<CategoryAttribute> results = new HashSet<CategoryAttribute>();
		List<Category> toProcess = new ArrayList<>(getCategories());
		while(!toProcess.isEmpty()) {
			Category item = toProcess.remove(0);
			if (item.getChildren() != null) toProcess.addAll(item.getChildren());
			for (CategoryAttribute ca : item.getRootAttributes()) {
				if (ca.getIsRoot() && ca.getAttribute().equals(att)) results.add(ca);
			}
		}
		return results;
	}
//	/**
//	 * Searches a category and its children for all categories that are associated
//	 * with the given attribute.
//	 * 
//	 * @param cat category to search
//	 * @param att attribute to find
//	 * @param results map to add results to
//	 */
//	private void searchCategory (Category cat, Attribute att, HashSet<CategoryAttribute> results){
//		if (cat.getAllAttributes() != null){
//			for (CategoryAttribute rel : cat.getAttributes()){
//				if (rel.getAttribute().equals(att)){
//					results.add(rel);
//				}
//			}
//		}
//		if (cat.getChildren() != null){
//			for (Category child : cat.getChildren()){
//				searchCategory(child, att, results);
//			}
//		}
//	}
	
	
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
		cat.getCategory().setAttributeActive(cat.getAttribute(), enabled);
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
			if (cat.getAllAttributes()!=null){
				for (CategoryAttribute att: cat.getAllAttributes()){
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
			//enable all root attributes
			for (CategoryAttribute ca : cat.getAllAttributes()) {
				if (ca.getIsRoot()) {
					disableAttribute(ca, enabled);
				}else {
					//set the attribute enabled/disabled value to the value of the root attribute
					Category parent = ca.getCategory().getParent();
					while(parent != null) {
					
						for (CategoryAttribute ca2 : parent.getRootAttributes()) {
							if (ca.getAttribute().equals(ca2.getAttribute())) {
								ca.setIsActive(ca2.getIsActive());
								parent = null;
								break;
							}
						}
						if (parent != null) parent = parent.getParent();
					}
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
	 * @throws Exception if changes cannot be saved
	 */
	public void save(Session session, IProgressMonitor monitor) throws Exception{
		session.beginTransaction();
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DataModel_Progress_SaveDm, attributes.size() + categories.size());
		try {
			progress.subTask(Messages.DataModel_validationtask);
			validateAndFixCategoryAttributes();
			
			for (Attribute att : attributes) {
				progress.subTask(Messages.DataModel_Progress_SaveAttribute + att.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));

				if (att.getIcon() != null && att.getIcon().getUuid() == null) {
					session.persist(att.getIcon());
				}
				if (att.getAttributeList() != null) att.getAttributeList().forEach(e->{
					if (e.getIcon() != null && e.getIcon().getUuid() == null) {
						session.persist(e.getIcon());
					}
				});
				
				if (att.getTree() != null) {
					processAttributeTree(att, (node)->{
						if (node.getIcon() != null && node.getIcon().getUuid() == null) {
							node.getIcon().setConservationArea(node.getAttribute().getConservationArea());
							session.persist(node.getIcon());
						}
					});
				}
				session.persist(att);
				session.flush();
				progress.worked(1);
			}
			session.flush();

			progress.subTask(Messages.DataModel_categoriestask);
			processCategories(this, (node)->{
				if (node.getIcon() != null && node.getIcon().getUuid() == null) {
					node.getIcon().setConservationArea(node.getConservationArea());
					HibernateManager.saveOrMerge(session,  node.getIcon());
				}
			});
			session.flush();
			
			for (Category c : categories) {
				progress.subTask(Messages.DataModel_Progress_SaveCategory + c.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
				session.persist(c);
				session.flush();
				progress.worked(1);
			}
			DataModelManager.INSTANCE.updateLastModified(session);
			session.clear();
			
			session.getTransaction().commit();
		} catch (HibernateException ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		DataModelManager.INSTANCE.fireChangeListeners();
		
	}

	//a part of the changes for 8.1.0 which allows ordering
	//of all attributes (parent and current) for a category
	//this class ensures that the appropriate database
	//structures exists 
	public void validateAndFixCategoryAttributes() throws Exception{
		List<Category> toProcess = new ArrayList<>(this.getCategories());
		while(!toProcess.isEmpty()) {
			Category c = toProcess.remove(0);
			toProcess.addAll(c.getChildren());
			validateAndFixCategory(c);
		}
		//each root attribute must exist in all children
		//eat non-root attribute must exist as a root attribute in a parent
	}
	private void validateAndFixCategory(Category c) throws Exception{
		for(CategoryAttribute ca: c.getAllAttributes()) {
			if (ca.getIsRoot()) {
				List<Category> toProcess = new ArrayList<>();
				toProcess.addAll(c.getChildren());
				while(!toProcess.isEmpty()) {
					Category kid = toProcess.remove(0);
					toProcess.addAll(kid.getChildren());
					CategoryAttribute temp = kid.findAttribute(ca.getAttribute());
					if (temp == null) {
						throw new Exception(MessageFormat.format(Messages.DataModel_CategoryConfigError, c.getKeyId(), ca.getAttribute().getKeyId(), kid.getKeyId()));
					}
					//fix root setting
					if (temp.getIsRoot()) {
						SmartPlugIn.log(MessageFormat.format("Child category attribute {0} {1} is flagged as root, setting to non-root.", kid.getKeyId(), temp.getAttribute().getKeyId()), null); //$NON-NLS-1$
						temp.setIsRoot(false);
					}
					//if the category is active then the active state should match the root
					//if the category is disabled the active state should be disabled
					if (kid.getIsActive()) {
						if (temp.getIsActive() != ca.getIsActive()) {
							SmartPlugIn.log(MessageFormat.format("Child category attribute {0} {1} does not match root attribute active flag, updating active flag.", kid.getKeyId(), temp.getAttribute().getKeyId()), null); //$NON-NLS-1$
							temp.setIsActive(ca.getIsActive());
						}
					}else {
						if (temp.getIsActive()) {
							SmartPlugIn.log(MessageFormat.format("Category attribute {0} {1}; category is disabled, attribute is not. Updating attributing isactive statue", kid.getKeyId(), temp.getAttribute().getKeyId()), null); //$NON-NLS-1$
							temp.setIsActive(false);
						}
					}
				}
			}else {
				//not root
				boolean isfound = false;
				Category parent = c.getParent();
				while(parent != null) {
					CategoryAttribute next = parent.findAttribute(ca.getAttribute());
					if (next.getIsRoot()) {
						isfound = true;
						break;
					}
					parent = parent.getParent();
				}
				if (!isfound) {
					throw new Exception(Messages.DataModel_CategoryConfigError2);

				}
			}
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
		switch(type) {
		case BOOLEAN:return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_BOOLEAN_ICON);
		case DATE: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_DATE_ICON);
		case TIME: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_TIME_ICON);
		case LINE: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_LINE_ICON);
		case LIST: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_LIST_ICON);
		case MLIST: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_MULTI_LIST_ICON);
		case NUMERIC: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
		case POLYGON: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_POLYGON_ICON);
		case TEXT: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_TEXT_ICON);
		case TREE: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_TREE_ICON);
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
				consumer.accept((AttributeTreeNode)e);
				return true;
			});
		}
	}
}
