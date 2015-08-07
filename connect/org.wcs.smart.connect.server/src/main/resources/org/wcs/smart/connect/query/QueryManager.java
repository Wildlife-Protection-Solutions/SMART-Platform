package org.wcs.smart.connect.query;

import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.query.model.Query;

public enum QueryManager {

	INSTANCE;
	//4c4facd2-50bc-4533-8b30-26dc84828e61
	public Query findQuery(UUID uuid, Session session){
		
		PatrolObservationQuery query = (PatrolObservationQuery) session.get(PatrolObservationQuery.class, uuid);
		if (query == null){
			System.out.println("query is null");
		}else{
			System.out.println(query.getName());
		}
		
		
		return query;
	}
	
	
}
