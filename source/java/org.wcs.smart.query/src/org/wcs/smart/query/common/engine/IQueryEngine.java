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
	
	public void addParameterValue(Object parameter);
}
