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

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IQuery;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.SortSpec;
import org.eclipse.datatools.connectivity.oda.spec.QuerySpecification;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.UuidUtils;

/**
 * Shared implementation for oda queries wrapper.
 * 
 * @author Emily
 *
 */
public abstract class AbstractSmartBirtQuery implements IQuery{

	public static final String SMART_DATASET_TYPE = "org.wcs.smart.data.oda.smart.smartQueryDataset"; //$NON-NLS-1$
	
	protected int m_maxRows;

	//the query uuid and type
	protected UUID uuid;
	
	//the loaded query
	protected Query smartQuery;
	protected AbstractSmartQuery wrapperObject;

	//dataset parameters
	protected HashMap<Object, Object> parameters = null;
	
	//dataset metadata
	protected ISmartParameterMetadata pMetadata = null;
	protected SmartConnection connection;

	/**
	 * Parses the query text in the query type and query UUID
	 * @param queryText
	 * @return 
	 */
	public static ParsedQuery parseQueryText(String queryText){
		String[] bits = queryText.split(":"); //$NON-NLS-1$
		String queryType = bits[0];
		UUID uuid = null;
		if (bits.length >= 2 &&  !bits[1].isEmpty()){
			uuid = UuidUtils.stringToUuid(bits[1]);
		}
		return new ParsedQuery(queryType, uuid);
	}
	
	/**
	 * Creates a new smart query
	 */
	public AbstractSmartBirtQuery(SmartConnection connection) {
		this.connection = connection;
		
	}

	public Query getQuery(){
		return this.smartQuery;
	}
	
	public HashMap<Object, Object> getParameters(){
		return parameters;
	}
	
	
	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p>Here the queryText contains the hex encoded uuid
	 * of the query.  The query is loaded from the database and
	 * parsed to ensure it is valid.
	 * </p>
	 */
	public abstract void prepare(String queryText) throws OdaException;
	
	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setAppContext(java.lang
	 * .Object)
	 */
	public void setAppContext(Object context) throws OdaException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#close()
	 */
	public void close() throws OdaException {
		try{
			if (wrapperObject != null) wrapperObject.dispose(connection);
		}catch(SQLException ex){
			throw new OdaException(ex);
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException {
		
		IResultSetMetaData metadata = wrapperObject.getMetaData(this, connection);
		if (metadata != null){
			return metadata;
		}
		throw new OdaException("Cannot load metadata for the provided query."); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#executeQuery()
	 */
	public IResultSet executeQuery() throws OdaException {
		return wrapperObject.executeQuery(this, connection);
	}

	/**
	 * Properties not supported.
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setProperty(java.lang.String
	 * , java.lang.String)
	 */
	public void setProperty(String name, String value) throws OdaException {
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setMaxRows(int)
	 */
	public void setMaxRows(int max) throws OdaException {
		m_maxRows = max;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMaxRows()
	 */
	public int getMaxRows() throws OdaException {
		return m_maxRows;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#clearInParameters()
	 */
	public void clearInParameters() throws OdaException {
		parameters.clear();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setInt(java.lang.String,
	 * int)
	 */
	public void setInt(String parameterName, int value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setInt(int, int)
	 */
	public void setInt(int parameterId, int value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDouble(java.lang.String,
	 * double)
	 */
	public void setDouble(String parameterName, double value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDouble(int, double)
	 */
	public void setDouble(int parameterId, double value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(java.lang
	 * .String, java.math.BigDecimal)
	 */
	public void setBigDecimal(String parameterName, BigDecimal value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBigDecimal(int,
	 * java.math.BigDecimal)
	 */
	public void setBigDecimal(int parameterId, BigDecimal value)
			throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setString(java.lang.String,
	 * java.lang.String)
	 */
	public void setString(String parameterName, String value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setString(int,
	 * java.lang.String)
	 */
	public void setString(int parameterId, String value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setDate(java.lang.String,
	 * java.sql.Date)
	 */
	public void setDate(String parameterName, Date value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setDate(int,
	 * java.sql.Date)
	 */
	public void setDate(int parameterId, Date value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTime(java.lang.String,
	 * java.sql.Time)
	 */
	public void setTime(String parameterName, Time value) throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTime(int,
	 * java.sql.Time)
	 */
	public void setTime(int parameterId, Time value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(java.lang.
	 * String, java.sql.Timestamp)
	 */
	public void setTimestamp(String parameterName, Timestamp value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setTimestamp(int,
	 * java.sql.Timestamp)
	 */
	public void setTimestamp(int parameterId, Timestamp value)
			throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(java.lang.String
	 * , boolean)
	 */
	public void setBoolean(String parameterName, boolean value)
			throws OdaException {
		setObject(parameterName, value);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setBoolean(int,
	 * boolean)
	 */
	public void setBoolean(int parameterId, boolean value) throws OdaException {
		setObject(parameterId, value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setObject(java.lang.String,
	 * java.lang.Object)
	 */
	public void setObject(String parameterName, Object value)
			throws OdaException {
		parameters.put(getParameterKey(parameterName), value);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setObject(int,
	 * java.lang.Object)
	 */
	public void setObject(int parameterId, Object value) throws OdaException {
		parameters.put(getParameterKey(parameterId), value);
	}

	/**
	 * 
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setNull(java.lang.String)
	 */
	public void setNull(String parameterName) throws OdaException {
		setObject(parameterName, null);
	}

	/**
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#setNull(int)
	 */
	public void setNull(int parameterId) throws OdaException {
		setObject(parameterId, null);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#findInParameter(java.lang
	 * .String)
	 */
	public int findInParameter(String parameterName) throws OdaException {
		return ((org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData.Parameter)
				getParameterKey(parameterName)).index;
	}

	/**
	 * Implementations must overwrite if they do not want to use the 
	 * default SmartParameterMetaData;
	 * 
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getParameterMetaData()
	 */
	public ISmartParameterMetadata getParameterMetaData() throws OdaException {
		return getParameterMetaDataLocal();
	}

	private ISmartParameterMetadata getParameterMetaDataLocal() throws OdaException {
		if (pMetadata == null) {
			pMetadata = new SmartParameterMetaData();
		}
		return pMetadata;
	}
	
	public Object getParameterKey(int parameterId)  throws OdaException{
		getParameterMetaData();
		return pMetadata.findParameter(parameterId);
	}
	
	public Object getParameterKey(String parameterName)  throws OdaException{
		getParameterMetaData();
		return pMetadata.findParameter(parameterName);
	}
	
	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#setSortSpec(org.eclipse
	 * .datatools.connectivity.oda.SortSpec)
	 */
	public void setSortSpec(SortSpec sortBy) throws OdaException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getSortSpec()
	 */
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
