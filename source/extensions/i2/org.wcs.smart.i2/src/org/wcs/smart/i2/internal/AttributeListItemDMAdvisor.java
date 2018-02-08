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
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelObservationAttribute;

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
			return "Invalid Object";
		}
		AttributeListItem item = (AttributeListItem)object;
		if (item.getUuid() == null) return null;
		
		long cnt = QueryFactory.buildCountQuery(session, IntelObservationAttribute.class,  new Object[] {"attributeListItem", item}); //$NON-NLS-1$
		if (cnt == 0){
	     	return null;
		 }
		return MessageFormat.format("Attribute list item is associated with {0, number, integer} intelligence observations.  These observations must be removed before the attribute list item can be deleted.", cnt);
			

	}

}

