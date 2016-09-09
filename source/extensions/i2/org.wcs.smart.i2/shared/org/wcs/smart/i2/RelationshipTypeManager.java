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
package org.wcs.smart.i2;

import java.text.Collator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;

/**
 * Tools for managing relationship types
 * 
 * @author Emily
 *
 */
public enum RelationshipTypeManager {
	INSTANCE;
	
	private RelationshipTypeManager(){
		
	}
	
	/**
	 * Loads all relationship types and sorts by name
	 * @param session
	 * @param ca
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<IntelRelationshipType> getRelationshipTypes(Session session, ConservationArea ca){
		List<IntelRelationshipType> types = session.createCriteria(IntelRelationshipType.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.list();
		types.sort((IntelRelationshipType a, IntelRelationshipType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	public void canDelete(IntelRelationshipType type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception("Unknown error occurrs while deleteing relationship type.");
		}
	}
	
	/**
	 * Deletes a relationship type an all associated data (relationships, attributes etc.)
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteRelationshipType(IntelRelationshipType type, Session session) throws Exception{
		//delete all relationship attribute values
		Query q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.relationship in (FROM IntelEntityRelationship r WHERE  r.relationshipType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all relationships
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.relationshipType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();		
		
		//delete relationship attributes 
		q = session.createQuery("delete from IntelRelationshipTypeAttribute ii WHERE ii.id.relationshipType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		session.delete(type);
	}
}
