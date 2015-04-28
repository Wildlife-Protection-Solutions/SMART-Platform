package org.wcs.smart.query.common.engine;

public interface IQueryEngine {

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
