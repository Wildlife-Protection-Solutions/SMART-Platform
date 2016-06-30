package org.wcs.smart.query.compound.engine;

import java.sql.SQLException;
import java.util.HashMap;

import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.model.Query;

public class CompoundQueryEngine implements IQueryEngine {

	@Override
	public IQueryResult executeQuery(Query query,
			HashMap<String, Object> parameters) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canExecute(String queryTypeKey) {
		return (queryTypeKey.equalsIgnoreCase(CompoundMapQuery.TYPE_KEY));
	}

	@Override
	public String tableNamePrefix(Class<?> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String tablePrefix(Class<?> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String tableName(Class<?> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addParameterValue(Object parameter) {
		// TODO Auto-generated method stub
		return null;
	}

}
