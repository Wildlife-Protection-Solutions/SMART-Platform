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
package org.wcs.smart.i2.birt.entity.relation;

import org.eclipse.datatools.connectivity.oda.IConnection;
import org.eclipse.datatools.connectivity.oda.IDataSetMetaData;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;

/**
 * Entity record dataset metadata
 * 
 * @author Emily
 *
 */
public class EntityRelationDatasetMetadata implements IDataSetMetaData {

	private AbstractIntelBirtConnection connection;
	
	public EntityRelationDatasetMetadata(AbstractIntelBirtConnection connection){
		this.connection = connection;
	}
	
	@Override
	public IConnection getConnection() throws OdaException {
		return connection;
	}

	@Override
	public IResultSet getDataSourceObjects(String catalog, String schema,
			String object, String version) throws OdaException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDataSourceMajorVersion() throws OdaException {
		return 1;
	}

	@Override
	public int getDataSourceMinorVersion() throws OdaException {
		return 0;
	}

	@Override
	public String getDataSourceProductName() throws OdaException {
		return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(getClass(), connection.getCurrentLocale());
	}

	@Override
	public String getDataSourceProductVersion() throws OdaException {
		return Integer.toString(getDataSourceMajorVersion()) + "." + //$NON-NLS-1$
				Integer.toString(getDataSourceMinorVersion());
	}

	@Override
	public int getSQLStateType() throws OdaException {
		return IDataSetMetaData.sqlStateSQL99;
	}

	@Override
	public boolean supportsMultipleResultSets() throws OdaException {
		return false;
	}

	@Override
	public boolean supportsMultipleOpenResults() throws OdaException {
		return false;
	}

	@Override
	public boolean supportsNamedResultSets() throws OdaException {
		return false;
	}

	@Override
	public boolean supportsNamedParameters() throws OdaException {
		return false;
	}

	@Override
	public boolean supportsInParameters() throws OdaException {
		return false;
	}

	@Override
	public boolean supportsOutParameters() throws OdaException {
		return false;
	}

	@Override
	public int getSortMode() {
		return IDataSetMetaData.sortModeNone;
	}

}
