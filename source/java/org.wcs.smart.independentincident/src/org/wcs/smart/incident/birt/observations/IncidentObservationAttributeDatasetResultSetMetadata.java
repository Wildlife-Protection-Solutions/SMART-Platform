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
package org.wcs.smart.incident.birt.observations;

import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.incident.birt.SmartIncidentDriver;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.UuidUtils;

/**
 * Incident observation attribute result set metadata
 * 
 * @author Emily
 *
 */
public class IncidentObservationAttributeDatasetResultSetMetadata implements IResultSetMetaData {

	public enum Column {
		OBS_UUID(Messages.IncidentObservationAttributeDatasetResultSetMetadata_uuidcolumnname, "obs:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_namecolumnname, "attribute:name", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		TEXT_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_valueasstringcolumnname, "attribute:value", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		STRING_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_stringvaluecolumnname, "attribute:stringvalue", java.sql.Types.VARCHAR),  //$NON-NLS-1$
		NUMBER_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_numbervaluecolumnname, "attribute:numbervalue", java.sql.Types.NUMERIC),  //$NON-NLS-1$
		BOOLEAN_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_booleanvaluecolumnname, "attribute:booleanvalue", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		DATE_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_datevaluecolumnname, "attribute:datevalue", java.sql.Types.DATE), //$NON-NLS-1$
		TIME_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_timevaluecolumnname, "attribute:timevalue", java.sql.Types.TIME),  //$NON-NLS-1$
		LINE_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_LineGeometyrName, "attribute:linegeometry", IGeometryColumn.Type.MULTILINESTRING.birtDataType), //$NON-NLS-1$
		POLYGON_VALUE(Messages.IncidentObservationAttributeDatasetResultSetMetadata_PolgyonGeometryName, "attribute:polygongeometry", IGeometryColumn.Type.MULTIPOLYGON.birtDataType); //$NON-NLS-1$
		
		public String name;
		public String key;
		public int type;
		
		private Column(String name, String key, int type){
			this.name = name;
			this.key = key;
			this.type = type;
		}
		
		public Object getValue(WaypointObservationAttribute wo){
			switch(this){
			case TEXT_VALUE: return wo.getAttributeValueAsString(Locale.getDefault());
			case ATTRIBUTE: return wo.getAttribute().getName();
			case BOOLEAN_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.BOOLEAN) {
					return wo.getNumberValue() >= 0.5;
				}
				return null;
				
			case DATE_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.DATE) {
					return wo.getDateValue();
				}
				return null;
			case TIME_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.TIME) {
					return wo.getTimeValue();
				}
				return null;
			case NUMBER_VALUE: 
				if (wo.getAttribute().getType() == Attribute.AttributeType.NUMERIC) {
					return wo.getNumberValue();
				}
				return null;
			case OBS_UUID: return UuidUtils.uuidToString(wo.getObservation().getUuid());
			case STRING_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.LIST ||
					wo.getAttribute().getType() == Attribute.AttributeType.MLIST || 
					wo.getAttribute().getType() == Attribute.AttributeType.TEXT || 
					wo.getAttribute().getType() == Attribute.AttributeType.TREE) { 
				
					return wo.getAttributeValueAsString(Locale.getDefault());
				}
				return null;
			case LINE_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.LINE &&
					wo.getGeometry() != null) {
					return wo.getGeometry().getGeometry();
				}
				return null;
			case POLYGON_VALUE:
				if (wo.getAttribute().getType() == Attribute.AttributeType.POLYGON &&
					wo.getGeometry() != null) {
					return wo.getGeometry().getGeometry();
				}
				return null;
			}
			 
			return null;
		}
	}
	
	public IncidentObservationAttributeDatasetResultSetMetadata(){
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
	     return SmartIncidentDriver.getNativeDataTypeName( nativeTypeCode, IncidentObservationAttributeDataset.DATASET_TYPE );
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