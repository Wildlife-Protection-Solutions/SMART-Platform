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
package org.wcs.smart.i2.birt.record.location;

import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelObservationAttribute;

/**
 * Entity record datasets results metadata
 * @author Emily
 *
 */
public class RecordLocationObservationDetailsDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		RECORD_UUID("recordobs:record_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		LOCATIONID("recordobs:locationid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		OBSERVATION_UUID("recordobs:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTE_KEY("recordobs:attributekey", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTE("recordobs:attribute", java.sql.Types.VARCHAR), //$NON-NLS-1$
		STRING_VALUE("recordobs:stringvalue", java.sql.Types.VARCHAR), //$NON-NLS-1$
		NUMBER_VALUE("recordobs:numbervalue", java.sql.Types.DOUBLE), //$NON-NLS-1$
		DATE_VALUE("recordobs:datevalue", java.sql.Types.DATE), //$NON-NLS-1$
		BOOLEAN_VALUE("recordobs:booleanvalue", java.sql.Types.BOOLEAN), //$NON-NLS-1$
		POLYGON_VALUE("recordobs:polygonvalue", IGeometryColumn.Type.MULTIPOLYGON.birtDataType), //$NON-NLS-1$
		LINESTRING_VALUE("recordobs:linestringvalue", IGeometryColumn.Type.MULTILINESTRING.birtDataType), //$NON-NLS-1$
		GEOM_AREA_VALUE("recordobs:geomarea", java.sql.Types.DOUBLE), //$NON-NLS-1$
		GEOM_PERIMETER_VALUE("recordobs:geomperimeter", java.sql.Types.DOUBLE); //$NON-NLS-1$
		
		String id;
		int type;
		
		Column(String id, int type){
			this.id = id;
			this.type = type;
		}
		public String getColumnName(Locale l){
			return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(this, l);
		}
		public String getId(){
			return this.id;
		}
		public Object getValue(IntelObservationAttribute location, Locale l) {
			switch(this) {
			case ATTRIBUTE: return location.getAttribute().getName();
			case ATTRIBUTE_KEY: return location.getAttribute().getKeyId();
			case BOOLEAN_VALUE:
				if (location.getAttribute().getType() == AttributeType.BOOLEAN) return location.getAttributeValue();
				return null;
			case DATE_VALUE:
				if (location.getAttribute().getType() == AttributeType.DATE) return location.getAttributeValue();
				return null;
			case LINESTRING_VALUE:
				if (location.getAttribute().getType() == AttributeType.LINE) return location.getGeometry().getGeometry();
				return null;
			case LOCATIONID:
				return location.getObservation().getLocation().getId();
			case NUMBER_VALUE:
				if (location.getAttribute().getType() == AttributeType.NUMERIC) return location.getAttributeValue();
				return null;
			case OBSERVATION_UUID:
				return location.getUuid();
			case POLYGON_VALUE:
				if (location.getAttribute().getType() == AttributeType.POLYGON) return location.getGeometry().getGeometry();
				return null;
			case GEOM_AREA_VALUE:
				if (location.getAttribute().getType() == AttributeType.POLYGON) return location.getGeometry().getArea();
				return null;
			case GEOM_PERIMETER_VALUE:
				if (location.getAttribute().getType().isGeometry()) return location.getGeometry().getPerimeter();
				return null;
			case RECORD_UUID:
				return location.getObservation().getLocation().getRecord().getUuid();
			case STRING_VALUE:
				if (location.getAttribute().getType() == AttributeType.TEXT) return location.getAttributeValue();
				if (location.getAttribute().getType() == AttributeType.LIST) return location.getAttributeValueAsString(l);
				if (location.getAttribute().getType() == AttributeType.MLIST) return location.getAttributeValueAsString(l);
				if (location.getAttribute().getType() == AttributeType.TREE) return location.getAttributeValueAsString(l);
				return null;
			}
			return null;
		}
	}
	
	private Locale l;
	
	public RecordLocationObservationDetailsDatasetResultSetMetadata(Locale l){
		this.l = l;
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
		return Column.values()[index-1].getColumnName(l);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].id;
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, RecordLocationObservationDetailsDataset.DATASET_TYPE );
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
