/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;

import com.ibm.icu.text.Collator;

/**
 * Access the merged Data Model for Cross Conservation Area Analysis.
 * 
 * This merges data models together and updates icons to use
 * the icons defined in the CCAA conservation area.
 * 
 * @author Emily
 *
 */
public abstract class CcaaDataModel {

	
	protected SimpleDataModel dm = null;
	private Integer maxCategoryDepth = null;
	protected ConservationArea[] cas;
	protected ConservationArea core;
	

	
	protected CcaaDataModel(ConservationArea core, Collection<ConservationArea> cas) {
		this.core = core;
		this.cas = cas.toArray(new ConservationArea[cas.size()]);
		
	}
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		maxCategoryDepth = null;
		dm = null;
	}
	
	/**
	 * Return the merged data model of all the existing conservation
	 * areas.
	 * <p>This will block until the data model is loaded</p>
	 * @return the data model for querying
	 */
	public SimpleDataModel getDataModel() {
		if (dm != null) return dm;
		
		synchronized (this) {
			maxCategoryDepth = null;
			getDataModelInternal();
			return dm;
		}
	}

	/**
	 * needs to update the dm field
	 * @return
	 */
	protected abstract void getDataModelInternal();
	
	
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
	public List<AttributeListItem> getAttributeListItems(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.keyId FROM AttributeListItem a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea IN (:cas) group by a.keyId having count(*) = :cnt"; //$NON-NLS-1$
		Query<String> q = session.createQuery(query, String.class)
			.setParameterList("cas", cas) //$NON-NLS-1$
			.setParameter("attributeKey", attribute.getKeyId()) //$NON-NLS-1$
			.setParameter("cnt", Long.valueOf(cas.length)); //$NON-NLS-1$
		
		List<String> keys = q.list();
		if (keys.size() == 0){
			//return empty list
			return new ArrayList<AttributeListItem>();
		}
		query = "FROM AttributeListItem a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca AND a.keyId IN (:keys)"; //$NON-NLS-1$
		
		List<AttributeListItem> items = session.createQuery(query, AttributeListItem.class)
			.setParameter("ca", core) //$NON-NLS-1$
			.setParameterList("keys", keys) //$NON-NLS-1$
			.setParameter("attributeKey", attribute.getKeyId()) //$NON-NLS-1$
			.list();
		//update icons
		for (AttributeListItem li : items) {
			li.setIcon( DataModelMerger.findIcon(session, findIconKey(li, session)) );
		}
		
		//sort alphabetically as orders may be duplicated
		items.sort((a,b)->Collator.getInstance().compare(a.getName(),  b.getName()));
		return items;
	}
	
	private String findIconKey (AttributeListItem a, Session session){
		if (a.getIcon() != null) return a.getIcon().getKeyId();
		
		String hql = "SELECT distinct icon.keyId FROM AttributeListItem WHERE keyId = :key AND attribute.conservationArea in (:ca) AND icon is not null";//$NON-NLS-1$
		Query<String> q = session.createQuery(hql, String.class);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", cas);//$NON-NLS-1$
		
		List<String> items = q.list();
		if (items.isEmpty()) return null;
		return items.get(0);
	}

	private String findIconKey (AttributeTreeNode a, Session session){
		if (a.getIcon() != null) return a.getIcon().getKeyId();
		
		String hql = "SELECT distinct icon.keyId FROM AttributeTreeNode WHERE keyId = :key AND attribute.conservationArea in (:ca) AND icon is not null";//$NON-NLS-1$
		Query<String> q = session.createQuery(hql, String.class);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", cas);//$NON-NLS-1$
		
		List<String> items = q.list();
		if (items.isEmpty()) return null;
		return items.get(0);
	}
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	public List<AttributeTreeNode> getAttributeTreeNodes(Attribute attribute, Session session){
		//we need to only include items that are shared across all conservation areas
		String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :attributeKey AND b.conservationArea in (:cas) group by a.hkey having count(*) = :cnt"; //$NON-NLS-1$
		
		List<String> hkeys = session.createQuery(query, String.class)
			.setParameterList("cas", cas) //$NON-NLS-1$
			.setParameter("attributeKey", attribute.getKeyId()) //$NON-NLS-1$
			.setParameter("cnt", Long.valueOf(cas.length)) //$NON-NLS-1$
			.list();
		if (hkeys.size() == 0){
			return new ArrayList<AttributeTreeNode>();
		}
		
		query = "FROM AttributeTreeNode a WHERE a.attribute.keyId = :attributeKey AND a.attribute.conservationArea = :ca and a.hkey IN (:keys) and parent is null"; //$NON-NLS-1$
		List<AttributeTreeNode> roots = session.createQuery(query, AttributeTreeNode.class)
			.setParameter("ca", core) //$NON-NLS-1$
			.setParameterList("keys", hkeys) //$NON-NLS-1$
			.setParameter("attributeKey", attribute.getKeyId()) //$NON-NLS-1$
			.list();
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
		
		//update icons
		parent.setIcon( DataModelMerger.findIcon(session, findIconKey(parent, session)) );
				
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
	 * 
	 * Gets all the categories at a given level that are shared across all conservation areas.
	 * The objects returned are associated with the main conservation area. 
	 * 
	 * @param session
	 * @param level
	 * @return
	 */
	public List<Category> getCategories(Session session, int level){
		List<Category> toVisit = new ArrayList<>();
		toVisit.addAll(getDataModel().getCategories());
		int visitlevel = 0;
		if (visitlevel == level) return toVisit;
		
		List<Category> nextVisit = new ArrayList<>();
		while(!toVisit.isEmpty()) {
			
			while(!toVisit.isEmpty()) {
				Category next = toVisit.remove(0);
				nextVisit.addAll(next.getChildren());
			}
			
			if (visitlevel + 1 == level) {
				return nextVisit;
			}
			toVisit = nextVisit;
			nextVisit = new ArrayList<>();
			visitlevel++;
			
		}
		return Collections.emptyList();
		
	}
	
	/**
	 * Get a list of root categories
	 * @return
	 */
	public List<Category> getCategories(){
		return getDataModel().getCategories();
	}
	
	/**
	 * @see org.wcs.smart.query.datamodel.IDataModelManager#getActiveAttributes(org.wcs.smart.ca.datamodel.DataModel)
	 * 
	 * @returns a list of all attributes in the merged data model
	 */
	public List<Attribute> getAttributes(){
		return new ArrayList<>(getDataModel().getAttributes());
	}
	
	
	public int getCategoryDepth(){
		if (maxCategoryDepth != null) return maxCategoryDepth;
			
		int numCategory = 0;
		for (Category cat : getDataModel().getCategories()) {
			numCategory = Math.max(numCategory, getDepth(cat));
		}
		maxCategoryDepth = numCategory;
		return maxCategoryDepth;
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
	
	
}
