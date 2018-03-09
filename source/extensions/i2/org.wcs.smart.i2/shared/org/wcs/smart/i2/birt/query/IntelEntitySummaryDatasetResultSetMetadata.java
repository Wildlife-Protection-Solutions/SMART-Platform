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
package org.wcs.smart.i2.birt.query;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.CcaaQueryItemProvider;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.engine.EntitySummaryQueryHeaderEngine;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;

/**
 * Result set metadata for summary queries
 * 
 * @author Emily
 *
 */
public class IntelEntitySummaryDatasetResultSetMetadata implements IResultSetMetaData {

	private final static String HEADER_COLUMN_KEY = "header"; //$NON-NLS-1$
		
	protected SummaryQueryResult results;
	
	/**
	 * creates a new metadata object for a given query.  The summary
	 * query must have header parsed before configuring.
	 * @param query
	 */
	public IntelEntitySummaryDatasetResultSetMetadata(IntelQueryDataset dataset) throws OdaException{
		IntelEntitySummaryQuery query = dataset.getConnection().getSession().get(IntelEntitySummaryQuery.class, dataset.getQuery());
		if (query == null) {
			throw new OdaException("Entity Summary Query not found"); //$NON-NLS-1$
		}
		
		try {
			SumQueryDefinition parsedQuery = IntelEntitySummaryQuery.parseQuery(query.getQueryString());
	
			IQueryItemProvider itemProvider = null;
			if (!query.getConservationArea().getIsCcaa()) {
				itemProvider = new CaQueryItemProvider(dataset.getConnection().getConservationAreas().iterator().next(), query.getConservationArea());
			}else {
				itemProvider = new CcaaQueryItemProvider(dataset.getConnection().getConservationAreas(), query.getConservationArea());
			}
			
			results = new SummaryQueryResult();
			EntitySummaryQueryHeaderEngine.INSTANCE.getHeaderInfo(parsedQuery, results, null, itemProvider, dataset.getConnection().getCurrentLocale(), dataset.getConnection().getSession());

		} catch (Exception e) {
			throw new OdaException(e);
		}
	}
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		if (results == null) return 0;
		int cnt = results.getNumDataColumns() + results.getRowHeaders().size();
		return cnt;
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
			return HEADER_COLUMN_KEY + "_" + index; //$NON-NLS-1$
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
			return java.sql.Types.VARCHAR;
		}else{
			return java.sql.Types.DOUBLE;
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		int nativeTypeCode = getColumnType( index );
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, IntelQueryDataset.DATASET_TYPE );
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
