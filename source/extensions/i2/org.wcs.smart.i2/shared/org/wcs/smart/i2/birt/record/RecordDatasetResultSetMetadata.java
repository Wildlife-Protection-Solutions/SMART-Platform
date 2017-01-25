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

import java.text.MessageFormat;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity record datasets results metadata
 * @author Emily
 *
 */
public class RecordDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		UUID("record:uuid", "UUID", java.sql.Types.VARCHAR),
		TITLE("record:title", "Title", java.sql.Types.VARCHAR),
		DESCRIPTION("record:description", "Description", java.sql.Types.VARCHAR),
		CREATED_BY("record:createdby", "Created By", java.sql.Types.VARCHAR),
		LAST_MODIFIED_BY("record:lastmodifiedby", "Last Modified By", java.sql.Types.VARCHAR),
		CREATED("record:created", "Date Created", java.sql.Types.DATE),
		LAST_MODIFIED("record:lastmodified", "Date Last Modified", java.sql.Types.DATE),
		STATUS("record:status", "Status", java.sql.Types.VARCHAR),
		STATUS_KEY("record:status_key", "Status Key", java.sql.Types.VARCHAR);
		
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
		
		public Object getValue(IntelRecord record) {
			switch(this){
			case UUID:
				return UuidUtils.uuidToString(record.getUuid());
			case TITLE:
				return record.getTitle();
			case DESCRIPTION:
				return record.getDescription();
			case CREATED_BY:
				return MessageFormat.format("{0} {1}", record.getCreatedBy().getGivenName(), record.getCreatedBy().getFamilyName());
			case LAST_MODIFIED_BY:
				return MessageFormat.format("{0} {1}", record.getLastModifiedBy().getGivenName(), record.getLastModifiedBy().getFamilyName());
			case CREATED:
				return record.getDateCreated();
			case LAST_MODIFIED:
				return record.getDateModified();
			case STATUS:
				return record.getStatus().name();
			case STATUS_KEY:
				return record.getStatus().name();
			
			default:
				break;
			}
			return null;
		}
	}
	
	public RecordDatasetResultSetMetadata(){
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, RecordDataset.DATASET_TYPE );
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
