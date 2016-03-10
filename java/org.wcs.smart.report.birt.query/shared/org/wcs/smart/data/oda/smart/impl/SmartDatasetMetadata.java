/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Implementation class of IDataSetMetaData for an ODA runtime driver. <br>
 * For demo purpose, the auto-generated method stubs have hard-coded
 * implementation that assume this custom ODA data set is capable of handling a
 * query that returns a single result set and accepts scalar input parameters by
 * index. A custom ODA driver is expected to implement own data set specific
 * behavior in its place.
 */
public class SmartDatasetMetadata implements IDataSetMetaData {
	private IConnection m_connection;

	public SmartDatasetMetadata(IConnection connection) {
		m_connection = connection;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getConnection()
	 */
	public IConnection getConnection() throws OdaException {
		return m_connection;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getDataSourceObjects
	 * (java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public IResultSet getDataSourceObjects(String catalog, String schema,
			String object, String version) throws OdaException {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * getDataSourceMajorVersion()
	 */
	public int getDataSourceMajorVersion() throws OdaException {
		return 1;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * getDataSourceMinorVersion()
	 */
	public int getDataSourceMinorVersion() throws OdaException {
		return 0;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * getDataSourceProductName()
	 */
	public String getDataSourceProductName() throws OdaException {
		return "Smart Data Source";
//		return Messages.SmartDatasetMetadata_SmartDataSourceName;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * getDataSourceProductVersion()
	 */
	public String getDataSourceProductVersion() throws OdaException {
		return Integer.toString(getDataSourceMajorVersion()) + "." + //$NON-NLS-1$
				Integer.toString(getDataSourceMinorVersion());
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getSQLStateType()
	 */
	public int getSQLStateType() throws OdaException {
		return IDataSetMetaData.sqlStateSQL99;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * supportsMultipleResultSets()
	 */
	public boolean supportsMultipleResultSets() throws OdaException {
		return false;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * supportsMultipleOpenResults()
	 */
	public boolean supportsMultipleOpenResults() throws OdaException {
		return false;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * supportsNamedResultSets()
	 */
	public boolean supportsNamedResultSets() throws OdaException {
		return false;
	}

	/*
	 * @see org.eclipse.datatools.connectivity.oda.IDataSetMetaData#
	 * supportsNamedParameters()
	 */
	public boolean supportsNamedParameters() throws OdaException {
		return false;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsInParameters
	 * ()
	 */
	public boolean supportsInParameters() throws OdaException {
		return true;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#supportsOutParameters
	 * ()
	 */
	public boolean supportsOutParameters() throws OdaException {
		return false;
	}

	/*
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IDataSetMetaData#getSortMode()
	 */
	public int getSortMode() {
		return IDataSetMetaData.sortModeNone;
	}

}
