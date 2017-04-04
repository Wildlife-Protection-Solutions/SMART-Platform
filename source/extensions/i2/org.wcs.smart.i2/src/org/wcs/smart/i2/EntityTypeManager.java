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
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Tools for managing entity types
 * 
 * @author Emily
 *
 */
public enum EntityTypeManager {
	INSTANCE;
	
	private EntityTypeManager(){
		
	}
	
	/**
	 * Loads all entity types and sorts by name
	 * @param session
	 * @param ca
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<IntelEntityType> getEntityTypes(Session session, ConservationArea ca){
		List<IntelEntityType> types = session.createCriteria(IntelEntityType.class)
			.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.list();
		types.sort((IntelEntityType a, IntelEntityType b) -> Collator.getInstance().compare(a.getName(), b.getName()));
		return types;
	}
	
	public void canDelete(IntelEntityType type, Session session) throws Exception{
		if (!DeleteManager.canDelete(type, session)){
			throw new Exception(Messages.EntityTypeManager_DeleteError);
		}
	}
	
	/**
	 * Deletes an entity type an all associated data (relationships, entities, record links etc)
	 * 
	 * @param type
	 * @param session
	 * @throws Exception
	 */
	public void deleteEntityType(IntelEntityType type, Session session) throws Exception{
		
		//update relationships references to null
		Query q = session.createQuery("UPDATE IntelRelationshipType SET sourceEntityType = null where sourceEntityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("UPDATE IntelRelationshipType SET targetEntityType = null where targetEntityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity attribute values
		q = session.createQuery("delete from IntelEntityAttributeValue ieav where ieav.id.entity in (FROM IntelEntity WHERE entityType = :type)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all relationship attribute values
		q = session.createQuery("delete from IntelEntityRelationshipAttributeValue ii where ii.id.relationship in (FROM IntelEntityRelationship r WHERE  r.sourceEntity in (FROM IntelEntity WHERE entityType = :type) or r.targetEntity in (FROM IntelEntity WHERE entityType = :type2))"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.setParameter("type2", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all relationships
		q = session.createQuery("delete from IntelEntityRelationship ii where ii.sourceEntity in (FROM IntelEntity WHERE entityType = :type) or ii.targetEntity in (FROM IntelEntity WHERE entityType = :type2)"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.setParameter("type2", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity attachments
		q = session.createQuery("delete from IntelEntityAttachment ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity records
		q = session.createQuery("delete from IntelEntityRecord ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all locations
		q = session.createQuery("delete from IntelEntityLocation ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all links to working sets
		q = session.createQuery("delete from IntelWorkingSetEntity ii where ii.id.entity in (FROM IntelEntity WHERE entityType = :type) "); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all entity 		
		q = session.createQuery("delete from IntelEntity WHERE entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();

		//delete all entity type attribute 		
		q = session.createQuery("delete from IntelEntityTypeAttribute ii WHERE ii.id.entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();

		//delete all attribute groups 		
		q = session.createQuery("delete from IntelEntityTypeAttributeGroup WHERE entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all record source attribute values
		q = session.createQuery("delete from IntelRecordAttributeValue ii where ii.attribute in ( FROM IntelRecordSourceAttribute ii where ii.entityType = :type )"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete all record source attributes
		q = session.createQuery("delete from IntelRecordSourceAttribute ii where ii.entityType = :type"); //$NON-NLS-1$
		q.setParameter("type", type); //$NON-NLS-1$
		q.executeUpdate();
		
		session.delete(type);
	}
}
