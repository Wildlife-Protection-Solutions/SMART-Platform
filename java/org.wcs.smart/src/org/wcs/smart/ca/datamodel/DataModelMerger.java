package org.wcs.smart.ca.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Class to merge datamodels. 
 * @author Emily
 *
 */
public class DataModelMerger {

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
	public DataModel mergeDataModels(ConservationArea[] cas, Session session){
		if (cas == null || cas.length < 2){
			throw new IllegalStateException("more than 1 conservation area must be provided");//$NON-NLS-1$
		}
		/* load data model for first ca found */
		DataModel dm = HibernateManager.loadDataModel(cas[0], session);
		
		/* root categories */
		DataModel newDataModel = new DataModel(cas[0], new ArrayList<Category>(), new ArrayList<Attribute>());
		for (Category c : dm.getCategories()){
			if (canKeep(c, cas, session)){
				newDataModel.addRootCategories(c);
			}
		}
		newDataModel.getActiveCategories().addAll(newDataModel.getCategories());
		
		/* attributes */
		for (Attribute a : dm.getAttributes()){
			if (canKeep(a, cas, session)){
				a.setName(a.findName(cas[0].getDefaultLanguage()));
				newDataModel.getAttributes().add(a);
			}
		}
		
		/* child categories and attributes */
		for (Category c : dm.getCategories()){
			processChildren(c, cas, session);
		}
		return newDataModel;
	}
	
	/*
	 * processes all children of a given category
	 */
	private void processChildren(Category parent, ConservationArea[] ca, Session session){
		
		//TODO: I need to deal with the name/language issue here
		parent.setName(parent.findName(ca[0].getDefaultLanguage()));
		if(parent.getChildren() != null){
			List<Category> kids = new ArrayList<Category>();
			for (Category kid : parent.getChildren()){
				if (canKeep(kid, ca, session)){
					kids.add(kid);
				}
			}
			
			parent.getChildren().clear();
			parent.getChildren().addAll(kids);
			parent.setActiveChildren(kids);
		}
		
		if (parent.getAttributes() != null){
			List<CategoryAttribute> attributes = new ArrayList<CategoryAttribute>();
			for (CategoryAttribute categoryAttribute : parent.getAttributes()){
				if (canKeep(categoryAttribute, ca, session)){
					attributes.add(categoryAttribute);
				}
			}
			parent.setAttributes(attributes);
		}

		if (parent.getChildren() != null){
			for (Category kid : parent.getChildren()){
				processChildren(kid, ca, session);
			}
		}
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
		String hql = "SELECT count(*) FROM Attribute WHERE keyId = :key AND conservationArea in (:ca)";//$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("key", a.getKeyId());//$NON-NLS-1$
		q.setParameterList("ca", ca);//$NON-NLS-1$
		Long cnt = (Long) q.list().get(0);
		if (cnt == ca.length){
			//this category exists in each conservation area so we keep it
			return true;
		}
		return false;
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
