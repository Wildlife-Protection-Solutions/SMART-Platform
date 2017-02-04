package org.wcs.smart.i2.birt.record;

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

public class RecordAttributeDataset implements IQuery {
	
	public static final String DATASET_TYPE = "org.wcs.smart.i2.birt.dataset.record.details.attributes";

	private IResultSetMetaData r_metadata = null;
	protected RecordParameterMetadata pMetadata = null;
	private int m_maxRows;
	
	protected AbstractIntelBirtConnection connection;
	
	protected HashMap<Integer, Object> parameters;
	
	public RecordAttributeDataset(AbstractIntelBirtConnection connection){
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
			r_metadata = new RecordAttributeDatasetResultSetMetadata();
		}
		return r_metadata;
	}

	@Override
	public IResultSet executeQuery() throws OdaException {
		RecordAttributeDatasetResultSet set = new RecordAttributeDatasetResultSet((RecordAttributeDatasetResultSetMetadata)getMetaData(), 
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