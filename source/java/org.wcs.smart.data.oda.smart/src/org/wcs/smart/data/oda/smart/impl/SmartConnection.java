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

import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.query.SmartQuery;
import org.wcs.smart.data.oda.smart.impl.table.SmartTableQuery;
import org.wcs.smart.data.oda.smart.internal.Messages;

import com.ibm.icu.util.ULocale;

/**
 * Implementation class of IConnection for SMART ODA runtime driver.
 */
public class SmartConnection implements IConnection {
	
	private boolean m_isOpen = false;

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#open(java.util.Properties)
	 */
	public void open(Properties connProperties) throws OdaException {
		//if smart running connection is open
		m_isOpen = true;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#setAppContext(java.lang.Object)
	 */
	public void setAppContext(Object context) throws OdaException {
		// do nothing; assumes no support for pass-through context
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#close()
	 */
	public void close() throws OdaException {
		m_isOpen = false;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#isOpen()
	 */
	public boolean isOpen() throws OdaException {
		return m_isOpen;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMetaData(java.lang.String)
	 */
	public IDataSetMetaData getMetaData(String dataSetType) throws OdaException {
		return new SmartDatasetMetadata(this);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#newQuery(java.lang.String)
	 */
	public IQuery newQuery(String dataSetType) throws OdaException {
		try {
			if (dataSetType.equals(SmartQuery.SMART_DATASET_TYPE)) {
				return new SmartQuery();
			}else if (dataSetType.equals(SmartTableQuery.SMART_DATASET_TYPE)){
				return new SmartTableQuery();
			}
			throw new OdaException(
					MessageFormat.format(Messages.SmartConnection_Error_DatasetNotSupported,
							new Object[]{dataSetType}));
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#getMaxQueries()
	 */
	public int getMaxQueries() throws OdaException {
		return 0; // no limit
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#commit()
	 */
	public void commit() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#rollback()
	 */
	public void rollback() throws OdaException {
		// do nothing; assumes no transaction support needed
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IConnection#setLocale(com.ibm.icu.util.ULocale)
	 */
	public void setLocale(ULocale locale) throws OdaException {
		// do nothing; assumes no locale support
	}

}
