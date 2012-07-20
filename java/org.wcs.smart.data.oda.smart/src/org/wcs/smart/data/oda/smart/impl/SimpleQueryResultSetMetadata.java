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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.model.observation.QueryColumn;

/**
 * Resultset Metadata object for 
 * an observation query;
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SimpleQueryResultSetMetadata implements IResultSetMetaData {

	private QueryColumn[] queryColumns;
	
	public SimpleQueryResultSetMetadata(SimpleQuery query){
		List<QueryColumn> vis = new ArrayList<QueryColumn>();
		for (QueryColumn col : query.getQueryColumns()){
			if (col.isVisible()){
				vis.add(col);
			}
		}
		queryColumns = vis.toArray(new QueryColumn[vis.size()]);
	}
	
	public QueryColumn getQueryColumn(int index){
		return queryColumns[index];
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return queryColumns.length;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	@Override
	public int getColumnDisplayLength(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		return queryColumns[index-1].getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return queryColumns[index-1].getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		return queryColumns[index-1].getType().getSqlType();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartDriver.getNativeDataTypeName( nativeTypeCode );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 */
	@Override
	public int getPrecision(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int index) throws OdaException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int index) throws OdaException {
		return columnNullableUnknown;
	}

}
