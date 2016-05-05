package org.wcs.smart.udig.catalog.smart;

import org.locationtech.udig.catalog.IService;

/**
 * Abstract class for identifying services that require
 * hibernate connections. 
 * 
 * @author Emily
 *
 */
public abstract class ISessionService extends IService {

	protected IDatabaseConnectionProvider connectionProvider;
	
	/**
	 * Sets the connection provider for the service.  If not provided the 
	 * application context (SmartContext) default connection provider will be used.
	 * 
	 * @param dbProvider
	 */
	public void setConnectionProvider(IDatabaseConnectionProvider dbProvider){
		this.connectionProvider = dbProvider;
	}
	/**
	 * The database connection provider for the service
	 * @return
	 */
	public IDatabaseConnectionProvider getConnectionProvider(){
		return this.connectionProvider;
	}
	
}
