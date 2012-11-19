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
package org.wcs.smart.patrol.internal.advisors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

/**
 * Advisor for deleting list items
 * from a list attribute.
 * Validates patrol related uses of list items.
 * 
 * @author egouge
 *
 */
public class AttributeListItemDMAdvisor implements IDeleteAdvisor {


	/**
	 * <p>
	 * Validates that that attribute list item
	 * is not used in any observations.
	 * <p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.AttributeListItem, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof AttributeListItem)){
			return "Object not of type AttributeListItem. Can not delete.";
		}
		AttributeListItem item = (AttributeListItem)object;
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

}

