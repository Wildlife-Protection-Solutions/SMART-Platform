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
package org.wcs.smart.intelligence.report.oda;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IBlob;
import org.eclipse.datatools.connectivity.oda.IClob;
import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.criterion.Restrictions;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * SMRAT Intelligence Location Points target result set
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class IntelligencePointsResultSet implements IResultSet {

	private static final GeometryFactory gf = new GeometryFactory();
	
	public static final String GEOM_COLUMN_NAME = "geometry"; //$NON-NLS-1$
	
	private int m_maxRows = -1;

	private int currentRow = -1;
	private Object lastObject = null;
	private List<IntelligencePoint> points;
	private IntelligencePointsResultSetMetadata metadata;
	private CoordinateReferenceSystem crs = null;
	
	public IntelligencePointsResultSet(String[] uuids, 
			IntelligencePointsResultSetMetadata metaData,
			SmartIntelligenceConnection connection) {
		this.metadata = metaData;
		points = new ArrayList<IntelligencePoint>();
		try{
			this.crs = connection.getProjectionProvider().getProjection().getParsedCoordinateReferenceSystem();
		}catch (Exception ex){
			this.crs = SmartDB.DATABASE_CRS;
		}
		
		
		for (int i = 0; i < uuids.length; i ++){
			try{
				Intelligence p = (Intelligence)connection.getSession().createCriteria(Intelligence.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.add(Restrictions.eq("uuid", UuidUtils.stringToUuid(uuids[i]))).list().get(0); //$NON-NLS-1$
				if (p != null) {
					points.addAll(p.getPoints());
					m_maxRows += p.getPoints().size();
				}
			}catch (Exception ex){
				IntelligencePlugIn.log("Error creating plan target result set", ex); //$NON-NLS-1$
			}
		}
		
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
		return m_maxRows;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#next()
	 */
	public boolean next() throws OdaException {
		currentRow ++;
		if (currentRow > getMaxRows()){
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


	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSet#getString(int)
	 */
	public String getString(int index) throws OdaException {
		lastObject = getCurrentItem(index);
		if (lastObject == null) {
			return ""; //$NON-NLS-1$
		}
		return lastObject.toString();
	}

	/**
	 * object for the current row in the given column index 
	 * @param colIndex column index
	 * @return
	 */
	private Object getCurrentItem(int colIndex) {
		IntelligencePoint pt = points.get(currentRow);
		
		switch (colIndex) {
			case 1: return ReprojectUtils.transform(pt.getX(), pt.getY(), crs).getX();
			case 2: return ReprojectUtils.transform(pt.getX(), pt.getY(), crs).getY();
			case 3: return gf.createPoint(new Coordinate(pt.getX(), pt.getY()));
		}
		return ""; //$NON-NLS-1$
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
		lastObject = getCurrentItem(index);
		if (lastObject instanceof Integer) {
			return (Integer) lastObject;
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
		lastObject = getCurrentItem(index);
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
		lastObject = getCurrentItem(index);
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
		lastObject = getCurrentItem(index);
		if (lastObject instanceof Date) {
			return (Date) lastObject;
		} else if (lastObject instanceof Time) {
			return new Date(((Time) lastObject).getTime());
		} else if (lastObject instanceof java.util.Date) {
			return new Date(((java.util.Date) lastObject).getTime());
		}else if (lastObject == null){
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
		lastObject = getCurrentItem(index);
		if (lastObject instanceof Time) {
			return (Time) lastObject;
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
		lastObject = getCurrentItem(index);
		if (lastObject instanceof Timestamp) {
			return (Timestamp) lastObject;
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
		lastObject = getCurrentItem(index);
		if (lastObject instanceof Boolean) {
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
		return lastObject == null;
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
