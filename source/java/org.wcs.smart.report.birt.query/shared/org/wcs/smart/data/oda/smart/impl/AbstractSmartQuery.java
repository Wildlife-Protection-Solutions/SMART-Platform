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

import java.sql.SQLException;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.query.common.IMetadataProvider;
import org.wcs.smart.data.oda.smart.query.common.MemoryQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.PagedQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.SimpleQueryResultSetMetadata;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.report.birt.map.execute.SmartQueryExecutionException;

/**
 * Query dataset handler for allowing queries
 * to be available in a BIRT report.
 * 
 * @author Emily
 *
 */
public abstract class AbstractSmartQuery {
	/**
	 * Report Smart Query Extension Point
	 */
	public static final String SMART_QUERY_EXTENSION_ID = "org.wcs.smart.report.birt.query.queryDataset"; //$NON-NLS-1$
	
	protected IMetadataProvider metadataProvider;
	protected IQueryResult result;
	
	/**
	 * Sets a metadata provider
	 * @param provider
	 */
	public void setMetadataProvider(IMetadataProvider provider){
		this.metadataProvider = provider;
	}
	
	/**
	 * Gets the metadata provider
	 * @return
	 */
	public IMetadataProvider getMetadataProvider(){
		return this.metadataProvider;
	}
	
	/**
	 * Prepares the query but loading and ensure it exists.
	 * @param smartQuery
	 * @throws OdaException
	 */
	public abstract void prepare(AbstractSmartBirtQuery smartQuery, SmartConnection connection) throws OdaException;
	
	/**
	 * Executes the queries and returns the result set
	 * @param smartQuery
	 * @return
	 * @throws OdaException
	 */
	public abstract  IResultSet executeQuery(AbstractSmartBirtQuery smartQuery, SmartConnection connetion) throws OdaException;
	
	/**
	 * Return result set metadata
	 * @param smartQuery
	 * @return
	 * @throws OdaException
	 */
	public abstract IResultSetMetaData getMetaData(AbstractSmartBirtQuery smartQuery, SmartConnection connetion) throws OdaException;
	
	/*
	 * users the metadata provider to create the metadata
	 */
	protected IResultSetMetaData getMetaDataInternal(AbstractSmartBirtQuery smartQuery, SmartConnection connection) throws OdaException{
		return metadataProvider.createMetadata(smartQuery.getQuery(), smartQuery.isAttachment, connection);
	}
	
	public void dispose(SmartConnection connection) throws SQLException{
		if (result != null) result.dispose(connection.getSession());
	}
	/*
	 * executes the connect query using the connection.executequery options
	 */
	protected IResultSet executeQueryInternal(AbstractSmartBirtQuery query,
			SmartConnection connection) throws OdaException {
		// the result set
		result = query.getCachedResults();
		if (result == null || result.isDisposed()) {
			try {
				result = connection.executeQuery(query.getQuery());
				query.setCachedResults( result );
			} catch (Exception ex) {
				throw new SmartQueryExecutionException(ex.getCause() != null ? ex.getCause() : ex);
			}
		}

		if (query.isAttachment) {
			if (result instanceof IPagedImageResultSet) {
				try {
					return new PagedQueryResultSet(
						((IPagedImageResultSet)result).getImageIterator(connection.getSession()),
						((IPagedImageResultSet)result).getImageCount(),
						(SimpleQueryResultSetMetadata) getMetaDataInternal(query,connection),
						connection);
				}catch (SQLException ex) {
					throw new OdaException(ex);
				}
						
			}else {
				throw new UnsupportedOperationException();
			}
		}
		if (result instanceof MemoryQueryResult<?>) {
			return new MemoryQueryResultSet((MemoryQueryResult<?>) result,
					(SimpleQueryResultSetMetadata) getMetaDataInternal(query,connection),
					connection);
		} else if (result instanceof IPagedQueryResultSet) {
			return new PagedQueryResultSet((IPagedQueryResultSet<?>) result,
					(SimpleQueryResultSetMetadata) getMetaDataInternal(query,connection),
					connection);
		} else if (result instanceof GridQueryResult) {
			return new MemoryQueryResultSet((GridQueryResult) result,
					(SimpleQueryResultSetMetadata) getMetaDataInternal(query,connection),
					connection);
		} else if (result instanceof SummaryQueryResult ){
			return new SummaryQueryResultSet((SummaryQueryResult)result,
					(SummaryQueryResultSetMetadata)getMetaDataInternal(query,connection),
					connection);
		}else{
			throw new OdaException("Query result set type not supported."); //$NON-NLS-1$
		}
	}
	
}