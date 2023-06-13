/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.IDmEditAdvisor;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Edit advisor to warn users that attributes associated with
 * entity types should not be modified.
 * 
 * @author Emily
 *
 */
public class DmAttributeEditAdvisor implements IDmEditAdvisor {

	public DmAttributeEditAdvisor() {
	}

	@Override
	public String canEdit(Attribute attribute, Session session) {
		//find all entity types associated with attribute
		Query<IntelEntityType> q = session.createQuery("FROM IntelEntityType where dmAttribute = :att", IntelEntityType.class); //$NON-NLS-1$
		q.setParameter("att", attribute); //$NON-NLS-1$
		List<IntelEntityType> items = q.list();
		if (items.size()  > 0){
			IntelEntityType et = (IntelEntityType)items.get(0);
			String msg = Messages.DmAttributeEditAdvisor_ManagedAttributeMsg;
			return MessageFormat.format(msg, new Object[]{et.getName()});			
		}
		
		return null;
	}

	@Override
	public String canEdit(Category category, Session session) {
		return null;
	}

}
