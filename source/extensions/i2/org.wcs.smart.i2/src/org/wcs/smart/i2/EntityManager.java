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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Entity manager
 * 
 * @author Emily
 *
 */
public enum EntityManager {
	
	INSTANCE;
	
	/**
	 * Checks for another entity of the same type in the same conservation area with the provided
	 * id.
	 * 
	 * @param newId
	 * @param type
	 * @param ca
	 * @param session
	 * @return
	 */
	public boolean isDuplicateId(Object newId, IntelEntityType type, ConservationArea ca, Session session, UUID currentEntity){
		if (newId == null) return false;
		
		IntelAttribute attribute = type.getIdAttribute();
		String query = "SELECT count(*) FROM IntelEntity e join e.attributes as v where e.conservationArea = :ca and e.entityType = :type and v.id.attribute = :attribute "; //$NON-NLS-1$
		switch(attribute.getType()){
			case BOOLEAN:
			case POSITION:
				return false;	//don't both checking we will always have duplicates
			case DATE:
				query += " and v.stringValue = :test"; //$NON-NLS-1$
				break;
			case LIST:
				query += " and v.attributeListItem = :test"; //$NON-NLS-1$
				break;
			case EMPLOYEE:
				query += " and v.employee = :test"; //$NON-NLS-1$
				break;
			case NUMERIC:
				query += " and v.numberValue = :test"; //$NON-NLS-1$
				break;
			case TEXT:
				query += " and v.stringValue = :test ";  //$NON-NLS-1$
				break;
		}
		
		if (currentEntity != null){
			query += " AND e.uuid != :entity "; //$NON-NLS-1$
		}
		Query<?> hql = session.createQuery(query);
		hql.setParameter("attribute", type.getIdAttribute()); //$NON-NLS-1$
		hql.setParameter("ca", ca); //$NON-NLS-1$
		hql.setParameter("type", type); //$NON-NLS-1$
		switch(attribute.getType()){
			case BOOLEAN: 
			case POSITION:
				return false; // not supported
			case DATE:
				hql.setParameter("test", ((java.sql.Date)newId).toString()); //$NON-NLS-1$
				break;
			case LIST:
			case NUMERIC:
			case TEXT:
			case EMPLOYEE:
				hql.setParameter("test", newId); //$NON-NLS-1$
				break;
				
		}
		if (currentEntity != null){
			hql.setParameter("entity",  currentEntity); //$NON-NLS-1$
		}

		long cnt = (Long) hql.uniqueResult();
		if (cnt > 0) return true;
		return false;
		
	}
	
	
	public void deleteEntity(IntelEntity entity, Session session) throws Exception{

		//first see if the entity is linked to a data model attribute list item
		//then determine if that list item can be deleted
		//if ok then delete list item othrwise throw exception
		
		if (entity.getDmAttributeListItem() != null) {
			
			try {
				if (!DeleteManager.canDelete(entity.getDmAttributeListItem(), session)) {
					throw new Exception("Cannot delete linked data model list item"); //$NON-NLS-1$
				}
			}catch (Exception ex) {
				throw new Exception("Cannot delete linked data model list item: " + ex.getMessage(), ex); //$NON-NLS-1$
			}
			
			session.delete(entity.getDmAttributeListItem());
		}
		
		//delete all record attribute links 
		Query<?> q = session.createQuery("DELETE FROM IntelRecordAttributeValueList where id.elementUuid = :entityUuid");  //$NON-NLS-1$
		q.setParameter("entityUuid", entity.getUuid()); //$NON-NLS-1$
		q.executeUpdate();

				
		//delete all entity relationships attributes
		q = session.createQuery("DELETE FROM IntelEntityRelationshipAttributeValue where id.relationship IN (FROM IntelEntityRelationship where (id.sourceEntity = :srcentity or id.targetEntity = :trgentity))");  //$NON-NLS-1$
		q.setParameter("srcentity", entity); //$NON-NLS-1$
		q.setParameter("trgentity", entity); //$NON-NLS-1$
		q.executeUpdate();

		
		//delete all entity relationships 
		q = session.createQuery("DELETE FROM IntelEntityRelationship where (id.sourceEntity = :srcentity or id.targetEntity = :trgentity)"); //$NON-NLS-1$
		q.setParameter("srcentity", entity); //$NON-NLS-1$
		q.setParameter("trgentity", entity); //$NON-NLS-1$
		q.executeUpdate();

		//delete all working set links 
		q = session.createQuery("DELETE FROM IntelWorkingSetEntity where id.entity = :entity"); //$NON-NLS-1$
		q.setParameter("entity", entity); //$NON-NLS-1$
		q.executeUpdate();
		
		//delete entity
		session.delete(entity);
		
		if (entity.getEntityAttachments() != null){
			for (IntelEntityAttachment attachment : entity.getEntityAttachments()){
				if (AttachmentManager.INSTANCE.canDelete(attachment.getAttachment(), session)){
					session.delete(attachment.getAttachment());
				}
			}
		}
	}
	

	/**
	 * Finds all entity location that occur during the two dates provided the dFilter array.
	 * If not dates provided returns all records.
	 * 
	 * @param session
	 * @param dFilter
	 * @return
	 */
	public List<IntelEntityLocation> getEntityLocations(Session session, UUID entityUuid, Date[] dFilter){
		List<IntelEntityLocation> alllocations = null;
		
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			Query<IntelEntityLocation> q = session.createQuery("FROM IntelEntityLocation WHERE id.entity.uuid = :uuid and id.location.dateTime between :d1 and :d2", IntelEntityLocation.class); //$NON-NLS-1$
			q.setParameter("uuid", entityUuid); //$NON-NLS-1$
			q.setParameter("d1", dFilter[0]); //$NON-NLS-1$
			q.setParameter("d2", dFilter[1]); //$NON-NLS-1$
			alllocations = q.list();
		}else{
			alllocations = QueryFactory.buildQuery(session, IntelEntityLocation.class, new Object[] {"id.entity.uuid", entityUuid}).getResultList(); //$NON-NLS-1$
		}
		return alllocations;
	}
}
