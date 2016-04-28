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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Merges two datamodels, returning a data model that only
 * includes items that exist in all datamodels.
 *  
 * @author Emily
 *
 */
public class DataModelMerger {

	private Language defaultLanguage = null;
	
	public DataModelMerger(){
		
	}
	
	/**
	 * Merges the data models associated with the provided conservation areas.
	 * Datamodel items (categories, attributes, categoryAttribute) are only
	 * kept if all datamodels have the same key;
	 * 
	 * @param cas conservation area data models to merge
	 * @param session hibernate session
	 * @return a merged data model
	 */
	public DataModel mergeDataModels(ConservationArea[] cas, 
			ConservationArea defaultCa, 
			Session session, IProgressMonitor monitor){
		
		if (cas == null || cas.length < 2){
			throw new IllegalStateException("more than 1 conservation area must be provided");//$NON-NLS-1$
		}
		// load data model for default Ca
		monitor.beginTask(Messages.DataModelMerger_TaskName, 4);
		
		monitor.subTask(Messages.DataModelMerger_SubTask1);
		DataModel dm = HibernateManager.loadDataModel(defaultCa, session);
		monitor.worked(1);
		
		DataModel newDataModel = new DataModel(defaultCa, new ArrayList<Category>(), new ArrayList<Attribute>());
		defaultLanguage = SmartUtils.findLanguageMatch(defaultCa.getLanguages());
		if (defaultLanguage == null){
			defaultLanguage = defaultCa.getDefaultLanguage();
		}
				
		// ATTRIBUTES
		monitor.subTask(Messages.DataModelMerger_SubTask2);
		for (Attribute a : dm.getAttributes()){
			if (canKeep(a, cas, session)){
				Attribute copy = new Attribute();
				copy.setAggregations(a.getAggregations());
				copy.setConservationArea(SmartDB.getCurrentConservationArea());
				copy.setIsRequired(a.getIsRequired());
				copy.setKeyId(a.getKeyId());
				copy.setMaxValue(a.getMaxValue());
				copy.setMinValue(a.getMinValue());
				copy.setName(a.getName());
				copy.setRegex(a.getRegex());
				copy.setType(a.getType());
				
				newDataModel.getAttributes().add(copy);
				mergeAttributeAggregations(copy,cas, session);
			}
		}
		monitor.worked(1);
		
		/* root categories */
		monitor.subTask(Messages.DataModelMerger_SubTask3);
		for (Category c : dm.getCategories()){
			if (canKeep(c, cas, session)){
				Category newRoot = cloneCategory(c, null, newDataModel.getAttributes(), cas,session);
				newDataModel.addRootCategories(newRoot);
			}
		}
		monitor.worked(1);
		
		return newDataModel;
	}
	

	private Category cloneCategory(Category toClone, 
			Category parent, List<Attribute> clonedAttributes, 
			ConservationArea[] cas, Session session){
		
		Category clone = new Category();
		clone.setHkey(toClone.getHkey());
		clone.setKeyId(toClone.getKeyId());
		clone.setCategoryOrder(toClone.getCategoryOrder());
		clone.setParent(parent);
		clone.setIsActive(true);
		
		//clone the labels
		clone.setName(toClone.getName());
			
		if (toClone.getAttributes() != null){
			clone.setAttributes(new ArrayList<CategoryAttribute>());
			for (CategoryAttribute attribute : toClone.getAttributes()){
				if (canKeep(attribute, cas, session)){
					//	find attribute
					Attribute newAttribute = null;
					for (Attribute clonedAttribute: clonedAttributes){
						if (clonedAttribute.getKeyId().equals(attribute.getAttribute().getKeyId())){
							newAttribute = clonedAttribute;
							break;
						}
					}
					if (newAttribute !=  null){
						CategoryAttribute link = new CategoryAttribute(clone, newAttribute);
						link.setIsActive(true);
						clone.getAttributes().add(link);
					}
				}
				
			}
		}
		if (toClone.getChildren() != null){
			clone.setChildren(new ArrayList<Category>());
			
			for (Category child : toClone.getChildren()){
				if (canKeep(child,cas, session)){
					Category kid = cloneCategory(child,clone,clonedAttributes, cas, session);
					clone.getChildren().add(kid);
				}
			}
			clone.setActiveChildren(clone.getChildren());
		}
		
		
		return clone;
	}

	
	@SuppressWarnings("unchecked")
	private void mergeAttributeAggregations(Attribute a, ConservationArea[] ca, Session session){
		List<Aggregation> aggs = new ArrayList<Aggregation>(a.getAggregations());
		if (aggs.size() == 0){
			return;
		}
		
		String hql = "FROM Attribute a where a.keyId = :key and a.conservationArea in (:ca)"; //$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("key", a.getKeyId()); //$NON-NLS-1$
		q.setParameterList("ca", ca); //$NON-NLS-1$
		List<Attribute> results = q.list();
		for (Attribute tmp : results){
			for (Iterator<Aggregation> iterator = aggs.iterator(); iterator.hasNext();) {
				Aggregation agg = (Aggregation) iterator.next();
				if (!tmp.getAggregations().contains(agg)){
					iterator.remove();
				}
			}
		}
		a.setAggregations(aggs);
		return;
	}
	/**
	 * Determines if an attribute is shared across all conservation areas.
	 * The attribute key must exist in each conservation area.
	 * 
	 * @param a
	 * @param ca
	 * @param session
	 * @return
	 */
	private boolean canKeep(Attribute a, ConservationArea[] ca, Session session){
		String hql = "FROM Attribute WHERE keyId = :key AND conservationArea in (:ca)";//$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$
		List<Attribute> atts = q.list();
		if (atts.size() != ca.length) return false;
		if (atts.size() == 0) return false;
		Attribute.AttributeType type = atts.get(0).getType();
		for (Attribute at : atts){
			if (at.getType() != type){
				//types do not match.
				return false;
			}
		}
		//attribute exists in each ca with the same type
		return true;
	}
	
	/**
	 * Determines if a category is shared across all conservation areas.
	 * The category hkey must exist in all conservation areas.
	 * @param c
	 * @param ca
	 * @param session
	 * @return
	 */
	private boolean canKeep(Category c, ConservationArea[] ca, Session session){
		String hql = "SELECT count(*) FROM Category WHERE hkey = :key AND conservationArea in (:ca)";//$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("key", c.getHkey());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$
		Long cnt = (Long) q.list().get(0);
		if (cnt == ca.length){
			//this category exists in each conservation area so we keep it
			return true;
		}
		return false;
	}
	
	/**
	 * Determines if a category/attribute relationship is shared
	 * across all conservation areas.
	 * 
	 * @param c
	 * @param ca
	 * @param session
	 * @return
	 */
	private boolean canKeep(CategoryAttribute c, ConservationArea[] ca, Session session){
		String hql = "SELECT distinct a.conservationArea.uuid, a.keyId FROM CategoryAttribute ca join  ca.id.attribute a join ca.id.category c WHERE c.hkey = :ckey AND a.keyId = :akey and a.conservationArea in (:ca)";//$NON-NLS-1$
		
		Query q = session.createQuery(hql);
		q.setParameter("ckey", c.getCategory().getHkey());//$NON-NLS-1$
		q.setParameter("akey", c.getAttribute().getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", ca); //$NON-NLS-1$
		int cnt = q.list().size();
		if (cnt == ca.length){
			//this category exists in each conservation area so we keep it
			return true;
		}
		return false;
	}
}
