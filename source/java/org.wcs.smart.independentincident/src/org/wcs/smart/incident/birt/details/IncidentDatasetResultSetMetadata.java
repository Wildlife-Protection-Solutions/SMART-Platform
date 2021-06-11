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
package org.wcs.smart.incident.birt.details;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.incident.birt.SmartIncidentDriver;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART plan target result set metadata.
 * 
 * @author Emily
 * @since 2.0.0
 *
 */
public class IncidentDatasetResultSetMetadata implements IResultSetMetaData {

	public static final String RAW_GEOM_COLUMN_NAME = "wp:rawgeometry"; //$NON-NLS-1$
	public static final String GEOM_COLUMN_NAME = "wp:geometry"; //$NON-NLS-1$
	

	public enum Column {

		UUID ("Incident UUID", "wp:uuid", java.sql.Types.VARCHAR),
		ID("Incident ID", "wp:id", java.sql.Types.VARCHAR),
		DATETIME("Date Time", "wp:datetime", java.sql.Types.TIMESTAMP), //$NON-NLS-1$
		RAW_X("Raw X", "wp:rawx", java.sql.Types.DOUBLE),
		RAW_Y("Raw Y", "wp:rawy", java.sql.Types.DOUBLE),
		DISTANCE("Distance", "wp:distance", java.sql.Types.DOUBLE),
		BEARING("Bearing", "wp:direction", java.sql.Types.DOUBLE),
		X("X", "wp:projx", java.sql.Types.DOUBLE),
		Y("Y", "wp:projy", java.sql.Types.DOUBLE),
		COMMENTS("Comments", "wp:comment", java.sql.Types.VARCHAR),
		OBSERVER("Observer", "wp:observer", java.sql.Types.VARCHAR),
		CA_ID("Conservation Area ID", "ca:id", java.sql.Types.VARCHAR),
		CA_NAME("Conservation Area Name", "ca:name", java.sql.Types.VARCHAR),
		
		RAW_GEOMETRY("Raw Geometry", RAW_GEOM_COLUMN_NAME, java.sql.Types.JAVA_OBJECT),
		GEOMETRY("Geometry", GEOM_COLUMN_NAME, java.sql.Types.JAVA_OBJECT);
	
		public String name;
		public String key;
		public int type;
		
		private Column(String name, String key, int type){
			this.name = name;
			this.key = key;
			this.type = type;
		}
		
		public Object getValue(Waypoint wp){
			
			switch(this){
			case BEARING: return wp.getDirection();
			case COMMENTS: return wp.getComment();
			case DATETIME: return wp.getDateTime();
			case DISTANCE: return wp.getDistance();
			case GEOMETRY: return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getX(), wp.getY()));
			case ID: return wp.getId();
			case OBSERVER: return "IMPLEMENT THIS";
			case X: return wp.getX();
			case Y: return wp.getY();
			case RAW_GEOMETRY: return GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getRawX(), wp.getRawY()));
			case UUID: return UuidUtils.uuidToString(wp.getUuid());
			case RAW_X: return wp.getRawX();
			case RAW_Y: return wp.getRawY();
			case CA_ID: return wp.getConservationArea().getId();
			case CA_NAME: return wp.getConservationArea().getName();
			}
			return null;
		}
	}
	
	public IncidentDatasetResultSetMetadata(){
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return Column.values().length;
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
		return Column.values()[index-1].name;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].key;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		return Column.values()[index-1].type;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return SmartIncidentDriver.getNativeDataTypeName( nativeTypeCode, IncidentDataset.DATASET_TYPE );
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