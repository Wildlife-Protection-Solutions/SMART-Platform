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
package org.wcs.smart.entity.event;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;

/**
 * Disallows the deleting of a datamodel attribute if
 * it is associated with an entity type.
 * 
 * @author Emily
 *
 */
public class DmAttributeDeleteAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Attribute)){
			return Messages.DmAttributeDeleteAdvisor_InvalidType;
		}
		
		//check if this is the attribute associated with any entity type
		Attribute toDelete = (Attribute)object;
		Query q = session.createQuery("FROM EntityType WHERE dmAttribute = :todelete"); //$NON-NLS-1$
		q.setParameter("todelete", toDelete); //$NON-NLS-1$
		List<?> results = q.list();
		if (results.size() > 0){
			//attribute associated with an entity and cannot be deleted
			return MessageFormat.format(Messages.DmAttributeDeleteAdvisor_CannotDeleteAttributeEtAssociation, 
					new Object[]{((EntityType)results.get(0)).getName()});	
		}
		
		//check if this is an attribute used to describe entities
		q = session.createQuery("FROM EntityAttribute WHERE dmAttribute = :todelete"); //$NON-NLS-1$
		q.setParameter("todelete", toDelete); //$NON-NLS-1$
		results = q.list();
		if (results.size() > 0){
			//attribute associated with an entity and cannot be deleted
			return MessageFormat.format(Messages.DmAttributeDeleteAdvisor_CannotDeleteAttributeAssociation, 
					new Object[]{((EntityAttribute)results.get(0)).getEntityType().getName()});	
		}
		return null;
	}

}
