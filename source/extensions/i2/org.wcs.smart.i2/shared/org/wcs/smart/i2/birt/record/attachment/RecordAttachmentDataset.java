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
package org.wcs.smart.i2.birt.record.attachment;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;
import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.SortSpec;
import org.eclipse.datatools.connectivity.oda.spec.QuerySpecification;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.record.RecordParameterMetadata;

/**
 * Record details dataset
 * 
 * @author Emily
 *
 */
public class RecordAttachmentDataset implements IQuery {
	
	public static final String DATASET_TYPE = "org.wcs.smart.i2.birt.dataset.record.attachment"; //$NON-NLS-1$

	private IResultSetMetaData r_metadata = null;
	private RecordParameterMetadata pMetadata = null;
	private int m_maxRows;
	
	private AbstractIntelBirtConnection connection;
	
	private HashMap<Integer, Object> parameters;
	
	public RecordAttachmentDataset(AbstractIntelBirtConnection connection){
		this.connection = connection;
		parameters = new HashMap<Integer,Object>();
		
	}
	@Override
	public void prepare(String queryText) throws OdaException {
	}

	@Override
	public void setAppContext(Object context) throws OdaException {
	}

	@Override
	public void setProperty(String name, String value) throws OdaException {
	}

	@Override
	public void close() throws OdaException {
		r_metadata = null;
	}

	@Override
	public void setMaxRows(int max) throws OdaException {
		m_maxRows = max;
	}

	@Override
	public int getMaxRows() throws OdaException {
		return m_maxRows;
	}

	@Override
	public IResultSetMetaData getMetaData() throws OdaException {
		if (r_metadata == null){
			r_metadata = new RecordAttachmentDatasetResultSetMetadata(connection.getCurrentLocale());
		}
		return r_metadata;
	}

	@Override
	public IResultSet executeQuery() throws OdaException {
		RecordAttachmentDatasetResultSet set = new RecordAttachmentDatasetResultSet((RecordAttachmentDatasetResultSetMetadata)getMetaData(), 
				connection, parameters,
				(RecordParameterMetadata)getParameterMetaData());
		return set;
	}

	@Override
	public void clearInParameters() throws OdaException {
	}

	@Override
	public void setInt(String parameterName, int value) throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setInt(int parameterId, int value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setDouble(String parameterName, double value)
			throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setDouble(int parameterId, double value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal value)
			throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setBigDecimal(int parameterId, BigDecimal value)
			throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setString(String parameterName, String value)
			throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setString(int parameterId, String value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setDate(String parameterName, Date value) throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setDate(int parameterId, Date value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setTime(String parameterName, Time value) throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setTime(int parameterId, Time value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp value)
			throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setTimestamp(int parameterId, Timestamp value)
			throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setBoolean(String parameterName, boolean value)
			throws OdaException {
		setObject(parameterName, value);
		
	}

	@Override
	public void setBoolean(int parameterId, boolean value) throws OdaException {
		setObject(parameterId, value);
		
	}

	@Override
	public void setObject(String parameterName, Object value)
			throws OdaException {
		for (int i =0; i < pMetadata.getParameterCount(); i ++){
			if (pMetadata.getParameterName(i).equals(parameterName)){
				setObject(i, value);
				break;
			}
		}
	}


	@Override
	public void setObject(int parameterId, Object value) throws OdaException {
		parameters.put(parameterId, value);
	}

	@Override
	public void setNull(String parameterName) throws OdaException {
		setObject(parameterName, null);
		
	}

	@Override
	public void setNull(int parameterId) throws OdaException {
		setObject(parameterId, null);
		
	}

	@Override
	public int findInParameter(String parameterName) throws OdaException {
		for (int i = 0; i < getParameterMetaData().getParameterCount(); i ++){
			if (getParameterMetaData().getParameterName(i).equals(parameterName)){
				return i;
			}
		}
		return -1;
	}

	@Override
	public IParameterMetaData getParameterMetaData() throws OdaException {
		if (pMetadata == null) {
			pMetadata = new RecordParameterMetadata(DATASET_TYPE);
		}
		return pMetadata;
	}

	@Override
	public void setSortSpec(SortSpec sortBy) throws OdaException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortSpec getSortSpec() throws OdaException {
		return null;
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSpecification(org.eclipse
	 * .datatools.connectivity.oda.spec.QuerySpecification)
	 */
	public void setSpecification(QuerySpecification querySpec)
			throws OdaException, UnsupportedOperationException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSpecification()
	 */
	public QuerySpecification getSpecification() {
		// assumes no support
		return null;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#getEffectiveQueryText()
	 */
	public String getEffectiveQueryText() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#cancel()
	 */
	public void cancel() throws OdaException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
