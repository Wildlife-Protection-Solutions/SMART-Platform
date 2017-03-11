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
package org.wcs.smart.i2.birt.record;

import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;

/**
 * Record attribute dataset result set metadata
 * @author Emily
 *
 */
public class RecordAttributeDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		RECORD_UUID("recordatt:record_uuid",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		ATTRIBUTE("recordatt:name",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		TEXT("recordatt:text", java.sql.Types.VARCHAR), //$NON-NLS-1$
		STRING_VALUE("recordatt:stringvalue", java.sql.Types.VARCHAR), //$NON-NLS-1$
		NUMBER_VALUE("recordatt:numbervalue", java.sql.Types.DOUBLE), //$NON-NLS-1$
		DATE_VALUE("recordatt:datevalue",java.sql.Types.DATE); //$NON-NLS-1$
		
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
		
		public Object getValue(IntelRecordAttributeValue record, Locale l, CoordinateReferenceSystem crs, Session s) {
			switch(this){
			case RECORD_UUID:
				return record.getRecord().getUuid();
			case ATTRIBUTE:
				String name = record.getAttribute().getName();
				if (name == null || name.isEmpty()){
					if (record.getAttribute().getAttribute() != null){
						name = record.getAttribute().getAttribute().getName();
					}else if (record.getAttribute().getEntityType() != null){
						name = record.getAttribute().getEntityType().getName();
					}
					name += ":"; //$NON-NLS-1$
					return name;
				}else{
					return record.getAttribute().getName();
				}
			case DATE_VALUE:
				return record.getDateValue();
			case NUMBER_VALUE:
				return record.getNumberValue();
			case STRING_VALUE:
				return record.getStringValue();
			case TEXT:
				if (record.getAttribute().isListAttribute()){
					if (record.getAttributeListItems().isEmpty()) return ""; //$NON-NLS-1$
					StringBuilder sb = new StringBuilder();
					for (IntelRecordAttributeValueList item : record.getAttributeListItems()){
						if (record.getAttribute().getAttribute() != null){
							IntelAttributeListItem a = (IntelAttributeListItem) s.get(IntelAttributeListItem.class, item.getId().getElementUuid());
							if (a != null) sb.append(a.getName());
						}else if (record.getAttribute().getEntityType() != null){
							IntelEntity a = (IntelEntity) s.get(IntelEntity.class, item.getId().getElementUuid());
							if (a != null) sb.append(a.getIdAttributeAsText(l));
						}
						sb.append(", "); //$NON-NLS-1$
					}
					return sb.substring(0, sb.length()- 2);
				}else{
					return record.getAttributeValueAsString(l, crs);
				}
			}
			return null;
		}
	}
	
	private Locale l;
	public RecordAttributeDatasetResultSetMetadata(Locale l){
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, RecordAttributeDataset.DATASET_TYPE );
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
