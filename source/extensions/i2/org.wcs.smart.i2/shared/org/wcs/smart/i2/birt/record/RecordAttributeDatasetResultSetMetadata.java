package org.wcs.smart.i2.birt.record;

import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.hibernate.Session;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordAttributeValueList;

public class RecordAttributeDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		RECORD_UUID("recordatt:record_uuid", "Record UUID", java.sql.Types.VARCHAR),
		ATTRIBUTE("recordatt:name", "Attribute Name", java.sql.Types.VARCHAR),
		TEXT("recordatt:text", "Attribute Value", java.sql.Types.VARCHAR),
		STRING_VALUE("recordatt:stringvalue", "String Value", java.sql.Types.VARCHAR),
		NUMBER_VALUE("recordatt:numbervalue", "Number Value", java.sql.Types.DOUBLE),
		DATE_VALUE("recordatt:datevalue", "Date Value", java.sql.Types.DATE);
		
		String id;
		String name;
		int type;
		
		Column(String id, String name, int type){
			this.id = id;
			this.name = name;
			this.type = type;
		}
		public String getColumnName(){
			return this.name;
		}
		public String getId(){
			return this.id;
		}
		
		public Object getValue(IntelRecordAttributeValue record, Locale l, Session s) {
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
					name += ":";
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
					if (record.getAttributeListItems().isEmpty()) return "";
					StringBuilder sb = new StringBuilder();
					for (IntelRecordAttributeValueList item : record.getAttributeListItems()){
						if (record.getAttribute().getAttribute() != null){
							IntelAttributeListItem a = (IntelAttributeListItem) s.get(IntelAttributeListItem.class, item.getId().getElementUuid());
							if (a != null) sb.append(a.getName());
						}else if (record.getAttribute().getEntityType() != null){
							IntelEntity a = (IntelEntity) s.get(IntelEntity.class, item.getId().getElementUuid());
							if (a != null) sb.append(a.getIdAttributeAsText(l));
						}
						sb.append(", ");
					}
					return sb.substring(0, sb.length()- 2);
				}else{
					return record.getAttributeValueAsString(l);
				}
			}
			return null;
		}
	}
	
	public RecordAttributeDatasetResultSetMetadata(){
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
