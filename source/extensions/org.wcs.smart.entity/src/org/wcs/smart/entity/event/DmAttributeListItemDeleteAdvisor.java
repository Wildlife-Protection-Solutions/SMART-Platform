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
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttributeValue;

/**
 * Delete advisor for deleting attribute list items
 * from the attribute of an entity type.  If an entity
 * references the list item, it cannot be deleted.
 * 
 * @author Emily
 *
 */
public class DmAttributeListItemDeleteAdvisor implements IDeleteAdvisor {


	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof AttributeListItem)){
			return "Invalid object type.";
		}
		
		//if entity is represented by list item cannot delete
		AttributeListItem toDelete = (AttributeListItem)object;
		Query q = session.createQuery("FROM Entity WHERE attributeListItem = :todelete");
		q.setParameter("todelete", toDelete);
		List<?> results = q.list();
		if (results.size() > 0){
			//attribute associated with an entity and cannot be deleted
			return MessageFormat.format("The attribute list item is associated with the Entity ''{0}''.  This list item cannot be removed until the entity is removed.", 
					new Object[]{((Entity)results.get(0)).getId()});	
		}
		
		
		//if an entity attribute is represented by the list item cannot delete
		q = session.createQuery("FROM EntityAttributeValue v WHERE v.attributeListItem = :todelete");
		q.setParameter("todelete", toDelete);
		results = q.list();
		if (results.size() > 0){
			EntityAttributeValue v1 = (EntityAttributeValue) results.get(0);
			//attribute associated with an entity and cannot be deleted
			return MessageFormat.format("The attribute list item is the value for the attribute ''{0}'' for entity ''{1}''.  This list item cannot be removed until the relationship is removed.", 
					new Object[]{v1.getEntityAttribute().getName(), v1.getEntity().getId()});	
		}
		return null;
	}

}
