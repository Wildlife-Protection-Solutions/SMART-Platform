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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;

 
/**
 * Advisor for deleting attributes from the data model.
 * Validates patrol related uses of attributes.
 * @author egouge
 *
 */
public class AttributeDMAdvisor implements IDeleteAdvisor {


	/**
	 * <p>
	 * Validates that no observations
	 * are associated with the given attribute.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Attribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Attribute)){
			return "Object not of type Attribute. Can not delete.";
		}
		Attribute attribute = (Attribute)object;
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
}
