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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection.Permission;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.CaQueryItemProvider;
import org.wcs.smart.i2.query.CcaaQueryItemProvider;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IQueryItemProvider;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;

/**
 * Record attribute dataset result set metadata
 * @author Emily
 *
 */
public class IntelQueryDatasetResultSetMetadata implements IResultSetMetaData {
	
	private List<IQueryColumn> columns;
	private List<String> names;
	
	public IntelQueryDatasetResultSetMetadata(IntelQueryDataset dataset) throws OdaException{
		AbstractIntelQuery query = null;
		if (dataset.getQueryType().equalsIgnoreCase(IntelRecordObservationQuery.KEY)) {
			query = dataset.getConnection().getSession().get(IntelRecordObservationQuery.class, dataset.getQuery());
		}else if(dataset.getQueryType().equalsIgnoreCase(IntelEntityRecordQuery.KEY)) {
			query = dataset.getConnection().getSession().get(IntelEntityRecordQuery.class, dataset.getQuery());
		}
		if (query == null) {
			throw new OdaException("Profiles Record Observtion Query not found"); //$NON-NLS-1$
		}
		try {
			IQueryItemProvider itemProvider = null;
			if (!query.getConservationArea().getIsCcaa()) {
				itemProvider = new CaQueryItemProvider(dataset.getConnection().getConservationAreas().iterator().next(), query.getConservationArea());
			}else {
				itemProvider = new CcaaQueryItemProvider(dataset.connection.hasPermission(Permission.QUERY), query.getConservationArea());
			}			
			columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, itemProvider, dataset.getConnection().getCurrentLocale(), dataset.getConnection().getSession());
			names = new ArrayList<>(columns.size());
			//ensure names are unique
			for(IQueryColumn c: columns) {
				String name = c.getColumnName();
				int i = 0;
				while(names.contains(name)) {
					name += name + " " + i++; //$NON-NLS-1$
				}
				names.add(name);
			}
		} catch (Exception e) {
			throw new OdaException(e);
		}
	}
	
	/**
	 * 
	 * @param index 0 based
	 * @return
	 */
	public IQueryColumn getQueryColumn(int index) {
		return columns.get(index);
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return columns.size();		
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnDisplayLength(int)
	 */
	@Override
	public int getColumnDisplayLength(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnLabel(int)
	 */
	@Override
	public String getColumnLabel(int index) throws OdaException {
		return names.get(index-1);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return  columns.get(index-1).getKey();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		switch (columns.get(index-1).getDataType()) {
		case BOOLEAN: return java.sql.Types.BOOLEAN;
		case DATE: return java.sql.Types.DATE;
		case GEOMETRY: return java.sql.Types.JAVA_OBJECT;
		case NUMERIC: return java.sql.Types.DOUBLE;
		case STRING: return java.sql.Types.VARCHAR;
		case TIME: return java.sql.Types.TIME;
		default:
			return java.sql.Types.VARCHAR;
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
	 */
	@Override
	public int getPrecision(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getScale(int)
	 */
	@Override
	public int getScale(int arg0) throws OdaException {
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#isNullable(int)
	 */
	@Override
	public int isNullable(int arg0) throws OdaException {
		return columnNullableUnknown;
	}

}
