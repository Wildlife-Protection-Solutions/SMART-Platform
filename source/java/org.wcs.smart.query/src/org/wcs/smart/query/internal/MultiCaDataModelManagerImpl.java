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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.util.SmartUtils;
/**
 * Data model manager for when users are performing cross
 * conservation analysis.
 * 
 * @author Emily
 *
 */
/**
 * @author Emily
 *
 */
public class MultiCaDataModelManagerImpl implements IDataModelManager{

	private DataModel dm = null;

	private Integer dmDepth = null;
	
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		dmDepth = null;
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
			synchronized (this) {
				if (dm != null){
					return dm;
				}
				Display.getDefault().syncExec(new Runnable(){

					@Override
					public void run() {
						ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
						try {
							dialog.run(true, false, loadAndMergeDataModelJob);
						} catch (Exception e) {
							QueryPlugIn.displayLog(Messages.MultiCaDataModelManagerImpl_MergeError + e.getLocalizedMessage(), e);
							e.printStackTrace();
						}
						
					}});
			}
		}
		return this.dm;
	}
	
	
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * <p>The objects returned are associated with the same conservation
	 * areas as the attribute passed in</p> 
	 * 
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@SuppressWarnings({"unchecked" })
	public List<AttributeListItem> getActiveAttributeListItems(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.keyId FROM AttributeListItem a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea IN (:cas) group by a.keyId having count(*) = :cnt"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount()); //$NON-NLS-1$
		
		List<String> keys = q.list();
		if (keys.size() == 0){
			//return empty list
			return new ArrayList<AttributeListItem>();
		}
		query = "FROM AttributeListItem a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca AND a.keyId IN (:keys)"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
		q.setParameterList("keys", keys); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		
		List<AttributeListItem> items = q.list();
		return items;
	}
	
	/**
	 * Determines all list items for the given
	 * attribute. 
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@Override
	public List<AttributeListItem> getAttributeListItems(Attribute attribute, Session session){
		return getActiveAttributeListItems(attribute, session);	
	}
	
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	@SuppressWarnings({"unchecked" })
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt"; //$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount()); //$NON-NLS-1$
		
		List<String> hkeys = q.list();
		if (hkeys.size() == 0){
			return new ArrayList<AttributeTreeNode>();
		}
		query = "FROM AttributeTreeNode a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca and a.hkey IN (:keys) and parent is null"; //$NON-NLS-1$
		q = session.createQuery(query);
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
		q.setParameterList("keys", hkeys); //$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId()); //$NON-NLS-1$
			
		List<AttributeTreeNode> roots = q.list();
		
		//load all kids
		for (AttributeTreeNode node:roots){
			loadChildren(node);
		}
		//merge all kids evicting as we go so we don't have problems later
		for (AttributeTreeNode node:roots){
			visitTreeNode(session, node, hkeys);
		}
			
		return roots;		
	}
	private void loadChildren(AttributeTreeNode parent){
		for (AttributeTreeNode kid : parent.getChildren()){
			loadChildren(kid);
		}
	}
	private void visitTreeNode(Session session, AttributeTreeNode parent, List<String> keys){
		session.evict(parent);
		for (Iterator<AttributeTreeNode> iterator = parent.getChildren().iterator(); iterator.hasNext();) {
			AttributeTreeNode node = (AttributeTreeNode) iterator.next();
			if (!keys.contains(node.getHkey())){
				iterator.remove();
			}
		}
		parent.setActiveChildren(parent.getChildren());
		for (AttributeTreeNode child: parent.getChildren()){
			visitTreeNode(session,child,keys);
		}
	}
	

	/**
	 * Returns the attribute with the given key from 
	 * the shared data model.
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, String attributeKey){
		DataModel dm = getDataModel();
		for (Attribute a : dm.getAttributes()){
			if (a.getKeyId().equals(attributeKey)){
				return a;
			}
		}
		return null;
	}

	
	/**
	 * Returns the attribute associated with the main conservation area with the attribute of the given key.
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, Attribute attribute){
		if (attribute.getConservationArea().equals(SmartDB.getConservationAreaConfiguration().getMainConservationArea())){
			return attribute;
		}
		return getAttribute(session, attribute.getKeyId());
	}
	
	
	/**
	 * 
	 * Gets all the attribute tree nodes at 
	 * a given level in the data tree.    
	 * 
	 * <p>returns only items shared across all conservation areas</p>
	 * 
	 * @param session
	 * @param uuid attribute uuid
	 * @param level tree node level
	 * @param active ignored
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session, Attribute attribute, int level, boolean active){
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :key and smart.hkeyLength(a.hkey) = :level AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt";//$NON-NLS-1$
		Query q = session.createQuery(query);
		q.setParameter("key", attribute.getKeyId());//$NON-NLS-1$
		q.setParameter("level", level);//$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas());//$NON-NLS-1$
		q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());//$NON-NLS-1$
		List<String> hkeys = q.list();
			
		q = session.createQuery("FROM AttributeTreeNode a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca and hkey in (:hkeys)");//$NON-NLS-1$
		q.setParameter("attributeKey", attribute.getKeyId());//$NON-NLS-1$
		q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
		q.setParameterList("hkeys", hkeys);//$NON-NLS-1$
		List<AttributeTreeNode> nodes = q.list();
		return nodes;
	}
	
	
	/**
	 * Loads the category for the given category key from the shared data model  
	 * @param session
	 * @param categoryKey
	 * @return category object or <code>null</code> if not loaded
	 */
	@Override
	public Category getCategory(Session session, String categoryKey){
		DataModel dm = getDataModel();
		Category category = findCategory(categoryKey, dm.getCategories());
		return category;
		
	}
	
	/**
	 * 
	 * Gets all the categories at a given level that are shared across all conservation areas.
	 * The objects returned are associated with the main conservation area. 
	 * 
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
	 * @return attributelistitem the list item key
	 * @return the attribute list item associated with the main conservation area or <code>null</code> if
	 * attribute list item is not shared across all conservation areas.
	 */
	@Override
	public AttributeListItem getAttributeListItem(Session session, String attributeKey, String attributeListItem){
		Query q = session.createQuery(" SELECT ali From AttributeListItem ali join ali.attribute as a where a.conservationArea in (:cas) and ali.keyId = :key and a.keyId = :attributeKey"); //$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("key", attributeListItem); //$NON-NLS-1$
		q.setParameter("attributeKey", attributeKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<AttributeListItem> results = q.list();
		if (results.size() == SmartDB.getConservationAreaConfiguration().getCaCount()){
			for(AttributeListItem i : results){
				if (i.getAttribute().getConservationArea().equals(SmartDB.getConservationAreaConfiguration().getMainConservationArea())){
					return i;
				}
			}
			return results.get(0);
		}
		return null;
			
	}
	
	/**
	 * Loads an attribute tree node for the given key.
	 * Returns the attributetreenode associated with the main conservation area if 
	 * found otherwise will return null
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeTreeNode getAttributeTreeNode(Session session, String attributeKey, String attributeTreeHKey){
		Query q = session.createQuery(" SELECT ali From AttributeTreeNode ali join ali.attribute as a where a.conservationArea IN (:cas) and ali.hkey = :key and a.keyId = :attribute"); //$NON-NLS-1$
		q.setParameterList("cas", SmartDB.getConservationAreaConfiguration().getConservationAreas()); //$NON-NLS-1$
		q.setParameter("key", attributeTreeHKey); //$NON-NLS-1$
		q.setParameter("attribute", attributeKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<AttributeTreeNode> results = q.list();
		if (results.size() == SmartDB.getConservationAreaConfiguration().getCaCount() ){
			for (AttributeTreeNode i : results){
				if (i.getAttribute().getConservationArea().equals(SmartDB.getConservationAreaConfiguration().getMainConservationArea())){
					return i;
				}
			}
			return results.get(0);
		}else{
			return null;
			
		}
	}
	
	/**
	 * Returns the full category label for a category with the
	 * given uuid.  This attempts to find the label
	 * in the same language as the current system language.
	 * If not it uses the default language of the conservation area.
	 * 
	 * @param session
	 * @param categoryUuid
	 * @return
	 */
	@Override
	public String[] getFullCategoryLabel(Session session, UUID categoryUuid){
		//if (true) return new String[]{"abc"};
		
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
			Language l = SmartUtils.findLanguageMatch(category.getConservationArea().getLanguages());
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
	public String getAttributeListItemLabel(Session session, UUID cauuid, UUID keyuuid ){
		return LabelConstants.getDescription(keyuuid, cauuid);
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
	public String getAttributeTreeNodeLabel(Session session, UUID cauuid, UUID keyuuid){
		return LabelConstants.getDescription(keyuuid, cauuid);
	}
	
	/**
	 * @see org.wcs.smart.query.datamodel.IDataModelManager#getActiveAttributes(org.wcs.smart.ca.datamodel.DataModel)
	 * @returns a list of all attributes in the merged data model
	 */
	public List<Attribute> getActiveAttributes(DataModel dm){
		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.addAll(dm.getAttributes());
		return attributes;
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
	
	private IRunnableWithProgress loadAndMergeDataModelJob = new IRunnableWithProgress() {
		
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {
			dmDepth = null;
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				ConservationAreaConfiguration config = SmartDB.getConservationAreaConfiguration();
				DataModelMerger merger = new DataModelMerger();
				dm = merger.mergeDataModels(
						config.getConservationAreas().toArray(new ConservationArea[config.getCaCount()]),
						config.getMainConservationArea(),
						session, monitor);
						
			}finally{
				session.getTransaction().rollback();
				session.close();
			}

			
		}
	};	
}
