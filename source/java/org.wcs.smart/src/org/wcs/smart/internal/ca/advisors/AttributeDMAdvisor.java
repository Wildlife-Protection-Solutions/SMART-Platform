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
package org.wcs.smart.internal.ca.advisors;

import java.text.MessageFormat;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.internal.Messages;

 
/**
 * Advisor for deleting attributes from the data model.
 * @author egouge
 *
 */
public class AttributeDMAdvisor implements IDeleteAdvisor {


	/**
	 * <p>
	 * Validates the attribute is not associated with
	 * any categories.
	 * </p>
	 * @see org.wcs.smart.ca.datamodel.IDataModelAdvisor#canDelete(org.wcs.smart.ca.datamodel.Attribute, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Attribute)){
			return Messages.AttributeDMAdvisor_Error_NotAttribute;
		}
		Attribute attribute = (Attribute)object;
		if (attribute.getUuid() == null){
			return null;
		}
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<CategoryAttribute> query = cb.createQuery(CategoryAttribute.class);
		Root<CategoryAttribute> root = query.from(CategoryAttribute.class);
		query.where(cb.equal(root.get("id").get("attribute"), attribute)); //$NON-NLS-1$ //$NON-NLS-2$
		
		List<CategoryAttribute> items = session.createQuery(query).getResultList();
		if (items.size() == 0){
			return null;
		}else{
			StringBuilder sb = new StringBuilder();
			sb.append(MessageFormat.format(Messages.AttributeDMAdvisor_Error_AttributeReferenced2, new Object[]{attribute.getName()}) );
			for (CategoryAttribute it : items){
				sb.append(it.getCategory().getFullCategoryName() + "\n"); //$NON-NLS-1$
			}
			return sb.toString();
		}
	}
}
