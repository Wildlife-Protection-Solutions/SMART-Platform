package org.wcs.smart.i2.birt.entity;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelEntityType;


public class EntityDatasetResultSetMetadata implements IResultSetMetaData {

	private IntelEntityType type;
	
	public EntityDatasetResultSetMetadata(IntelEntityType type){
		this.type = type;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		int cnt = 7;
		if (type.getAttributes() != null){
			cnt += type.getAttributes().size();
		}
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
		if (index == 1) return "entity_uuid";
		if (index == 2) return "id";
		if (index == 3) return "date_created";
		if (index == 4) return "date_modified";
		if (index == 5) return "created_by";
		if (index == 6) return "modified_by";
		
		index = index - 7;
		if (index < type.getAttributes().size()){
			return type.getAttributes().get(index).getAttribute().getKeyId();
		}
		return "primary_image";
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		if (index == 1) return "Entity UUID";
		if (index == 2) return "ID";
		if (index == 3) return "Date Created";
		if (index == 4) return "Last Modified";
		if (index == 5) return "Created By";
		if (index == 6) return "Last Modified By";
		index = index - 7;
		if (index < type.getAttributes().size()){
			return type.getAttributes().get(index).getAttribute().getName();
		}
		return "Primary Image";
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		if (index == 1) return java.sql.Types.JAVA_OBJECT;
		if (index == 2) return java.sql.Types.VARCHAR;
		if (index == 3) return java.sql.Types.DATE;
		if (index == 4) return java.sql.Types.DATE;
		if (index == 5) return java.sql.Types.VARCHAR;
		if (index == 6) return java.sql.Types.VARCHAR;
		index = index - 7;
		if (index < type.getAttributes().size()){
			IAttributeType attType = type.getAttributes().get(index).getAttribute().getType();
			switch(attType){
			case BOOLEAN:
				return java.sql.Types.BOOLEAN;
			case DATE:
				return java.sql.Types.DATE;
			case NUMERIC:
				return java.sql.Types.DOUBLE;
			case LIST:
			case TEXT:
				return java.sql.Types.VARCHAR;
			default:
				break;
			
			};
		}
		return java.sql.Types.VARCHAR;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return IntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityDataset.DATASET_TYPE );
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
