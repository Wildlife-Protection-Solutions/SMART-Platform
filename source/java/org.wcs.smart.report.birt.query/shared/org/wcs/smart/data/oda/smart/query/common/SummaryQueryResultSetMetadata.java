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
package org.wcs.smart.data.oda.smart.query.common;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Resultset Metadata object for 
 * an summary query
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryResultSetMetadata implements IResultSetMetaData {

	private final static String HEADER_COLUMN_KEY = "header"; //$NON-NLS-1$
	protected SummaryQueryResult results;
	
	/**
	 * creates a new metadata object for a given query.  The summary
	 * query must have header parsed before configuring.
	 * @param query
	 */
	public SummaryQueryResultSetMetadata(SummaryQueryResult results){
		this.results = results;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return results.getNumDataColumns() + results.getRowHeaders().size();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 * @return -1
	 */
	@Override
	public int getColumnDisplayLength(int index) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		index = index - 1;
		if (index < results.getRowHeaders().size()){
			return ""; //$NON-NLS-1$
		}else{
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				if (i != 0){
					sb.append("\n");	 //$NON-NLS-1$
				}
				sb.append(results.getColumnHeaderValues()[i][index - results.getRowHeaders().size()].getName());
			}
			return sb.toString();
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		index = index - 1;
		if (index < results.getRowHeaders().size()){
			return HEADER_COLUMN_KEY + "_" + index; //$NON-NLS-1$
		}else{
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				if (i != 0){
					sb.append(" _ "); //$NON-NLS-1$
				}
				sb.append(results.getColumnHeaderValues()[i][index - results.getRowHeaders().size()].getKey());
				String identifier = results.getColumnHeaderValues()[i][index - results.getRowHeaders().size()].getIdentifier();
				if ( identifier != null){
					sb.append("_"); //$NON-NLS-1$
					sb.append(identifier);
				}
			}
			return sb.toString();
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		index--;
		if (index < results.getRowHeaders().size()){
			return QueryColumn.ColumnType.STRING.getSqlType();
		}else{
			return QueryColumn.ColumnType.NUMBER.getSqlType();
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartConnection.getNativeDataTypeName( nativeTypeCode , AbstractSmartBirtQuery.SMART_DATASET_TYPE);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 * @return -1
	 */
	@Override
	public int getPrecision(int index) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 * @return -1
	 */
	@Override
	public int getScale(int index) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int index) throws OdaException {
		return columnNullableUnknown;
	}

}
