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
package org.wcs.smart.entity;

import java.text.MessageFormat;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.entity.ccca.EntityTypeCcaaManager;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
/**
 * Entity Type database utilities.
 * 
 * @author Emily
 *
 */
public class EntityHibernateManager {

	/**
	 * Find the entity type with the given key 
	 * 
	 * @param entityType entity type key 
	 * @param session
	 * @return
	 */
	public static EntityType getEntityType(String entityType,  Session session){
		if (SmartDB.isMultipleAnalysis()){
			return EntityTypeCcaaManager.getInstance().findType(entityType);
		}
		org.hibernate.Query hq = session.createQuery("FROM EntityType WHERE keyId = :key and conservationArea = :ca"); //$NON-NLS-1$
		hq.setParameter("key", entityType); //$NON-NLS-1$
		hq.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> data = hq.list();
		if (data.size() != 1){
			throw new RuntimeException(MessageFormat.format(Messages.EntityHibernateManager_InvalidEntityTypeKey, new Object[]{entityType}));
		}
		EntityType et = (EntityType) data.get(0);
		return et;
		
	}
	
	/**
	 * Finds the entity attribute with the associated key.
	 * @param entityKey entity type key
	 * @param entityAttributeKey entity attribute key 
	 * @param session
	 * 
	 * @return
	 * @throws Exception
	 */
	public static EntityAttribute getEntityAttribute(String entityKey,  String entityAttributeKey, Session session) throws Exception{
		if (SmartDB.isMultipleAnalysis()){
			EntityType et = EntityTypeCcaaManager.getInstance().findType(entityKey);
			for (EntityAttribute ea : et.getAttributes()){
				if (ea.getKeyId().equals(entityAttributeKey)){
					return ea;
				}
			}
			return null;
		}
		Query q = session.createQuery("From EntityAttribute where entityType.conservationArea.uuid = :ca and entityType.keyId = :entitykey and keyId = :key"); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$
		q.setParameter("key", entityAttributeKey); //$NON-NLS-1$
		q.setParameter("entitykey", entityKey); //$NON-NLS-1$
		q.setCacheable(true);
		@SuppressWarnings("unchecked")
		List<EntityAttribute> results = q.list();
		if (results.size() != 1 ){
			throw new Exception(MessageFormat.format(Messages.EntityHibernateManager_InvalidEntityAttribute, new Object[]{entityAttributeKey, entityKey}));
		}else{
			return (EntityAttribute) results.get(0);
		}
		
	}
	
	/**
	 * Finds all active entity types. Lazily loads all attribute names.
	 * @return
	 */
	public static List<EntityType> getActiveEntityTypes(){
		if (SmartDB.isMultipleAnalysis()){
			return EntityTypeCcaaManager.getInstance().getAllEntityTypes();
		}
		
		
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Query q = session.createQuery("FROM EntityType WHERE conservationArea = :ca and status = :stat"); //$NON-NLS-1$
			q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			q.setParameter("stat", EntityType.Status.ACTIVE); //$NON-NLS-1$
			
			List<EntityType> items = q.list();
			for (EntityType t : items){
				t.getDmAttribute().getName();
				for (EntityAttribute ea : t.getAttributes()){
					ea.getName();
					ea.getDmAttribute().getType();
				}
			}
			return items;
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
	}
	
	/**
	 * Finds all active entity types.
	 * @return
	 */
	public static List<EntityType> getEntityTypes(Session session){
		if (SmartDB.isMultipleAnalysis()){
			return EntityTypeCcaaManager.getInstance().getAllEntityTypes();
		}
		Query q = session.createQuery("FROM EntityType WHERE conservationArea = :ca "); //$NON-NLS-1$
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<EntityType> items = q.list();
		return items;
	}
}
