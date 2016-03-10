package org.wcs.smart.query.common.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;

public interface ITablePagedQueryResultSet extends IPagedQueryResultSet{
	
	/**
	 * Gets results from the given result get.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException;
	
	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	public ResultSet getResultSet(Session session);
	
}
