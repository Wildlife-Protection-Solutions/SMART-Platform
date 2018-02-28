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
package org.wcs.smart.i2.internal;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.CategoryAttribute;

/**
 * Advisor for deleting attributes associate
 * with a specific category from the data model
 * Validates patrol related uses of this category/attribute.
 * @author egouge
 *
 */
public class CategoryAttributeDMAdvisor implements IDeleteAdvisor{

	/**
	 * <p>Validates that no observations
	 * have both the given category and given attribute or
	 * a child of the category and the given attribute
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.CategoryAttribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof CategoryAttribute)){
			return "Invalid Object"; //$NON-NLS-1$
		}
		CategoryAttribute categoryAttribute = (CategoryAttribute)object;
		if (categoryAttribute.getCategory().getUuid() == null 
				|| categoryAttribute.getAttribute().getUuid() == null ){
			return null;
		}
		Query<?> query = session.createQuery(
				"SELECT count(*) FROM IntelObservation wo join wo.observationAttributes woa join wo.category as cat " + //$NON-NLS-1$
				"WHERE cat.hkey like :categoryhkey and woa.id.attribute = :attribute"); //$NON-NLS-1$
		query.setParameter("categoryhkey", categoryAttribute.getCategory().getHkey() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
		query.setParameter("attribute", categoryAttribute.getAttribute()); //$NON-NLS-1$
		long cnt = ((Long)query.list().get(0));
		if (cnt != 0){
			return MessageFormat.format(
					Messages.CategoryAttributeDMAdvisor_CatAttUsedObs,
					cnt);
		}
		return null;
	}

	
}
