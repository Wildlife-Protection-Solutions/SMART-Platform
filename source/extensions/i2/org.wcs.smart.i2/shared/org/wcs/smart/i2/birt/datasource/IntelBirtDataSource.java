package org.wcs.smart.i2.birt.datasource;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.LogConfiguration;
import org.eclipse.datatools.connectivity.oda.OdaException;

public class IntelBirtDataSource implements IDriver {
	
	public static String ODA_DATA_SOURCE_ID = "org.wcs.smart.i2.birt.datasource"; //$NON-NLS-1$

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IDriver#getConnection(java.lang.String)
	 */
	public IConnection getConnection(String dataSourceType) throws OdaException {
		// assumes that this driver supports only one type of data source,
		// ignores the specified dataSourceType
		return new IntelBirtConnection();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IDriver#setLogConfiguration(org.eclipse.datatools.connectivity.oda.LogConfiguration)
	 */
	public void setLogConfiguration(LogConfiguration logConfig)
			throws OdaException {
		// do nothing; assumes simple driver has no logging
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IDriver#getMaxConnections()
	 */
	public int getMaxConnections() throws OdaException {
		return 0; // no limit
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IDriver#setAppContext(java.lang.Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		// do nothing; assumes no support for pass-through context
	}

}
