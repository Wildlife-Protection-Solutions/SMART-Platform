package org.wcs.smart.incident.birt;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.incident.birt.details.IncidentDatasetResultSetMetadata.Column;


public abstract class AbstractIncidentResultSet<T extends IResultSetMetaData> implements IResultSet { 

	protected Session session;	//connection session
	protected int m_maxRows = -1;
	protected int currentRow = -1;
	
	protected Object lastValue = null;
	private T metadata;
	
	/**
	 * Creates a new summary results set
	 * 
	 * @param query
	 *            the summary query
	 * @param metadata
	 *            the metadata
	 */
	public AbstractIncidentResultSet( SmartIncidentConnection connection, T metadata) {
		session = connection.getSession();
		this.metadata = metadata;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException{
		return metadata;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#setMaxRows(int)
	 */
	public void setMaxRows(int max) throws OdaException {
		m_maxRows = max;
	}

	/**
	 * Returns the maximum number of rows that can be fetched from this result
	 * set.
	 * 
	 * @return the maximum number of rows to fetch.
	 */
	protected int getMaxRows() {
		return m_maxRows;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
	 */
	public boolean next() throws OdaException {
		currentRow ++;
		if (currentRow >= getMaxRows()){
			return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
	 */
	public void close() throws OdaException {
		currentRow = 0;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getRow()
	 */
	public int getRow() throws OdaException {
		return currentRow;
	}

	public abstract Object findCurrentValue(int index);
	
	private Object getColumnObject(int index) {
		lastValue = findCurrentValue(index);
		return lastValue;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
	 */
	public String getString(int index) throws OdaException {
		Object x = getColumnObject(index);
		if (x == null) return null;
		return x.toString();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getString(java.lang
	 * .String)
	 */
	public String getString(String columnName) throws OdaException {
		return getString(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getInt(int)
	 */
	public int getInt(int index) throws OdaException {
		Object x = getColumnObject(index);
		if (x != null && x instanceof Integer) {
			return (Integer) x;
		}
		return -1;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getInt(java.lang.String
	 * )
	 */
	public int getInt(String columnName) throws OdaException {
		return getInt(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(int)
	 */
	public double getDouble(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) return -1;
		
		if (lastObject instanceof Double) {
			return (Double) lastObject;
		} else if (lastObject instanceof Float) {
			return (Float) lastObject;
		} else if (lastObject instanceof Integer) {
			return (Integer) lastObject;
		} else if (lastObject instanceof Long) {
			return (Long) lastObject;
		}
		return -1;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getDouble(java.lang
	 * .String)
	 */
	public double getDouble(String columnName) throws OdaException {
		return getDouble(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(int)
	 */
	public BigDecimal getBigDecimal(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) return BigDecimal.valueOf(-1);
		
		if (lastObject instanceof BigDecimal) {
			return (BigDecimal) lastObject;
		} else if (lastObject instanceof Double || lastObject instanceof Float) {
			return BigDecimal.valueOf((Double) lastObject);
		} else if (lastObject instanceof Long ) {
			return BigDecimal.valueOf((Long) lastObject);
		}
		return BigDecimal.valueOf(-1);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getBigDecimal(java.
	 * lang.String)
	 */
	public BigDecimal getBigDecimal(String columnName) throws OdaException {
		return getBigDecimal(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getDate(int)
	 */
	public Date getDate(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) {
			return null;
		}else if (lastObject instanceof Date) {
			return (Date) lastObject;
		} else if (lastObject instanceof Time) {
			return new Date(((Time) lastObject).getTime());
		}else if (lastObject instanceof LocalDate) {
			return java.sql.Date.valueOf( ((LocalDate)lastObject));
		}else if (lastObject instanceof LocalDateTime) {
			return java.sql.Date.valueOf( (((LocalDateTime)lastObject)).toLocalDate() );
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getDate(java.lang.String
	 * )
	 */
	public Date getDate(String columnName) throws OdaException {
		return getDate(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTime(int)
	 */
	public Time getTime(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) {
			return null;
		}else if (lastObject instanceof Time) {
			return (Time) lastObject;
		}else if (lastObject instanceof LocalTime) {
			return Time.valueOf((LocalTime)lastObject);
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getTime(java.lang.String
	 * )
	 */
	public Time getTime(String columnName) throws OdaException {
		return getTime(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(int)
	 */
	public Timestamp getTimestamp(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) {
			return null;
		}else if (lastObject instanceof Timestamp) {
			return (Timestamp) lastObject;
		}else if (lastObject instanceof LocalDateTime) {
			return Timestamp.valueOf((LocalDateTime)lastObject);
		}
		throw new UnsupportedOperationException();
	}

	/*
	 * @s*ee
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getTimestamp(java.lang
	 * .String)
	 */
	public Timestamp getTimestamp(String columnName) throws OdaException {
		return getTimestamp(findColumn(columnName));
	}

	/**
	 * Not Supported.
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(int)
	 */
	public IBlob getBlob(int index) throws OdaException {
		return (IBlob) findCurrentValue(index);
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getBlob(java.lang.String
	 * )
	 */
	public IBlob getBlob(String columnName) throws OdaException {
		return getBlob(findColumn(columnName));
	}

	/**
	 * Not supported.
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getClob(int)
	 */
	public IClob getClob(int index) throws OdaException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getClob(java.lang.String
	 * )
	 */
	public IClob getClob(String columnName) throws OdaException {
		return getClob(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getBoolean(int)
	 */
	public boolean getBoolean(int index) throws OdaException {
		Object lastObject = getColumnObject(index);
		if (lastObject == null) {
			return Boolean.FALSE;
		}else if (lastObject instanceof Boolean) {
			return (Boolean) lastObject;
		} else if (lastObject instanceof Integer) {
			return ((Integer) lastObject) <= 0;
		} else if (lastObject instanceof Double) {
			return ((Double) lastObject) <= 0.5;
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getBoolean(java.lang
	 * .String)
	 */
	public boolean getBoolean(String columnName) throws OdaException {
		return getBoolean(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getObject(int)
	 */
	public Object getObject(int index) throws OdaException {
		return getColumnObject(index);

	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#getObject(java.lang
	 * .String)
	 */
	public Object getObject(String columnName) throws OdaException {
		return getObject(findColumn(columnName));
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#wasNull()
	 */
	public boolean wasNull() throws OdaException {
		return lastValue == null;
	}

	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IResultSet#findColumn(java.lang
	 * .String)
	 */
	public int findColumn(String columnName) throws OdaException {
		for (int i = 0; i < metadata.getColumnCount(); i++) {
			if (metadata.getColumnName(i).equals(columnName)) {
				return i + 1;
			}
		}
		return -1;
	}
}