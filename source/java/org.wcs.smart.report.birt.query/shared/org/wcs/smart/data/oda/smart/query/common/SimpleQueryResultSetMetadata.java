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

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.GeometryColumn;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Resultset Metadata object for 
 * an simple query
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SimpleQueryResultSetMetadata implements IResultSetMetaData {

	protected QueryColumn[] queryColumns;
	protected GeometryColumn[] geometryColumns;
	
	/**
	 * Creates a new metadata object
	 * @param query the query to gather metadata for
	 */
	public SimpleQueryResultSetMetadata(SimpleQuery query, GeometryColumn[] geometryColumns, SmartConnection connection){
		this(query, connection);
		this.geometryColumns = geometryColumns;
	}
	
	protected SimpleQueryResultSetMetadata(SimpleQuery query, SmartConnection connection){
		List<QueryColumn> vis = new ArrayList<QueryColumn>();
		
		IProjectionProvider provider = null;
		try{
			provider =connection.getProjectionProvider();
		}catch (Exception ex){
			//TODO: log me
		}
		
		//observation queries that are flagged with show
		//data columns only should import all columns
		boolean isObs = false;
		boolean includeAll = false;
		if (query instanceof ObservationQuery){
			isObs = true;
			if (((ObservationQuery)query).isShowDataColumnsOnly()){
				includeAll = true;
			}
		}
		
		for (QueryColumn col : query.computeQueryColumns(connection.getCurrentLocale(), connection.getSession(), provider)){
			if ((isObs && (includeAll || col.isVisible())) || (!isObs && col.isVisible())){
				vis.add(col);
			}
		}
		queryColumns = vis.toArray(new QueryColumn[vis.size()]);
		
		//search duplicate names and update 
		//see ticket #1535 for more info
		HashSet<String> names = new HashSet<String>();
		for (QueryColumn qc : queryColumns){
			if (names.contains(qc.getName())){
				//need to update the name
				int counter = 1;
				String name = qc.getName();
				while(names.contains(name)){
					name = name + "_" + counter; //$NON-NLS-1$
					counter++;
				}
				qc.setName(name);
			}
			names.add(qc.getName());
		}
	}
	
	protected SimpleQueryResultSetMetadata(GriddedQuery query, SmartConnection connection){
		List<QueryColumn> vis = new ArrayList<QueryColumn>();
		for (QueryColumn col : query.computeQueryColumns(connection.getCurrentLocale(), connection.getSession())){
			if (col.isVisible()){
				vis.add(col);
			}
		}
		queryColumns = vis.toArray(new QueryColumn[vis.size()]);
	}
	
	/**
	 * @param index column index
	 * @return the query column at a given index
	 */
	public QueryColumn getQueryColumn(int index){
		if (index == queryColumns.length) return null;
		return queryColumns[index];
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return queryColumns.length + (geometryColumns == null ? 0 : geometryColumns.length);
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
		if (geometryColumns != null && index > queryColumns.length){
			return geometryColumns[index - queryColumns.length - 1].getLabel();  
		}
		return queryColumns[index-1].getName();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		if (geometryColumns != null && index > queryColumns.length){
			return geometryColumns[index - queryColumns.length - 1].getKey();  
		}
		return queryColumns[index-1].getKey();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		if (geometryColumns != null && index > queryColumns.length) return Types.JAVA_OBJECT;
		return queryColumns[index-1].getType().getSqlType();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartConnection.getNativeDataTypeName( nativeTypeCode, AbstractSmartBirtQuery.SMART_DATASET_TYPE );
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getPrecision(int)
	 * @return -1;
	 */
	@Override
	public int getPrecision(int index) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
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
