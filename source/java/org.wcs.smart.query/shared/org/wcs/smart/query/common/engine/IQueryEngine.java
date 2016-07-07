package org.wcs.smart.query.common.engine;

import java.sql.SQLException;
import java.util.HashMap;

import org.wcs.smart.query.model.Query;

public interface IQueryEngine {
	
	/**
	 * Executes the query with the set of parameters provided.
	 * Parameters required are specific to query type and engine and
	 * may include items such as current local, hibernate database session
	 * etc.
	 * 
	 * @param query
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public IQueryResult executeQuery(Query query, HashMap<String, Object> parameters) throws SQLException;
	
	/**
	 * Returns true if the engine can execute the given
	 * query type.  False otherwise
	 * @param queryTypeKey
	 * @return
	 */
	public boolean canExecute(String queryTypeKey);
	
	/**
	 * Patrol.class = "smart.patrol p"
	 * @param clazz
	 * @return the table name with associated short form
	 */
	public String tableNamePrefix(Class<?> clazz);
	
	/**
	 * 
	 * @param clazz
	 * @return the table prefix for the query query name
	 */
	public String tablePrefix(Class<?> clazz);
	/**
	 * 
	 * @param clazz
	 * @return the table name for the given query
	 */
	public String tableName(Class<?> clazz);
	
	/**
	 * Adds a parameter to the set of parameters for the currently active
	 * query.  It will return the name of the parameter which should be
	 * used in the query.  Parameters are named so that query
	 * strings can be build in any order (the where statement then
	 * the from statement, then combine); this way the order of the parameters
	 * don't matter.  @see org.wcs.smart.query.common.engine.NamedPreparedStatement
	 */
	public String addParameterValue(Object parameter);
}
