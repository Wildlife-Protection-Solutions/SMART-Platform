package org.wcs.smart.query.common.engine;

import java.sql.SQLException;

import org.hibernate.Session;

public interface IQueryResult {

	/**
	 * Disposes of the result set and any resources it uses.
	 */
	public void dispose(Session session) throws SQLException;
	
	/**
	 * 
	 * @return true if result set has been disposed of
	 */
	public boolean isDisposed();
	
}
