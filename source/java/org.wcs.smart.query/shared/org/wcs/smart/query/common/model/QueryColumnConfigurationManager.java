package org.wcs.smart.query.common.model;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.query.model.QueryColumnConfiguration;

public enum QueryColumnConfigurationManager {
	
	INSTANCE;
	
	public List<QueryColumnConfiguration> getColumnConfigurations(ConservationArea ca, Session session){
		return QueryFactory.buildQuery(session, QueryColumnConfiguration.class, 
				new Object[] {"conservationArea", ca}).list();
	}
	

	public void deleteConfiguration(QueryColumnConfiguration config, Session session) {
		//TODO: update query references?
		session.remove(config);
	}
}
