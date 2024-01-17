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
package org.wcs.smart.i2.birt.entity.location;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection.Permission;
import org.wcs.smart.i2.birt.datasource.DataSourceParameter;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity locations dataset result set
 * 
 * @author Emily
 *
 */
public class EntityLocationObservationAttributeDatasetResultSet implements IResultSet {

	private static final Object INSUFFICIENT_PRIVILEGES = new Object();

	private long m_maxRows = -1;
	private int m_currentRowId = -1;

	private Object[] currentRow;
	private Object lastRowItem;
	
	private EntityLocationObservationAttributeDatasetResultSetMetadata metadata;
	private ScrollableResults<Object[]> locationResults;
	private ScrollableResults<Object[]> wpResults;
	private Locale l;
	
	private Set<IntelProfile> viewableRecords = null;
	
	private int recordSourceLinkColumn = -1;
	
	/**
	 * Creates a new summary results set
	 * 
	 * @param query
	 *            the summary query
	 * @param metadata
	 *            the metadata
	 */
	public EntityLocationObservationAttributeDatasetResultSet(IntelEntityType type,
			EntityLocationObservationAttributeDatasetResultSetMetadata metadata, 
			AbstractIntelBirtConnection connection, HashMap<Integer, Object> parameters,
			EntityLocationParameterMetadata pmetadata) {
		
		this.l = connection.getCurrentLocale();
		this.metadata = metadata;
		viewableRecords = connection.hasPermission(Permission.RECORD);
		try {
			recordSourceLinkColumn = findColumn(EntityLocationDatasetResultSetMetadata.Column.SOURCELINK.id);
		}catch (OdaException ex) {
			throw new RuntimeException(ex);
		}
		
		Set<IntelProfile> profiles = connection.hasPermission(Permission.ENTITY);

		IntelEntity entity= null;
		int index = pmetadata.findParameterIndex(DataSourceParameter.ENTITY_UUID.getName());
		if (index >= 0){
			String entityparam = (String) parameters.get(index); 
			if ( entityparam != null){
				UUID eUuid = UuidUtils.stringToUuid(entityparam);
				entity = connection.getSession().get(IntelEntity.class, eUuid);
				
				//if entity not found or don't have permission to view it
				//this return nothing
				if (entity == null) {
					this.m_maxRows = 0;
					return;
				}
				if (!profiles.contains(entity.getProfile())) {
					this.m_maxRows = 0;
					return;
				}
				if (entity.getEntityType() != type) {
					this.m_maxRows = 0;
					return;
				}
			}
		}
		
		
		
		StringBuilder sb = new StringBuilder();
	
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("type", type); //$NON-NLS-1$
		
		sb.append("FROM IntelObservationAttribute oa join oa.observation o join o.location l "); //$NON-NLS-1$
		sb.append("join IntelEntityLocation ie on ie.id.location = l "); //$NON-NLS-1$
		sb.append("join ie.id.entity e "); //$NON-NLS-1$
		sb.append(" WHERE e.entityType = :type and e.profile in (:profiles) and oa.geom is not null "); //$NON-NLS-1$
		if (entity != null) {
			sb.append(" AND e = :entity"); //$NON-NLS-1$
			values.put("entity", entity); //$NON-NLS-1$
		}
		
		LocalDateTime startDate = null;
		LocalDateTime endDate = null;
		int index1 = pmetadata.findParameterIndex(DataSourceParameter.START_DATE.getName());
		int index2 = pmetadata.findParameterIndex(DataSourceParameter.END_DATE.getName());
		
		if (index1 > 0 && index2 > 0 && parameters.get(index1) != null && parameters.get(index2) != null){
			startDate = ((java.sql.Date) parameters.get(index1)).toLocalDate().atTime(LocalTime.MIN);
			endDate = ((java.sql.Date) parameters.get(index2)).toLocalDate().atTime(LocalTime.MAX);
			
			sb.append("AND l.dateTime >= :start and l.dateTime <= :end "); //$NON-NLS-1$

			values.put("start", startDate); //$NON-NLS-1$
			values.put("end", endDate); //$NON-NLS-1$
		}

		String count = " SELECT count(*) " + sb.toString(); //$NON-NLS-1$
		
		Query<Long> query1 = connection.getSession().createQuery(count, Long.class);	
		query1.setParameterList("profiles", profiles); //$NON-NLS-1$
		for (Entry<String,Object> e : values.entrySet()){
			query1.setParameter(e.getKey(), e.getValue());
		}
		m_maxRows = query1.uniqueResult();
		
		String items = "SELECT oa, e.uuid FROM " + sb.toString(); //$NON-NLS-1$
		Query<Object[]> query2 = connection.getSession().createQuery(items, Object[].class);	
		query2.setParameterList("profiles", profiles); //$NON-NLS-1$
		for (Entry<String,Object> e : values.entrySet()){
			query2.setParameter(e.getKey(), e.getValue());
		}
		locationResults = query2.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
		
		//now we need to figure out how to add observations
		//from the patrol/mission/survey module
		
		values.clear();
		sb = new StringBuilder();
		sb.append(" SELECT observation.uuid as observationuuid, attributeListItem.uuid as dmentityuuid FROM WaypointObservationAttribute WHERE "); //$NON-NLS-1$
		if (entity != null) {
			if (entity.getDmAttributeListItem() == null) {
				//not more observations to add
				return;
			}else {
				sb.append(" attributeListItem = :dmentityid "); //$NON-NLS-1$
				values.put("dmentityid", entity.getDmAttributeListItem()); //$NON-NLS-1$
			}
			
		}else {
			sb.append(" attributeListItem IN (SELECT dmAttributeListItem FROM "); //$NON-NLS-1$
			sb.append(" IntelEntity WHERE entityType = :type and profile in (:profiles) ) "); //$NON-NLS-1$
			values.put("type", type); //$NON-NLS-1$
			values.put("profiles",  profiles); //$NON-NLS-1$
		}
		
		StringBuilder sb2 = new StringBuilder();
		//sb2.append();
		sb2.append(" FROM WaypointObservationAttribute wa JOIN ("); //$NON-NLS-1$
		sb2.append(sb.toString());
		sb2.append(") b on b.observationuuid = wa.observation.uuid "); //$NON-NLS-1$
		sb2.append(" WHERE wa.geom is not null "); //$NON-NLS-1$
			
		//what we want here are any record observations associated 
		//with the entity that are geometry attributes.
		Query<Long> q1 = connection.getSession().createQuery("SELECT count(*) " + sb2.toString(), Long.class); //$NON-NLS-1$
		for (Entry<String,Object> e : values.entrySet()){
			if (e.getKey().equalsIgnoreCase("profiles")) { //$NON-NLS-1$
				q1.setParameterList("profiles",  (Collection<?>) e.getValue()); //$NON-NLS-1$
			}else {
				q1.setParameter(e.getKey(), e.getValue());
			}
		}
		m_maxRows += q1.uniqueResult();
		
		
		Query<Object[]> q2 = connection.getSession().createQuery(" SELECT wa, b.dmentityuuid " + sb2.toString(), Object[].class); //$NON-NLS-1$
		for (Entry<String,Object> e : values.entrySet()){
			if (e.getKey().equalsIgnoreCase("profiles")) { //$NON-NLS-1$
				q2.setParameterList("profiles",  (Collection<?>) e.getValue()); //$NON-NLS-1$
			}else {
				q2.setParameter(e.getKey(), e.getValue());
			}
		}
		
		wpResults= q2.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
		
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
		if (locationResults.next()){
			currentRow = locationResults.get();
			return true;
		}
		if (wpResults != null && wpResults.next()) {
			currentRow = wpResults.get();
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#close()
	 */
	public void close() throws OdaException {
		locationResults.close();
		locationResults = null;
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
		if (lastRowItem == INSUFFICIENT_PRIVILEGES) return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IIntelligenceLabelProvider.INSUFFICIENT_PRIVILEGES_LABEL, l);
		return lastRowItem.toString();
	}

	/**
	 * object for the current row in the given column index 
	 * @param colIndex column index
	 * @return
	 */
	private Object getCurrentItem(int colIndex) {
		if (currentRow == null) return null;
		if (colIndex == recordSourceLinkColumn && currentRow[0] instanceof IntelEntityLocation) {
			IntelEntityLocation location = (IntelEntityLocation) currentRow[0];
			if (!viewableRecords.contains(location.getLocation().getRecord().getProfile())) {
				return new Object[] {INSUFFICIENT_PRIVILEGES, INSUFFICIENT_PRIVILEGES};
			}
		}
		UUID euuid = (UUID) currentRow[1];
		return EntityLocationObservationAttributeDatasetResultSetMetadata.Column.values()[colIndex-1].getValue(currentRow[0], l, euuid);
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
		}else if (lastRowItem instanceof LocalDate) {
			return java.sql.Date.valueOf( ((LocalDate)lastRowItem));
		}else if (lastRowItem instanceof LocalDateTime) {
			return java.sql.Date.valueOf( (((LocalDateTime)lastRowItem)).toLocalDate() );
		}else if (lastRowItem == null){
			return null;
		}else if (lastRowItem == INSUFFICIENT_PRIVILEGES) {
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
		}else if (lastRowItem instanceof LocalTime) {
			return Time.valueOf((LocalTime)lastRowItem);
		}else if (lastRowItem == INSUFFICIENT_PRIVILEGES) {
			return null;
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
		}else if (lastRowItem instanceof Date) {
			return new Timestamp(((Date)lastRowItem).getTime());
		}else if (lastRowItem instanceof LocalDateTime) {
			return Timestamp.valueOf((LocalDateTime)lastRowItem);
		}else if (lastRowItem == INSUFFICIENT_PRIVILEGES) {
			return null;
		}else if (lastRowItem == null) {
			return null;
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
		}else if (lastRowItem == INSUFFICIENT_PRIVILEGES) {
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
		lastRowItem = getCurrentItem(index);
		return lastRowItem;
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
			if (metadata.getColumnName(i+1).equals(columnName)) {
				return i + 1;
			}
		}
		return -1;
	}

}

