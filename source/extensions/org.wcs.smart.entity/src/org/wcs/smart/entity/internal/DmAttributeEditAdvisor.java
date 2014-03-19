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
package org.wcs.smart.entity.internal;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.IDmEditAdvisor;
import org.wcs.smart.entity.model.EntityType;

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
		Query q = session.createQuery("FROM EntityType where dmAttribute = :att"); //$NON-NLS-1$
		q.setParameter("att", attribute); //$NON-NLS-1$
		List<?> items = q.list();
		if (items.size()  > 0){
			EntityType et = (EntityType)items.get(0);
			return MessageFormat.format(
					Messages.DmAttributeEditAdvisor_CannotEditAttribute, new Object[]{et.getName()});			
		}
		
		return null;
	}

	@Override
	public String canEdit(Category category, Session session) {
		return null;
	}

}
