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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
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
		String query = "SELECT count(*) FROM IntelEntity e join e.attributes as v where e.conservationArea = :ca and e.entityType = :type and v.id.attribute = :attribute ";
		switch(attribute.getType()){
			case BOOLEAN:
			case POSITION:
				return false;	//don't both checking we will always have duplicates
			case DATE:
				query += " and v.stringValue = :test";
				break;
			case LIST:
				query += " and v.attributeListItem = :test";
				break;
			case NUMERIC:
				query += " and v.numberValue = :test";
				break;
			case TEXT:
				query += " and v.stringValue = :test "; 
				break;
		}
		
		if (currentEntity != null){
			query += " AND e.uuid != :entity ";
		}
		Query hql = session.createQuery(query);
		hql.setParameter("attribute", type.getIdAttribute());
		hql.setParameter("ca", ca);
		hql.setParameter("type", type);
		switch(attribute.getType()){
			case BOOLEAN: 
			case POSITION:
				return false; // not supported
			case DATE:
				hql.setParameter("test", ((java.sql.Date)newId).toString());
				break;
			case LIST:
			case NUMERIC:
			case TEXT:
				hql.setParameter("test", newId);
				break;
				
		}
		if (currentEntity != null){
			hql.setParameter("entity",  currentEntity);
		}

		long cnt = (Long) hql.uniqueResult();
		if (cnt > 0) return true;
		return false;
		
	}
	
	
	public void deleteEntity(IntelEntity entity, Session session) throws Exception{

		//delete all record attribute links 
		Query q = session.createQuery("DELETE FROM IntelRecordAttributeValueList where id.elementUuid = :entityUuid"); 
		q.setParameter("entityUuid", entity.getUuid());
		q.executeUpdate();

				
		//delete all entity relationships attributes
		q = session.createQuery("DELETE FROM IntelEntityRelationshipAttributeValue where id.relationship IN (FROM IntelEntityRelationship where (id.sourceEntity = :srcentity or id.targetEntity = :trgentity))"); 
		q.setParameter("srcentity", entity);
		q.setParameter("trgentity", entity);
		q.executeUpdate();

		
		//delete all entity relationships 
		q = session.createQuery("DELETE FROM IntelEntityRelationship where (id.sourceEntity = :srcentity or id.targetEntity = :trgentity)");
		q.setParameter("srcentity", entity);
		q.setParameter("trgentity", entity);
		q.executeUpdate();

		//delete all working set links 
		q = session.createQuery("DELETE FROM IntelWorkingSetEntity where id.entity = :entity");
		q.setParameter("entity", entity);
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
	@SuppressWarnings("unchecked")
	public List<IntelEntityLocation> getEntityLocations(Session session, UUID entityUuid, Date[] dFilter){
		List<IntelEntityLocation> alllocations = null;
		
		if (dFilter != null && dFilter.length == 2 && dFilter[0] != null && dFilter[1] != null){
			Query q = session.createQuery("FROM IntelEntityLocation WHERE id.entity.uuid = :uuid and id.location.dateTime between :d1 and :d2");
			q.setParameter("uuid", entityUuid);
			q.setParameter("d1", dFilter[0]);
			q.setParameter("d2", dFilter[1]);
			alllocations = q.list();
		}else{
			alllocations = session.createCriteria(IntelEntityLocation.class)
				.add(Restrictions.eq("id.entity.uuid", entityUuid))
				.list();
		}
		return alllocations;
	}
}
