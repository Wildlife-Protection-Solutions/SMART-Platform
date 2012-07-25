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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.engine.DerbySummaryEngine;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.model.SummaryQueryResult;
import org.wcs.smart.query.model.observation.QueryColumn;

/**
 * Resultset Metadata object for 
 * an summary query
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryResultSetMetadata implements IResultSetMetaData {

	private SummaryQueryResult results;
	
	/**
	 * creates a new metadata object for a given query
	 * @param query
	 */
	public SummaryQueryResultSetMetadata(final SummaryQuery query){
		results =  new SummaryQueryResult();
		Job parseQuery = new Job("Parsing Query") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				try{
					DerbySummaryEngine.getHeaderInfo(query, results, session);
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
		};
		parseQuery.schedule();
		
		try {
			parseQuery.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
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
			return "";
		}else{
			StringBuilder sb= new StringBuilder();
			for (int i = 0; i < results.getColumnHeaderValues().length; i ++){
				sb.append(results.getColumnHeaderValues()[i][index - results.getRowHeaders().size()].getName());
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		String name = getColumnLabel(index);
		if(name.length() == 0){
			return "Header";
		}
		return getColumnLabel(index);
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
	     return SmartDriver.getNativeDataTypeName( nativeTypeCode );
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
