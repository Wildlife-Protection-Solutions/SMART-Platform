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
package org.wcs.smart.patrol;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelAdvisor;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Data model advisor for validating
 * data model changes on patrol data.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolDataModelAdvisor implements IDataModelAdvisor{

	/**
	 * <p>Validates that no observations
	 * are made against the given category or any of it's children
	 * categories.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Category, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Category category, Session session) {
		if (category.getUuid() == null) return null;
		Criteria query = session.createCriteria(WaypointObservation.class);
		query.add(Restrictions.eq("category", category));
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			return "The category is associated with " + cnt + " observations.  These observations must be removed before the category can be deleted.";
		}
		
		for(Category kid : category.getChildren()){
			String canDeleteKid = canDelete(kid, session);
			if (canDeleteKid != null){
				return canDeleteKid;
			}
		}
		return null;
		
	}

	/**
	 * <p>
	 * Validates that no observations
	 * are associated with the given attribute.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Attribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Attribute attribute, Session session) {
		if (attribute.getUuid() == null ) return null;
		Criteria query = session.createCriteria(WaypointObservationAttribute.class);
		query.add(Restrictions.eq("id.attribute", attribute));
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt == 0){
			return null;
		}
		return "The attribute is associated in " + cnt + " observations.  These observations must be removed before the attribute can be deleted.";
	}

	/**
	 * <p>Validates that no observations
	 * have both the given category and given attribute or
	 * a child of the category and the given attribute
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.CategoryAttribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(CategoryAttribute categoryAttribute, Session session) {
		if (categoryAttribute.getCategory().getUuid() == null 
				|| categoryAttribute.getAttribute().getUuid() == null ){
			return null;
		}
		Query query = session.createQuery("" +
				"SELECT count(*) FROM WaypointObservation wo join wo.attributes woa join wo.category as cat " +
				"WHERE cat.hkey like :categoryhkey and woa.id.attribute = :attribute");
		query.setParameter("categoryhkey", categoryAttribute.getCategory().getHkey() + "%");
		query.setParameter("attribute", categoryAttribute.getAttribute());
		long cnt = ((Long)query.list().get(0));
		if (cnt != 0){
			return "The category/attribute relationship is used associated with " + cnt + " observations.  These observations must be removed before the attribute can be deleted.";
		}
		return null;
		
		
	}

	/**
	 * <p>
	 * Validates that that attribute list item
	 * is not used in any observations.
	 * <p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.AttributeListItem, org.hibernate.Session)
	 */
	@Override
	public String canDelete(AttributeListItem item, Session session) {
		if (item.getUuid() == null) return null;
		Criteria query = session.createCriteria(WaypointObservationAttribute.class);
		query.add(Restrictions.eq("attributeListItem", item));
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt == 0){
			return null;
		}
		return "Attribute list item is associated with " + cnt + " observations.  These observations must be removed before the attribute list item can be deleted.";

	}

	/**
	 * <p>
	 * Validates that the attribute node item is not used
	 * in any observations.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.AttributeTreeNode, org.hibernate.Session)
	 */
	@Override
	public String canDelete(AttributeTreeNode node, Session session) {
		if (node.getUuid() == null) return null;
		Criteria query = session.createCriteria(WaypointObservationAttribute.class);
		query.add(Restrictions.eq("attributeTreeNode", node));
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt == 0){
			return null;
		}
		return "Attribute node item is associated with " + cnt + " observations.  These observations must be removed before the attribute node item can be deleted.";

	}

}
