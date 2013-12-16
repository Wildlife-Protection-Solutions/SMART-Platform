package org.wcs.smart.entity.model;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;

public class EntityFilter {

	
	public Query buildQuery(Session session){
	
		Query q = session.createQuery("SELECT e.uuid, e.id, e.name FROM EntityType e WHERE e.conservationArea = :ca");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		
		return q;
	}
}
