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
package org.wcs.smart.query.internal.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.MultipleCaAnalysisConfiguration;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.util.SmartUtils;
/**
 * Data model manager for when users are performing cross
 * conservation analysis.
 * 
 * @author Emily
 *
 */
public class MultiCaDataModelManagerImpl implements IDataModelManager{

	private DataModel dm = null;

	
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		dm = null;
	}
	
	/**
	 * Return the merged data model of all the existing conservation
	 * areas.
	 * <p>This will block until the data model is loaded</p>
	 * @return the data model for querying
	 */
	public DataModel getDataModel(){
		if (dm == null){
			Job job = loadAndMergeDataModelJob;
			synchronized (this) {
				if (job.getState() == Job.NONE || job.getState() == Job.SLEEPING){
					job.schedule();
				}
			}
			
			try{
				//wait for the current job to finish
				job.join();
			}catch (Exception ex){
				QueryPlugIn.log(ex.getMessage(), ex);
			}
		}
		return this.dm;
	}
	
	
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@SuppressWarnings({ "nls", "unchecked" })
	public List<AttributeListItem> getActiveAttributeListItems(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.keyId FROM AttributeListItem a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea IN (:cas) group by a.keyId having count(*) = :cnt"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount()); //$NON-NLS-1$
		
		List<String> keys = q.list();
			
		query = "FROM AttributeListItem a WHERE a.attribute = :attribute and a.keyId IN (:keys)"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("attribute", attribute); //$NON-NLS-1$
		q.setParameterList("keys", keys); //$NON-NLS-1$
			
		List<AttributeListItem> items = q.list();
		return items;
	}
	
	
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	@SuppressWarnings({ "nls", "unchecked" })
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount()); //$NON-NLS-1$
		
		List<String> hkeys = q.list();
			
		query = "FROM AttributeTreeNode a WHERE a.attribute = :attribute and a.hkey IN (:keys) and parent is null"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("attribute", attribute); //$NON-NLS-1$
		q.setParameterList("keys", hkeys); //$NON-NLS-1$
			
		List<AttributeTreeNode> roots = q.list();
			
		for (AttributeTreeNode node:roots){
			visitTreeNode(node, hkeys);
		}
			
		return roots;		
	}
	
	private void visitTreeNode(AttributeTreeNode parent, List<String> keys){
		for (Iterator<AttributeTreeNode> iterator = parent.getChildren().iterator(); iterator.hasNext();) {
			AttributeTreeNode node = (AttributeTreeNode) iterator.next();
			if (!keys.contains(node.getHkey())){
				iterator.remove();
			}
		}
		parent.setActiveChildren(parent.getChildren());
		for (AttributeTreeNode child: parent.getChildren()){
			visitTreeNode(child,keys);
		}
	}
	

	/**
	 * Returns the attribute associated with the main conservation area with the given key.
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, String attributeKey){
		Query q = session.createQuery("From Attribute where conservationArea = :ca and keyid = :key"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
		q.setParameter("key", attributeKey); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Attribute> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
	}
	
	
	/**
	 * 
	 * Gets all the attribute tree nodes at 
	 * a given level in the data tree.    
	 * 
	 * <p>returns only items shared across all conservation areas</p>
	 * 
	 * @param session
	 * @param level
	 * @param active if only active tree nodes should be loaded
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session, byte[] uuid, int level, boolean active){
		Attribute a = (Attribute) session.get(Attribute.class, uuid);
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :key and smart.hkeyLength(a.hkey) = :level AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt";//$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameter("level", level);//$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas());//$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());//$NON-NLS-1$
		List<String> hkeys = q.list();
			
		q = session.createQuery("FROM AttributeTreeNode a WHERE a.attribute.uuid = :uuid and hkey in (:hkeys)");//$NON-NLS-1$
		q.setParameter("uuid" ,uuid);//$NON-NLS-1$
		q.setParameterList("hkeys", hkeys);//$NON-NLS-1$
		List<AttributeTreeNode> nodes = q.list();
		return nodes;
	}
	
	
	/**
	 * Loads the category for the given category key from the main conservation area 
	 * @param session
	 * @param categoryKey
	 * @return category object or <code>null</code> if not loaded
	 */
	@Override
	public Category getCategory(Session session, String categoryKey){
		Query q = session.createQuery("From Category where conservationArea = :ca and hkey = :key"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea());	 //$NON-NLS-1$
		q.setParameter("key", categoryKey); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Category> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
		
	}
	
	
	/**
	 * 
	 * Gets all the categories at a given level in the data tree associated with the
	 * main conservation area
	 * @param session
	 * @param level
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Category> getCategories(Session session, int level){
		String query = "SELECT hkey FROM Category WHERE smart.hkeyLength(hkey) = :level and conservationArea in (:cas) group by hkey having count(*) = :cnt";//$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameter("level", level);//$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());//$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas());//$NON-NLS-1$
		List<String> hkeys = q.list();
			
		q = session.createQuery("FROM Category WHERE hkey in (:hkeys) and conservationArea = :ca");//$NON-NLS-1$
		q.setParameterList("hkeys", hkeys);//$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea());//$NON-NLS-1$
		List<Category> nodes = q.list();
		return nodes;
		
	}
	
	/**
	 * Loads an attribute list item for the given key and the
	 * main conservation area 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeListItem getAttributeListItem(Session session, String attributeKey, String attributeListItem){
		Query q = session.createQuery(" SELECT ali From AttributeListItem ali join ali.attribute as a where a.conservationArea = :ca and ali.keyId = :key and a.keyId = :attributeKey"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
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
	 * Loads an attribute tree not item item for the given hkey and the main 
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeTreeNode getAttributeTreeNode(Session session, String attributeKey, String attributeTreeHKey){
		Query q = session.createQuery(" SELECT ali From AttributeTreeNode ali join ali.attribute as a where a.conservationArea = :ca and ali.hkey = :key and a.keyId = :attribute"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
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
	 * given uuid.  This attempts to find the label
	 * in the same langauge as the current system language.
	 * If not it uses the default language of the conservation area.
	 * 
	 * @param session
	 * @param categoryUuid
	 * @return
	 */
	@Override
	public String[] getFullCategoryLabel(Session session, byte[] categoryUuid){
		DataModel dm = getDataModel();
		
		Category category = (Category) session.load(Category.class, categoryUuid);
		Category existingCategory = findCategory(category.getHkey(), dm.getCategories());
		
		//find the shared category in the datamodel
		//and use those labels; if not found use
		//the labels of the category
		ArrayList<String> values = new ArrayList<String>();
		if (existingCategory != null){
			values.add(existingCategory.getName());
			Category parent = existingCategory.getParent();
			while(parent != null){
				values.add(parent.getName());
				parent = parent.getParent();
			}	
		}else{
			
			Language l = SmartUtils.findLanguageMatch(category.getNames()).getLanguage();
			if (l == null){
				//default language of conservation area
				l = category.getConservationArea().getDefaultLanguage();
			}
			values.add(category.findName(l));
			Category parent = category.getParent();
			while(parent != null){
				values.add(parent.findName(l));
				parent = parent.getParent();
			}	
		}

		Collections.reverse(values);
		return values.toArray(new String[values.size()]);
		
		
	}
	private Category findCategory(String hkey, List<Category> categories){
		if (categories == null){
			return null;
		}
		for (Category kid :categories){
			if (kid.getHkey().equals(hkey)){
				return kid;
			}
		}
		for (Category kid :categories){
			Category find = findCategory(hkey, kid.getChildren());
			if (find != null){
				return find;
			}
		}
		return null;
	}
	
	/**
	 * This searches the merged data model for the given attribute and list
	 * item.  If found it uses the label provided by the data model otherwise
	 * it uses the label provided by the conservation area associated with the
	 * attribute that matches the code of the current active language
	 * or the default language.
	 * 
	 * @return the label to use for the given 
	 * attribute list item
	 * 
	 */
	@Override
	public String getAttributeListItemLabel(Session session, Attribute attribute, byte[] keyuuid){
		AttributeListItem item = (AttributeListItem) session.load(AttributeListItem.class, keyuuid);
		
		DataModel dm = getDataModel();
		for (Attribute a: dm.getAttributes()){
			if (a.getKeyId().equals(attribute.getKeyId())){
				a = (Attribute) session.load(Attribute.class, a.getUuid());
				for (AttributeListItem i : a.getAttributeList()){
					if (item.getKeyId().equals(i.getKeyId())){
						return i.getName();
					}
				}		
			}
		}
		
		
		//attribute not found in database
		Label l = SmartUtils.findLanguageMatch(item.getNames());
		if (l != null){
			return l.getValue();
		}else{
			return item.findName(attribute.getConservationArea().getDefaultLanguage()); 
		}
	}

	
	/**
	 * This searches the merged data model for the given attribute and tree
	 * node.  If found it uses the label provided by the data model otherwise
	 * it uses the label provided by the conservation area associated with the
	 * attribute that matches the code of the current active language
	 * or the default language.
	 * 
	 * @return the label to use for the given attribute tree node
	 */
	@Override
	public String getAttributeTreeNodeLabel(Session session, Attribute attribute, byte[] keyuuid){
		AttributeTreeNode item = (AttributeTreeNode) session.load(AttributeTreeNode.class, keyuuid);
		
		//does this key exist in the merged data model?  if so
		//we need to return that names
		DataModel dm = getDataModel();
		for (Attribute a : dm.getAttributes()){
			if (a.getKeyId().equals(attribute.getKeyId())){
				a = (Attribute) session.load(Attribute.class, a.getUuid());
				AttributeTreeNode found = findTreeNode(item.getHkey(), a.getTree());
				if (found != null){
					return found.getName();
				}
			}
		}
		
		//otherwise return the name provided with ca
		Label l = SmartUtils.findLanguageMatch(item.getNames());
		if (l != null){
			return l.getValue();
		}
		return item.findName(attribute.getConservationArea().getDefaultLanguage());
	}
	
	private AttributeTreeNode findTreeNode(String hkey, List<AttributeTreeNode> parents){
		if (parents == null){
			return null;
		}
		for (AttributeTreeNode node : parents){
			if (node.getHkey().equals(hkey)){
				return node;
			}
		}
		for (AttributeTreeNode node : parents){
			AttributeTreeNode found = findTreeNode(hkey, node.getChildren());
			if (found != null){
				return found;
			}
		}
		return null;
	}
	private Job loadAndMergeDataModelJob = new Job(Messages.MultiCaDataModelManagerImpl_LoadMergeJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				MultipleCaAnalysisConfiguration config = SmartDB.getConservationAreaConfiguration();
				DataModelMerger merger = new DataModelMerger();
				dm = merger.mergeDataModels(
						config.getConservationAreas().toArray(new ConservationArea[config.getCaCount()]),
						config.getMainConservationArea(),
						session);
						
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			return Status.OK_STATUS;
		}
	};
	
}
