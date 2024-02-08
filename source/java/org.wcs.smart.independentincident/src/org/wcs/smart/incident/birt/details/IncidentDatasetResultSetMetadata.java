/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.incident.birt.SmartIncidentDriver;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Incident dataset result set metadata
 * 
 * @author Emily
 * 
 *
 */
public class IncidentDatasetResultSetMetadata implements IResultSetMetaData {

	public static final String RAW_GEOM_COLUMN_NAME = "wp:rawgeometry"; //$NON-NLS-1$
	public static final String GEOM_COLUMN_NAME = "wp:geometry"; //$NON-NLS-1$
	

	public enum Column {

		UUID (Messages.IncidentDatasetResultSetMetadata_uuidcolumnname, "wp:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ID(Messages.IncidentDatasetResultSetMetadata_idcolumnname, "wp:id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DATETIME(Messages.IncidentDatasetResultSetMetadata_datetimecolumnname, "wp:datetime", java.sql.Types.TIMESTAMP), //$NON-NLS-1$
		RAW_X(Messages.IncidentDatasetResultSetMetadata_rawxcolumnname, "wp:rawx", java.sql.Types.DOUBLE), //$NON-NLS-1$
		RAW_Y(Messages.IncidentDatasetResultSetMetadata_rawycolumnname, "wp:rawy", java.sql.Types.DOUBLE), //$NON-NLS-1$
		DISTANCE(Messages.IncidentDatasetResultSetMetadata_distancecolumnname, "wp:distance", java.sql.Types.DOUBLE), //$NON-NLS-1$
		BEARING(Messages.IncidentDatasetResultSetMetadata_bearingcolumnname, "wp:direction", java.sql.Types.DOUBLE), //$NON-NLS-1$
		X(Messages.IncidentDatasetResultSetMetadata_xcolumnname, "wp:projx", java.sql.Types.DOUBLE), //$NON-NLS-1$
		Y(Messages.IncidentDatasetResultSetMetadata_ycolumnname, "wp:projy", java.sql.Types.DOUBLE), //$NON-NLS-1$
		COMMENTS(Messages.IncidentDatasetResultSetMetadata_commentscolumnname, "wp:comment", java.sql.Types.VARCHAR), //$NON-NLS-1$
		OBSERVER(Messages.IncidentDatasetResultSetMetadata_observercolumnname, "wp:observer", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CA_ID(Messages.IncidentDatasetResultSetMetadata_caidcolumnname, "ca:id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CA_NAME(Messages.IncidentDatasetResultSetMetadata_canamecolumnname, "ca:name", java.sql.Types.VARCHAR), //$NON-NLS-1$
		
		RAW_GEOMETRY(Messages.IncidentDatasetResultSetMetadata_rawgeomcolumnname, RAW_GEOM_COLUMN_NAME, IGeometryColumn.Type.POINT.birtDataType),
		GEOMETRY(Messages.IncidentDatasetResultSetMetadata_geomcolumnname, GEOM_COLUMN_NAME, IGeometryColumn.Type.POINT.birtDataType);
	
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
			case OBSERVER: {
				for (WaypointObservation wo : wp.getAllObservations()) {
					if (wo.getObserver() != null) return SmartLabelProvider.getShortLabel(wo.getObserver());
				}
				return ""; //$NON-NLS-1$
			}
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