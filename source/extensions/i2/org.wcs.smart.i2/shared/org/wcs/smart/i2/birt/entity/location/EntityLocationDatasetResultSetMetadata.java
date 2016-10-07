package org.wcs.smart.i2.birt.entity.location;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.model.IntelEntityLocation;

import com.vividsolutions.jts.io.ParseException;


public class EntityLocationDatasetResultSetMetadata implements IResultSetMetaData {

	public enum Column{
		ENTITY_UUID("entity_uuid", "Entity UUID", java.sql.Types.VARCHAR),
		ID("id", "ID", java.sql.Types.VARCHAR),
		GEOM("geom", "Geometry", java.sql.Types.JAVA_OBJECT),
		DATE("date", "Date", java.sql.Types.DATE),
		COMMENT("comment", "Comment", java.sql.Types.VARCHAR),
		OBSERVATION("observation", "Observation", java.sql.Types.VARCHAR);
		
		String id;
		String name;
		int type;
		
		Column(String id, String name, int type){
			this.id = id;
			this.name = name;
			this.type = type;
		}
		
		public String getId(){
			return this.id;
		}
		public Object getValue(IntelEntityLocation location) throws ParseException{
			if (this == ENTITY_UUID) return location.getEntity().getUuid();
			if (this == ID) return location.getLocation().getId();
			if (this == GEOM) return location.getLocation().getGeometry();
			if (this == DATE) return location.getLocation().getDateTime();
			if (this == COMMENT) return location.getLocation().getComment();
			if (this == OBSERVATION) return "TODO:";
			return null;
		}
	}
	public EntityLocationDatasetResultSetMetadata(){
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
		return Column.values()[index-1].id;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		return Column.values()[index-1].name;
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
	     return IntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityLocationDataset.DATASET_TYPE );
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
