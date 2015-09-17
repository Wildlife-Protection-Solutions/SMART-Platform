package org.wcs.smart.connect.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;

public enum LockManager {

	INSTANCE;
	//TODO: figure out if we can use the uuid here in some way so 
	//that other processes can also lock database if necessary
	private Map<UUID, Integer> lockKeys = Collections.synchronizedMap(new HashMap<UUID, Integer>());
	
	public void lockDatabase(Session session, UUID caUuid) throws Exception{
		Integer bigInt = null;
		synchronized (lockKeys) {
			bigInt = lockKeys.get(caUuid);
			if (bigInt == null){
				bigInt = lockKeys.size() + 1;
				lockKeys.put(caUuid, bigInt);
			}
		}
		
		session.createSQLQuery("SELECT cast(pg_advisory_lock(" + bigInt + ") as varchar)").list();
	}
	
	public void releaseDatabase(Session session, UUID caUuid) throws Exception{
		Integer bigInt = lockKeys.get(caUuid);
		if (bigInt == null) throw new Exception("No lock found for conservation area.");
		
		session.createSQLQuery("SELECT cast(pg_advisory_unlock(" + bigInt + ") as varchar)").list();
	}
}

