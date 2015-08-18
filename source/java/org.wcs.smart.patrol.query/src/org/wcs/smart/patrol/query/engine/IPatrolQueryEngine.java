package org.wcs.smart.patrol.query.engine;

import java.sql.SQLException;
import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;

public interface IPatrolQueryEngine extends IQueryEngine {
	
	public Session getCurrentConnection();
	
}
