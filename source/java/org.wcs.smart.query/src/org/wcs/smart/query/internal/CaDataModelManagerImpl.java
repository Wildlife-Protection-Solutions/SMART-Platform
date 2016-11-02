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
package org.wcs.smart.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
/**
 * Data model manager for when users are only performing 
 * analysis on a single conservation area.
 * @author Emily
 *
 */
public class CaDataModelManagerImpl implements IDataModelManager {

	private volatile DataModel dm = null;
	private Integer dmDepth = null;
	private ConservationArea conservationArea = null;
	
	public CaDataModelManagerImpl(){
		this(SmartDB.getCurrentConservationArea());
	}
	
	public CaDataModelManagerImpl(ConservationArea ca){
		if (ca.getIsCcaa()){
			throw new IllegalStateException("Cannot use the CaDataModelManager for multiple conservation area analysis."); //$NON-NLS-1$
		}
		this.conservationArea = ca;
	}
	
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		synchronized (this) {
			dmDepth = null;
			dm = null;	
		}
	}
	
	/**
	 * 
	 * <p>This will block until the data model is loaded</p>
	 * @return the data model for querying
	 */
	@Override
	public DataModel getDataModel(){
		if (dm == null){
			synchronized (this) {
				if (dm == null){
					Job job = loadDataModelJob;
					job.schedule();
					try{
						//wait for the current job to finish
						job.join();
					}catch (Exception ex){
						QueryPlugIn.log(ex.getMessage(), ex);
					}
				}
			}
		}
		return this.dm;
	}
	
	
	/**
	 * Determines the active attribute list items for the given
	 * attribute. 
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<AttributeListItem> getActiveAttributeListItems(Attribute attribute, Session session){
		List<AttributeListItem> items = session
					.createCriteria(AttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute)) //$NON-NLS-1$
					.add(Restrictions.eq("isActive", true)) //$NON-NLS-1$
					.list();
			return items;
		
	}
	
	/**
	 * Determines all active attribute list items for the given
	 * attribute. 
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@Override
	public List<AttributeListItem> getAttributeListItems(Attribute attribute, Session session){
		return attribute.getActiveListItems();		
	}
	
	/**
	 * Determines the active attribute tree items for the given
	 * attribute. 
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	@Override
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(Attribute attribute, Session session){
		attribute = (Attribute) session.load(Attribute.class, attribute.getUuid());
		if (attribute.getActiveTreeNodes() != null){
			for (AttributeTreeNode node : attribute.getActiveTreeNodes()){
				visitTreeNode(node);
			}
		}
		return attribute.getActiveTreeNodes();
	}
	
	private void visitTreeNode(AttributeTreeNode parent){
		if (parent.getActiveChildren() != null){
			for (AttributeTreeNode child: parent.getActiveChildren()){
				visitTreeNode(child);
			}
		}
	}
	
	/**
	 * Returns the attribute with the given key
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	@Override
	public Attribute getAttribute(Session session, String attributeKey){
		Query q = session.createQuery("From Attribute where conservationArea.uuid = :ca and keyid = :key"); //$NON-NLS-1$
		q.setParameter("ca", conservationArea.getUuid()); //$NON-NLS-1$
		q.setParameter("key", attributeKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<Attribute> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
	}

	
	/**
	 * Returns the attribute with the given key
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	@Override
	public Attribute getAttribute(Session session, Attribute attribute){
		return attribute;
	}
	
	/**
	 * 
	 * Gets all the attribute tree nodes at 
	 * a given level in the data tree.    
	 * 
	 * @param session
	 * @param level
	 * @param active if only active tree nodes should be loaded; if false all nodes will be returned
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session, Attribute attribute, int level, boolean active){
		String query = "FROM AttributeTreeNode WHERE attribute_uuid =:uuid AND smart.hkeyLength(hkey) = :level"; //$NON-NLS-1$
		if (active){
			query += " and isActive = :active"; //$NON-NLS-1$ 
		}
		
		Query q = session.createQuery(query);
		q.setParameter("uuid", attribute.getUuid()); //$NON-NLS-1$
		q.setParameter("level", level); //$NON-NLS-1$
		if (active) q.setParameter("active", active); //$NON-NLS-1$
		List<AttributeTreeNode> nodes = q.list();
		return nodes;
	}
	
	
	/**
	 * Loads the category for the given category key 
	 * @param session
	 * @param categoryKey
	 * @return category object or <code>null</code> if not loaded
	 */
	@Override
	public Category getCategory(Session session, String categoryKey){
		Query q = session.createQuery("From Category where conservationArea = :ca and hkey = :key"); //$NON-NLS-1$
		q.setParameter("ca", conservationArea); //$NON-NLS-1$
		q.setParameter("key", categoryKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<Category> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
	}
	
	/**
	 * Finds all attributes associated with the given category
	 * hkey or parent key.
	 * 
	 * @param categoryHkey
	 * @return
	 */
	@Override
	public Collection<Attribute> getAttributes(Session session, String categoryHkey){
		List<Attribute> attributes = new ArrayList<Attribute>();
		Category c = getCategory(session, categoryHkey);
		while (c != null){
			for(CategoryAttribute a : c.getAttributes()){
				attributes.add(a.getAttribute());
			}
			c = c.getParent();
		}
		return attributes;
	}
	
	/**
	 * 
	 * Gets all the categories at a given level in the data tree.
	 * @param session
	 * @param level
	 * @return
	 */
	@Override
	public List<Category> getCategories(Session session, int level){
		String query = "FROM Category WHERE conservationArea = :ca AND smart.hkeyLength(hkey) = :level"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameter("ca", conservationArea); //$NON-NLS-1$
		q.setParameter("level", level); //$NON-NLS-1$
		
		List<Category> cats = q.list();
		return cats;
	}

	
	/**
	 * Loads an attribute list item for the given key and the
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeListItem getAttributeListItem(Session session, String attributeKey, String attributeListItem){
		Query q = session.createQuery(" SELECT ali From AttributeListItem ali join ali.attribute as a where a.conservationArea = :ca and ali.keyId = :key and a.keyId = :attributeKey"); //$NON-NLS-1$
		q.setParameter("ca", conservationArea); //$NON-NLS-1$
		q.setParameter("key", attributeListItem); //$NON-NLS-1$
		q.setParameter("attributeKey", attributeKey); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<AttributeListItem> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}	
	}
	
	/**
	 * Loads an attribute tree not item item for the given hkey and the
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeTreeNode getAttributeTreeNode(Session session, String attributeKey, String attributeTreeHKey){
		Query q = session.createQuery(" SELECT ali From AttributeTreeNode ali join ali.attribute as a where a.conservationArea = :ca and ali.hkey = :key and a.keyId = :attribute"); //$NON-NLS-1$
		q.setParameter("ca", conservationArea); //$NON-NLS-1$
		q.setParameter("key", attributeTreeHKey); //$NON-NLS-1$
		q.setParameter("attribute", attributeKey); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<AttributeTreeNode> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
		
	}
		
	/**
	 * Returns the full category label for a category with the
	 * given uuid.
	 * 
	 * @param session
	 * @param categoryUuid
	 * @return
	 */
	@Override
	public String[] getFullCategoryLabel(Session session, UUID categoryUuid){
		Category category = (Category) session.load(Category.class, categoryUuid);
		ArrayList<String> values = new ArrayList<String>();
		values.add(category.getName());
		Category parent = category.getParent();
		while(parent != null){
			values.add(parent.getName());
			parent = parent.getParent();
		}
		Collections.reverse(values);
		return values.toArray(new String[values.size()]);
	}
	
	/**
	 * @return the label to use for the given 
	 * attribute like item 
	 */
	@Override
	public String getAttributeListItemLabel(Session session, UUID cauuid, UUID keyuuid){
		return getName(keyuuid, session);
	}
	
	/**
	 * @return the label to use for the given attribute tree node
	 */
	@Override
	public String getAttributeTreeNodeLabel(Session session, UUID cauuid, UUID keyuuid){
		return getName(keyuuid, session);
	}
	
	/**
	 * Load the label from the database for the given uuid.
	 * @param uuid
	 * @param session
	 * @return
	 */
	private String getName(UUID uuid, Session session){
		return Label.getDescription(uuid, session);
	}
	
	/**
	 * @see org.wcs.smart.query.datamodel.IDataModelManager#getActiveAttributes(org.wcs.smart.ca.datamodel.DataModel)
	 */
	public List<Attribute> getActiveAttributes(DataModel dm){
		List<Attribute> active = new ArrayList<Attribute>();
		for (Attribute a : dm.getAttributes()){
			if (isActive(a, dm)){
				active.add(a);
			}
		}
		return active;
		
	}
	
	@Override
	public int getActiveDepth(){
		if (dmDepth != null){
			return dmDepth;
		}
		int numCategory = 0;
		for (Category cat : getDataModel().getActiveCategories()) {
			numCategory = Math.max(numCategory, getDepth(cat));
		}
		dmDepth = numCategory;
		return dmDepth;
	}
	
	/**
	 * Compute the maximum category depth.
	 * 
	 * @param cat category
	 * @return maximum depth
	 */
	private int getDepth(Category cat) {
		int maxDepth = 0;
		for (Category child : cat.getActiveChildren()) {
			maxDepth = Math.max(maxDepth, getDepth(child));
		}
		return maxDepth + 1;
	}
	
	
	/*
	 * determines if attribute in the data model
	 * has an active category association
	 */
	private boolean isActive(Attribute a, DataModel dm){
		for (Category c : dm.getActiveCategories()){
			if (isActive(c, a)){
				return true;
			}
		}
		return false;
	}
	
	/*
	 * determines if the attribute has an active association
	 * with the given category or subcategory
	 */
	private boolean isActive(Category c, Attribute a){
		for (CategoryAttribute ca : c.getAttributes()){
			if (ca.getAttribute().equals(a) && ca.getIsActive()){
				return true;
			}
		}
		for (Category kid : c.getActiveChildren()){
			if (isActive(kid,a)){
				return true;
			}
		}
		return false;
	}
	
	
	
	
	private Job loadDataModelJob = new Job(Messages.CaDataModelManagerImpl_LoadDataModelJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			dmDepth = null;
			dm = null;
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				DataModel tmp = HibernateManager.loadDataModel(conservationArea, session);
				
				//load into memory; no-lazy loading here.
				for (Category cat: tmp.getCategories()){
					visitCategory(cat);
				}
				for (Category cat: tmp.getActiveCategories()){
					cat.getName();
//					visitCategory(cat);
				}
				for (Attribute att: tmp.getAttributes()){
					att.getAggregations().size();
				}
				dm = tmp;
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
		
			return Status.OK_STATUS;
		}
	
		/**
		 * visits a child and gets all attributes.
		 * 	<p>This is to ensure all data model elements
		 * are loaded in the hibernate session.  Circumvents
		 * the hibernate lazy-loading.</p>
		 * @param cat
		 */
		private void visitCategory(Category cat){
			cat.getName();
			for (Category child : cat.getChildren()){
				visitCategory(child);
				child.getName();
			}
			for (Category child : cat.getActiveChildren()){
				visitCategory(child);
				child.getName();
			}
			for (CategoryAttribute ca: cat.getAttributes()){
				ca.getAttribute().getName();
			}	
		}
	
	};
	
	
	
	
}
