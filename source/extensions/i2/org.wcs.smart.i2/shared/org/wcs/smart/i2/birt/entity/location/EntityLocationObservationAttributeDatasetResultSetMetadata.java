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

import java.util.Locale;
import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

/**
 * Entity locations dataset results metadata
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public class EntityLocationObservationAttributeDatasetResultSetMetadata implements IResultSetMetaData {

	public static enum Column{
		UUID("uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ENTITY_UUID("entity_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		
		WP_LOCATION_ID("wp_location:id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		WP_LOCATION_UUID("wp_location:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		WP_LOCATION("wp_location:type", java.sql.Types.VARCHAR), //$NON-NLS-1$
		
		DATE("observation:date", java.sql.Types.TIMESTAMP), //$NON-NLS-1$
		OBSERVATION_UUID("observation:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CATEGORY("observation:category", java.sql.Types.VARCHAR), //$NON-NLS-1$
		CATEGORYHKEY("observation:categorykey", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTE("observation:attribute", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTEKEY("observation:attributekey", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GEOM_SOURCE("geometry:source", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GEOM_AREA("geometry:area", java.sql.Types.DOUBLE),  //$NON-NLS-1$
		GEOM_PERIMETER("geometry:perimeter", java.sql.Types.DOUBLE), //$NON-NLS-1$
		LINESTRING("geometry:linestring", IGeometryColumn.Type.MULTILINESTRING.birtDataType), //$NON-NLS-1$
		POLYGON("geometry:polygon", IGeometryColumn.Type.MULTIPOLYGON.birtDataType); //$NON-NLS-1$
		
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
		
		public Object getValue(Object item, Locale l, UUID entityUuid) {
			if (this == ENTITY_UUID) return entityUuid;
			
			if (item instanceof IntelObservationAttribute) {
				return getValue((IntelObservationAttribute)item, l);
			}else if (item instanceof WaypointObservationAttribute) {
				return getValue((WaypointObservationAttribute)item, l);
			}
			return null;
		}
		
		private Object getValue(IntelObservationAttribute attribute, Locale l) {
			if (this == UUID) return attribute.getUuid();
			if (this == WP_LOCATION_ID) return attribute.getObservation().getLocation().getId();
			if (this == WP_LOCATION_UUID) return attribute.getObservation().getLocation().getUuid();
			if (this == WP_LOCATION) return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IIntelligenceLabelProvider.SRC_PROFILES, l);
			
			if (this == DATE) return attribute.getObservation().getLocation().getDateTime();
			if (this == OBSERVATION_UUID) return attribute.getObservation().getUuid();
			if (this == CATEGORY) return attribute.getObservation().getCategory().getName();
			if (this == CATEGORYHKEY) return attribute.getObservation().getCategory().getHkey();
			if (this == ATTRIBUTE) return attribute.getAttribute().getName();
			if (this == ATTRIBUTEKEY) return attribute.getAttribute().getKeyId();
			
			if (this == GEOM_SOURCE) return attribute.getGeometry().getSource().getLabel(l);
			if (this == GEOM_PERIMETER) return attribute.getGeometry().getPerimeter();
			if (this == GEOM_AREA) {
				if (attribute.getAttribute().getType() == AttributeType.POLYGON) return attribute.getGeometry().getArea();
				return null;
			}
			if (this == POLYGON) {
				if (attribute.getAttribute().getType() == AttributeType.POLYGON) return attribute.getGeometry().getGeometry();
				return null;
			}
			if (this == LINESTRING) {
				if (attribute.getAttribute().getType() == AttributeType.LINE) return attribute.getGeometry().getGeometry();
				return null;
			}
			return null;
		}
		
		private Object getValue(WaypointObservationAttribute attribute, Locale l) {
			if (this == UUID) return attribute.getUuid();
			if (this == WP_LOCATION_ID) return attribute.getObservation().getObservationGroup().getWaypoint().getId();
			if (this == WP_LOCATION_UUID) return attribute.getObservation().getObservationGroup().getWaypoint().getUuid();
			if (this == WP_LOCATION) return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IIntelligenceLabelProvider.SRC_WPS, l);
			
			if (this == DATE) return attribute.getObservation().getObservationGroup().getWaypoint().getDateTime();
			if (this == OBSERVATION_UUID) return attribute.getObservation().getUuid();
			if (this == CATEGORY) return attribute.getObservation().getCategory().getName();
			if (this == CATEGORYHKEY) return attribute.getObservation().getCategory().getHkey();
			if (this == ATTRIBUTE) return attribute.getAttribute().getName();
			if (this == ATTRIBUTEKEY) return attribute.getAttribute().getKeyId();
			
			if (this == GEOM_SOURCE) return attribute.getGeometry().getSource().getLabel(l);
			if (this == GEOM_PERIMETER) return attribute.getGeometry().getPerimeter();
			if (this == GEOM_AREA) {
				if (attribute.getAttribute().getType() == AttributeType.POLYGON) return attribute.getGeometry().getArea();
				return null;
			}
			if (this == POLYGON) {
				if (attribute.getAttribute().getType() == AttributeType.POLYGON) return attribute.getGeometry().getGeometry();
				return null;
			}
			if (this == LINESTRING) {
				if (attribute.getAttribute().getType() == AttributeType.LINE) return attribute.getGeometry().getGeometry();
				return null;
			}
			return null;
		}
	}
	
	private Locale l;
	
	public EntityLocationObservationAttributeDatasetResultSetMetadata(Locale l){
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityLocationDataset.DATASET_TYPE );
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
