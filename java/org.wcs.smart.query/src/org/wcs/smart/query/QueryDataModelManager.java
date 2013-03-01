package org.wcs.smart.query;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.MultipleCaAnalysisConfiguration;
import org.wcs.smart.hibernate.SmartDB;

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
/**
 * Class responsible for loading the SMART data model
 * to support querying.
 * 
 * @author Emily
 *
 */
public class QueryDataModelManager {

	private DataModel dm = null;
	
	private static QueryDataModelManager instance = null;
	
	public static QueryDataModelManager getInstance(){
		if (instance == null){
			instance = new QueryDataModelManager();
		}
		return instance;
	}
	
	/**
	 * Clears the current data model
	 */
	public void clearDataModel(){
		dm = null;
	}
	
	/**
	 * In the case of single conservation area querying this will
	 * return the data model of the current conservation area.  Otherwise
	 * this will return the merged data model of all the existing conservation
	 * areas.
	 * <p>This will block until the data model is loaded</p>
	 * @return the data model for querying
	 */
	public DataModel getDataModel(){
		if (dm == null){
			Job job = getDataModelJob();
			synchronized (instance) {
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
	 * Determines the active attribute list items for the given
	 * attribute.  If the case of multiple conservation area analysis this
	 * returns only items shared across all conservation areas, otherwise it returns
	 * active items in the current conservation area.
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	@SuppressWarnings({ "nls", "unchecked" })
	public List<AttributeListItem> getActiveAttributeListItems(Attribute attribute, Session session){
		if (SmartDB.isMultipleAnalysis()){
			//we need to only include items that are shared across all conservation areas
			String query = "SELECT a.keyId FROM AttributeListItem a join a.attribute b WHERE b.keyId = :attributeKey group by a.keyId having count(*) = :cnt";
			Query q = session.createQuery(query);
			q.setParameter("attributeKey", attribute.getKeyId());
			q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());
		
			List<String> keys = q.list();
			
			query = "FROM AttributeListItem a WHERE a.attribute = :attribute and a.keyId IN (:keys)";
			q = session.createQuery(query);
			q.setParameter("attribute", attribute);
			q.setParameterList("keys", keys);
			
			List<AttributeListItem> items = q.list();
//			for (AttributeListItem item:items){
//				item.setName(item.findName( SmartDB.getSelectedConservationAreas().get(0).getDefaultLanguage()));
//			}
			return items;
		
			
		}else{
			List<AttributeListItem> items = session
					.createCriteria(AttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute))
					.add(Restrictions.eq("isActive", true))
					.list();
			return items;
			
		}
		
	}
	/**
	 * Determines the active attribute tree items for the given
	 * attribute.  In the case of multiple conservation area analysis this
	 * returns only items shared across all conservation areas, otherwise it returns
	 * active items in the current conservation area.
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	@SuppressWarnings({ "nls", "unchecked" })
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(Attribute attribute, Session session){
		if (SmartDB.isMultipleAnalysis()){
			//we need to only include items that are shared across all conservation areas
			String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :attributeKey group by a.hkey having count(*) = :cnt";
			Query q = session.createQuery(query);
			q.setParameter("attributeKey", attribute.getKeyId());
			q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());
		
			List<String> hkeys = q.list();
			
			query = "FROM AttributeTreeNode a WHERE a.attribute = :attribute and a.hkey IN (:keys) and parent is null";
			q = session.createQuery(query);
			q.setParameter("attribute", attribute);
			q.setParameterList("keys", hkeys);
			
			List<AttributeTreeNode> roots = q.list();
			
			for (AttributeTreeNode node:roots){
//				node.setName(node.findName(SmartDB.getSelectedConservationAreas().get(0).getDefaultLanguage()));
				visitTreeNode(node, hkeys);
			}
			
			return roots;
		
			
		}else{
			attribute = (Attribute) session.load(Attribute.class, attribute.getUuid());
			if (attribute.getActiveTreeNodes() != null){
				for (AttributeTreeNode node : attribute.getActiveTreeNodes()){
					visitTreeNode(node, null);
				}
			}
			return attribute.getActiveTreeNodes();
			
		}
		
	}
	
	private void visitTreeNode(AttributeTreeNode parent, List<String> keys){
		if (keys == null){
			if (parent.getActiveChildren() != null){
				for (AttributeTreeNode child: parent.getActiveChildren()){
//					child.getHkey();
//					child.getName();
					visitTreeNode(child, keys);
				}
			}
		}else{
			for (Iterator<AttributeTreeNode> iterator = parent.getChildren().iterator(); iterator.hasNext();) {
				AttributeTreeNode node = (AttributeTreeNode) iterator.next();
				if (!keys.contains(node.getHkey())){
					iterator.remove();
				}
			}
			parent.setActiveChildren(parent.getChildren());
			for (AttributeTreeNode child: parent.getChildren()){
//				child.setName(child.findName(SmartDB.getSelectedConservationAreas().get(0).getDefaultLanguage()));
				visitTreeNode(child,keys);
			}
		}
	}
	
	
	public Attribute getAttribute(String attributeKey, Session session){
		Query q = session.createQuery("From Attribute where conservationArea = :ca and keyid = :key"); //$NON-NLS-1$
		if (SmartDB.isMultipleAnalysis()){
			q.setParameter("ca", SmartDB.getConservationAreaConfiguration().getMainConservationArea()); //$NON-NLS-1$
		}else{
			q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		}
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
	 * <p>In the case of multiple conservation area analysis this
	 * returns only items shared across all conservation areas, otherwise it returns
	 * active items in the current conservation area.</p>
	 * 
	 * @param session
	 * @param level
	 * @param active if only active tree nodes should be loaded
	 * @return
	 */
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session, byte[] uuid, int level, boolean active){
		if (SmartDB.isMultipleAnalysis()){
			Attribute a = (Attribute) session.get(Attribute.class, uuid);
			String query = "SELECT a.hkey FROM AttributeTreeNode a join a.attribute b WHERE b.keyId = :key and smart.hkeyLength(a.hkey) = :level group by a.hkey having count(*) = :cnt";
			Query q = session.createQuery(query);
			q.setParameter("key", a.getKeyId());
			q.setParameter("level", level);
			q.setInteger("cnt", SmartDB.getConservationAreaConfiguration().getCaCount());
			List<String> hkeys = q.list();
			
			q = session.createQuery("FROM AttributeTreeNode a WHERE a.attribute.uuid = :uuid and hkey in (:hkeys)");
			q.setParameter("uuid" ,uuid);
			q.setParameterList("hkeys", hkeys);
			List<AttributeTreeNode> nodes = q.list();
//			for (AttributeTreeNode n : nodes){
//				n.setName(n.findName(SmartDB.getSelectedConservationAreas().get(0).getDefaultLanguage()));
//			}
			return nodes;
				
		}else{
			String query = "FROM AttributeTreeNode WHERE attribute_uuid =:uuid AND smart.hkeyLength(hkey) = :level and isActive = :active"; //$NON-NLS-1$
			Query q = session.createQuery(query);
			q.setParameter("uuid", uuid); //$NON-NLS-1$
			q.setParameter("level", level); //$NON-NLS-1$
			q.setParameter("active", active); //$NON-NLS-1$
		
			List<AttributeTreeNode> nodes = q.list();
			return nodes;
		}
	}
	
	private Job getDataModelJob(){
		if (SmartDB.isMultipleAnalysis()){
			return loadAndMergeDataModelJob;
		}else{
			return loadDataModelJob;
		}
	}
	
	private Job loadAndMergeDataModelJob = new Job("Load And Merge Data Models"){
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
	
	private Job loadDataModelJob = new Job("Load Data Model"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
				//load into memory; no-lazy loading here.
				for (Category cat: dm.getCategories()){
					visitCategory(cat);
				}
				for (Attribute att: dm.getAttributes()){
					att.getAggregations().size();
				}
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
