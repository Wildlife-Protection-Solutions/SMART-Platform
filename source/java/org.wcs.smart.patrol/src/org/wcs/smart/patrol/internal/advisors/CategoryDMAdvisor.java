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

import java.text.MessageFormat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.WaypointObservation;

/**
 * Advisor for deleting categories from the data model.
 * Validates patrols not using the category.
 * 
 * @author egouge
 *
 */
public class CategoryDMAdvisor implements IDeleteAdvisor {


	/**
	 * <p>Validates that no observations
	 * are made against the given category or any of it's children
	 * categories.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Category, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Category)){
			return Messages.CategoryDMAdvisor_Error_InvalidObjectType;
		}
		Category category = (Category)object;
		if (category.getUuid() == null) return null;
		Criteria query = session.createCriteria(WaypointObservation.class);
		query.add(Restrictions.eq("category", category)); //$NON-NLS-1$
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			return MessageFormat.format(
					Messages.CategoryDMAdvisor_DeleteError,
					new Object[]{cnt });
		}
		if (category.getChildren() != null){
			for(Category kid : category.getChildren()){
				String canDeleteKid = canDelete(kid, session);
				if (canDeleteKid != null){
					return canDeleteKid;
				}
			}
		}
		return null;
	}
}

