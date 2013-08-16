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
import org.eclipse.datatools.connectivity.oda.IDriver;
import org.eclipse.datatools.connectivity.oda.LogConfiguration;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.util.manifest.DataTypeMapping;
import org.eclipse.datatools.connectivity.oda.util.manifest.ExtensionManifest;
import org.eclipse.datatools.connectivity.oda.util.manifest.ManifestExplorer;
import org.wcs.smart.data.oda.smart.internal.Messages;

/**
 * Implementation class of IDriver for a SMART ODA runtime driver.
 */
public class SmartDriver implements IDriver {
	static String ODA_DATA_SOURCE_ID = "org.wcs.smart.data.oda.smart"; //$NON-NLS-1$

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IDriver#getConnection(java.lang.String)
	 */
	public IConnection getConnection(String dataSourceType) throws OdaException {
		// assumes that this driver supports only one type of data source,
		// ignores the specified dataSourceType
		return new SmartConnection();
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

	/**
	 * Returns the object that represents this extension's manifest.
	 * 
	 * @throws OdaException
	 */
	static ExtensionManifest getManifest() throws OdaException {
		return ManifestExplorer.getInstance().getExtensionManifest(
				ODA_DATA_SOURCE_ID);
	}

	/**
	 * Returns the native data type name of the specified code, as defined in
	 * this data source extension's manifest.
	 * 
	 * @param nativeTypeCode
	 *            the native data type code
	 * @return corresponding native data type name
	 * @throws OdaException
	 *             if lookup fails
	 */
	public static String getNativeDataTypeName(int nativeDataTypeCode, String dataSetType)
			throws OdaException {
		DataTypeMapping typeMapping = getManifest().getDataSetType(dataSetType)
				.getDataTypeMapping(nativeDataTypeCode);
		if (typeMapping != null)
			return typeMapping.getNativeType();
		return Messages.SmartDriver_Underfined_MappingType;
	}

}
