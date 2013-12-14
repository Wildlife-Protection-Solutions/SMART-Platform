package org.wcs.smart.entity.model;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;

public class EntityFilter {

	
	public Query buildQuery(Session session){
	
		Query q = session.createQuery("SELECT uuid, id, name FROM Entity WHERE conservationArea = :ca");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		
		return q;
	}
}
