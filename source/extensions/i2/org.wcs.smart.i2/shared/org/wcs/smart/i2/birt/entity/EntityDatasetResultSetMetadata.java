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
package org.wcs.smart.i2.birt.entity;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
import org.wcs.smart.i2.model.IntelEntityType;

/**
 * Entity dataset result set metadata
 * 
 * @author Emily
 *
 */
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
		int cnt = 9;
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
		if (index == 3) return "type_key";
		if (index == 4) return "type";
		if (index == 5) return "date_created";
		if (index == 6) return "date_modified";
		if (index == 7) return "created_by";
		if (index == 8) return "modified_by";
		
		index = index - 9;
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
		if (index == 3) return "Entity Type Key";
		if (index == 4) return "Entity Type";
		if (index == 5) return "Date Created";
		if (index == 6) return "Last Modified";
		if (index == 7) return "Created By";
		if (index == 8) return "Last Modified By";
		index = index - 9;
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
		if (index == 3) return java.sql.Types.VARCHAR;
		if (index == 4) return java.sql.Types.VARCHAR;
		if (index == 5) return java.sql.Types.DATE;
		if (index == 6) return java.sql.Types.DATE;
		if (index == 7) return java.sql.Types.VARCHAR;
		if (index == 8) return java.sql.Types.VARCHAR;
		
		index = index - 9;
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
