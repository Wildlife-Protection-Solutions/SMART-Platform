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
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.hibernate.QueryFactory;

import jakarta.persistence.Tuple;


/**
 * Merges multiple datamodels, returning a data model that only
 * includes items that exist in all datamodels.  Generally this should
 * not be used by other plugins.  IF you want to access the merged data model 
 * use the CcaaDataModel class.
 *  
 * @author Emily
 *
 */
public class DataModelMerger {

	public enum ProgressMessages{
		TASKNAME,
		LOADING,
		MERGINGATTRIBUTES,
		MERGINGCATEGORIES
	}
	
	
	public DataModelMerger(){
		
	}
	
	
	/**
	 * Merges the data models associated with the provided conservation areas.
	 * Datamodel items (categories, attributes, categoryAttribute) are only
	 * kept if all datamodels have the same key;
	 * 
	 * @param cas conservation area data models to merge
	 * @param session hibernate session
	 * @param l - the local (only used for progress messages and if null the default locale will be used)
	 * @return a merged data model
	 */
	public SimpleDataModel mergeDataModels(ConservationArea[] cas, 
			ConservationArea defaultCa, 
			Session session, Locale l, 
			IProgressMonitor monitor){
		
		if (l == null) l = Locale.getDefault();
		if (cas == null){
			throw new IllegalStateException("at least 1 conservation area must be provided");//$NON-NLS-1$
		}
		// load data model for default Ca
		monitor.beginTask(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ProgressMessages.TASKNAME, l), 4);
		
		monitor.subTask(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ProgressMessages.LOADING, l));
		SimpleDataModel dm;
		try {
			dm = SimpleDataModel.loadDataModel(defaultCa, session);
		} catch (Exception e) {
			throw new IllegalStateException("unable to load data model", e);//$NON-NLS-1$
		}
		monitor.worked(1);
		
		
		SimpleDataModel newDataModel = new SimpleDataModel(defaultCa, new ArrayList<Category>(), new ArrayList<Attribute>());
						
		// ATTRIBUTES
		monitor.subTask(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ProgressMessages.MERGINGATTRIBUTES, l));
		for (Attribute a : dm.getAttributes()){
			if (canKeep(a, cas, session)){
				Attribute copy = new Attribute();
				copy.setAggregations(a.getAggregations());
				//copy.setConservationArea(SmartDB.getCurrentConservationArea());
				copy.setIsRequired(a.getIsRequired());
				copy.setKeyId(a.getKeyId());
				copy.setMaxValue(a.getMaxValue());
				copy.setMinValue(a.getMinValue());
				copy.setName(a.getName());
				copy.setRegex(a.getRegex());
				copy.setType(a.getType());
				
				copy.setIcon(findIcon(session, findIconKey(a, cas, session)));
				
				newDataModel.getAttributes().add(copy);
				mergeAttributeAggregations(copy,cas, session);
			}
		}
		monitor.worked(1);
		
		/* root categories */
		monitor.subTask(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ProgressMessages.MERGINGCATEGORIES, l));
		for (Category c : dm.getCategories()){
			if (canKeep(c, cas, session)){
				Category newRoot = cloneCategory(c, null, newDataModel.getAttributes(), cas,session);
				newDataModel.getCategories().add(newRoot);
			}
		}
		monitor.worked(1);
		
		return newDataModel;
	}
	
	/**
	 * Finds the icon for a given icon key
	 * 
	 * @param session
	 * @param iconKey
	 * @return
	 */
	public static Icon findIcon(Session session, String iconKey) {
		if (iconKey == null) return null;
		Icon icon = QueryFactory.buildQuery(session, Icon.class, 
				new Object[] {"conservationArea.uuid", ConservationArea.MULTIPLE_CA}, //$NON-NLS-1$
				new Object[] {"keyId", iconKey}).uniqueResult(); //$NON-NLS-1$
		return icon;
		
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
		clone.setIcon(findIcon(session,findIconKey(toClone, cas, session)));
		
		//clone the labels
		clone.setName(toClone.getName());
			
		if (toClone.getAllAttributes() != null){
			clone.setAllAttributes(new ArrayList<CategoryAttribute>());
			for (CategoryAttribute attribute : toClone.getAllAttributes()){
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
						link.setIsActive(attribute.getIsActive());
						link.setOrder(attribute.getOrder());
						link.setIsRoot(attribute.getIsRoot());
						clone.getAllAttributes().add(link);
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
			
		}	
		return clone;
	}

	
	private void mergeAttributeAggregations(Attribute a, ConservationArea[] ca, Session session){
		List<Aggregation> aggs = new ArrayList<Aggregation>(a.getAggregations());
		if (aggs.size() == 0){
			return;
		}
		
		String hql = "FROM Attribute a where a.keyId = :key and a.conservationArea in (:ca)"; //$NON-NLS-1$
		Query<Attribute> q = session.createQuery(hql, Attribute.class);
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
		Query<Attribute> q = session.createQuery(hql, Attribute.class);
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
		Query<Long> q = session.createQuery(hql, Long.class);
		q.setParameter("key", c.getHkey());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$
		Long cnt = q.list().get(0);
		if (cnt == ca.length){
			//this category exists in each conservation area so we keep it
			return true;
		}
		return false;
	}
	
	private String findIconKey (Category c, ConservationArea[] ca, Session session){
		if (c.getIcon() != null) return c.getIcon().getKeyId();
		
		String hql = "SELECT distinct icon.keyId FROM Category WHERE hkey = :key AND conservationArea in (:ca) AND icon is not null";//$NON-NLS-1$
		Query<String> q = session.createQuery(hql, String.class);
		q.setParameter("key", c.getHkey());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$

		//could potential find more than one if they are different per ca
		//if this is the case pick one to use
		List<String> keys = q.list();
		if (keys.size() > 0) return keys.get(0);
		return null;
	}
	
	private String findIconKey (Attribute a, ConservationArea[] ca, Session session){
		if (a.getIcon() != null) return a.getIcon().getKeyId();
		
		String hql = "SELECT distinct icon.keyId FROM Attribute WHERE keyId = :key AND conservationArea in (:ca) AND icon is not null";//$NON-NLS-1$
		Query<String> q = session.createQuery(hql, String.class);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$
		
		//could potential find more than one if they are different per ca
		//if this is the case pick one to use
		List<String> keys = q.list();
		if (keys.size() > 0) return keys.get(0);
		return null;
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
		
		Query<Tuple> q = session.createQuery(hql, Tuple.class);
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
