package org.wcs.smart.i2.birt.entity;

import java.util.HashSet;
import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;

public class EntityLocationAttributeDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		ENTITY_UUID("entity:entity_uuid", "Entity UUID", java.sql.Types.VARCHAR),
		ATTRIBUTE_KEY("attribute:key", "Attribute Key", java.sql.Types.VARCHAR),
		ATTRIBUTE_NAME("attribute:name", "Attribute Name", java.sql.Types.VARCHAR),
		GEOMETRY("attribute:geometry", "Geometry", java.sql.Types.JAVA_OBJECT);
		
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
		public Object getValue(IntelEntityAttributeValue value, Locale l) {
			if (this == ENTITY_UUID) return value.getEntity().getUuid();
			if (this == ATTRIBUTE_KEY) return value.getAttribute().getKeyId();
			if (this == ATTRIBUTE_NAME) return value.getAttribute().getName();
			if (this == GEOMETRY) return value.getAttributeValue();
			return null;
		}
	}
	
	public EntityLocationAttributeDatasetResultSetMetadata(){
		HashSet<String> fixedLabels = new HashSet<>();
		for (Column c : Column.values()){
			fixedLabels.add(c.name);
		}
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		int cnt = Column.values().length;
		return cnt;
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityLocationAttributeDataset.DATASET_TYPE );
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
