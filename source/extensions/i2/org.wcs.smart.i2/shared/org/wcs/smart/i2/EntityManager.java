package org.wcs.smart.i2;

import org.hibernate.Session;
import org.wcs.smart.i2.model.IntelEntity;

public enum EntityManager {
	INSTANCE;
	
	public void deleteEntity(IntelEntity entity, Session session){
		//TODO:
		session.delete(entity);
	}
}
