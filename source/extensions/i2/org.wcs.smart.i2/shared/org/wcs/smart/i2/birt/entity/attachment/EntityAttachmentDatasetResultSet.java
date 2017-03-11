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
package org.wcs.smart.i2.birt.entity.attachment;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity attachment dataset results set
 * 
 * @author Emily
 *
 */
public class EntityAttachmentDatasetResultSet implements IResultSet {

	private long m_maxRows = -1;
	private int m_currentRowId = -1;

	private Object currentItem;
	private Object lastRowItem;
	
	private EntityAttachmentDatasetResultSetMetadata metadata;
	private ScrollableResults results;
	
	private Session currentSession = null;
	
	/**
	 * Creates a new summary results set
	 * 
	 * @param query
	 *            the summary query
	 * @param metadata
	 *            the metadata
	 */
	public EntityAttachmentDatasetResultSet(IntelEntityType type,
			EntityAttachmentDatasetResultSetMetadata metadata, 
			AbstractIntelBirtConnection connection, HashMap<Integer, Object> parameters,
			EntityAttachmentParameterMetadata pmetadata) {
		
		this.metadata = metadata;
	
		String q1 = "SELECT count(*) FROM IntelEntityAttachment l WHERE l.id.entity.entityType = :type "; //$NON-NLS-1$
		String q2 = "FROM IntelEntityAttachment l WHERE l.id.entity.entityType = :type "; //$NON-NLS-1$
		
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("type", type); //$NON-NLS-1$
		int index =pmetadata.findParameterIndex(DataSourceParameter.ENTITY_UUID.getName());
		if (index >= 0){
			String entity = (String) parameters.get(index); 
			if ( entity != null){
				q1 += " AND l.id.entity.uuid = :uuid"; //$NON-NLS-1$
				q2 += " AND l.id.entity.uuid = :uuid"; //$NON-NLS-1$
				
				values.put("uuid", UuidUtils.stringToUuid(entity)); //$NON-NLS-1$
			}
		}
		
		Query query1 = connection.getSession().createQuery(q1);
		Query query2 = connection.getSession().createQuery(q2);
		for (Entry<String,Object> e : values.entrySet()){
			query1.setParameter(e.getKey(), e.getValue());
			query2.setParameter(e.getKey(), e.getValue());
		}
		
		m_maxRows = (Long)query1.uniqueResult();
		results = query2.setReadOnly(true)
				.scroll(ScrollMode.FORWARD_ONLY);

		currentSession = connection.getSession();
		
		this.m_currentRowId = 0;
	}
	
	

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getMetaData()
	 */
	public IResultSetMetaData getMetaData() throws OdaException {
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
		return (int)m_maxRows;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
	 */
	public boolean next() throws OdaException {
		m_currentRowId++;
		if (results.next()){
			currentItem = results.get();
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
	 */
	public void close() throws OdaException {
		results.close();
		results = null;
		m_maxRows = -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getRow()
	 */
	public int getRow() throws OdaException {
		return m_currentRowId;
	}
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
	 */
	public String getString(int index) throws OdaException {
		lastRowItem = getCurrentItem(index);
		if (lastRowItem == null) return ""; //$NON-NLS-1$
		return lastRowItem.toString();
	}

	/**
	 * object for the current row in the given column index 
	 * @param colIndex column index
	 * @return
	 */
	private Object getCurrentItem(int colIndex) {
		if (currentItem == null) return null;
		IntelEntityAttachment i = (IntelEntityAttachment) ((Object[])currentItem)[0];
		
		try {
			i.getAttachment().computeFileLocation(currentSession);
		} catch (Exception e) {
			Logger.getLogger(EntityAttachmentDatasetResultSet.class.getName()).log(Level.INFO, e.getMessage(), e);
		}
		return EntityAttachmentDatasetResultSetMetadata.Column.values()[colIndex-1].getValue(i);
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem == null) return -1;
		if (lastRowItem instanceof Integer) return (int) lastRowItem;
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof Double) {
			return (Double) lastRowItem;
		} else if (lastRowItem instanceof Float) {
			return (Float) lastRowItem;
		} else if (lastRowItem instanceof Integer) {
			return (Integer) lastRowItem;
		} else if (lastRowItem instanceof Long) {
			return (Long) lastRowItem;
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof BigDecimal) {
			return (BigDecimal) lastRowItem;
		} else if (lastRowItem instanceof Double || lastRowItem instanceof Float) {
			return BigDecimal.valueOf((Double) lastRowItem);
		} else if (lastRowItem instanceof Long ) {
			return BigDecimal.valueOf((Long) lastRowItem);
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof Date) {
			return (Date) lastRowItem;
		} else if (lastRowItem instanceof Time) {
			return new Date(((Time) lastRowItem).getTime());
		} else if (lastRowItem instanceof java.util.Date) {
			return new Date(((java.util.Date) lastRowItem).getTime());
		}else if (lastRowItem == null){
			return null;
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof Time) {
			return (Time) lastRowItem;
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof Timestamp) {
			return (Timestamp) lastRowItem;
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
		throw new UnsupportedOperationException();
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
		lastRowItem = getCurrentItem(index);
		if (lastRowItem instanceof Boolean) {
			return (Boolean) lastRowItem;
		} else if (lastRowItem instanceof Integer) {
			return ((Integer) lastRowItem) <= 0;
		} else if (lastRowItem instanceof Double) {
			return ((Double) lastRowItem) <= 0.5;
		}else if (lastRowItem == null){
			return false;
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
		return getCurrentItem(index);
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
		return lastRowItem == null;
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
