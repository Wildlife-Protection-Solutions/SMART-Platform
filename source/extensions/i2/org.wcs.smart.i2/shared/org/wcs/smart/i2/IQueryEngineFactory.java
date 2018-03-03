package org.wcs.smart.i2;

/**
 * Query engine factory that finds the query engine to run for a given query
 * type.  Will return null if no query engine found.
 * 
 * @author Emily
 *
 */
public interface IQueryEngineFactory {

	public IIntelQueryEngine findQueryEngine(String queryType);
	
}
