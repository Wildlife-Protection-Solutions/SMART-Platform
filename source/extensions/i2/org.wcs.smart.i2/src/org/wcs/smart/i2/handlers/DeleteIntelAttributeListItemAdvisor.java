/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.handlers;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;

/**
 * Validates the deletion of attribute list items
 * @author Emily
 *
 */
public class DeleteIntelAttributeListItemAdvisor implements IDeleteAdvisor {

	public DeleteIntelAttributeListItemAdvisor() {
	}

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof IntelAttributeListItem)){
			return "Object not an IntelAttributeListItem object.  Cannot delete.";
		}
		IntelAttributeListItem attribute = (IntelAttributeListItem) object;
		
		Long linkCnt =  
				(Long)session.createCriteria(IntelEntityAttributeValue.class)
			.add(Restrictions.eq("attributeListItem", attribute)) //$NON-NLS-1$
			.setProjection(Projections.rowCount())
			.uniqueResult();
		
		if (linkCnt > 0){
			return MessageFormat.format("This attribute list item is referenced by {0} entities.  This references must be removed before you can delete this item. ", linkCnt);
		}
		
		//TODO: relationship attributes
		return null;
	}

}
