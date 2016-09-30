package org.wcs.smart.i2;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;

public enum EntityManager {
	INSTANCE;
	
	public void deleteEntity(IntelEntity entity, Session session){
		//TODO:
		session.delete(entity);
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
