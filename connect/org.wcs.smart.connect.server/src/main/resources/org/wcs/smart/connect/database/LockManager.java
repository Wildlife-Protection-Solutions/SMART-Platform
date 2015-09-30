package org.wcs.smart.connect.database;

import org.hibernate.Session;
import org.wcs.smart.connect.model.ConservationAreaInfo;

public enum LockManager {

	INSTANCE;
	public void lockDatabase(Session session, ConservationAreaInfo info) throws Exception{
		Integer lockKey = info.getLockKey();
		session.createSQLQuery("SELECT cast(pg_advisory_lock(" + lockKey + ") as varchar)").list();
	}
	
	public void releaseDatabase(Session session, ConservationAreaInfo info ) throws Exception{
		Integer lockKey = info.getLockKey();
		session.createSQLQuery("SELECT cast(pg_advisory_unlock(" + lockKey + ") as varchar)").list();
	}
}

