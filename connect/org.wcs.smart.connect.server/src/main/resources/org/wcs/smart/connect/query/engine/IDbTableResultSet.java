package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.QueryColumn;

public interface IDbTableResultSet extends IQueryResult {
	
	public ResultSet getQueryResultSet(Connection c) throws SQLException;
		
	public String getValueAsString(ResultSet rs, QueryColumn column, Connection c) throws SQLException;

	public Object getValue(ResultSet rs, String columnKey, Connection c) throws SQLException;
}
