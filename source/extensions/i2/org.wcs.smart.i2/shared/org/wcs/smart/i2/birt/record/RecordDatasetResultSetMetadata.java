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
import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.eclipse.datatools.connectivity.oda.impl.Blob;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
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
		UUID("record:uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		TITLE("record:title", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DESCRIPTION("record:description", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SCRATCHPAD("record:scratchpad",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		CREATED_BY("record:createdby", java.sql.Types.VARCHAR), //$NON-NLS-1$
		LAST_MODIFIED_BY("record:lastmodifiedby",java.sql.Types.VARCHAR), //$NON-NLS-1$
		CREATED("record:created", java.sql.Types.DATE), //$NON-NLS-1$
		LAST_MODIFIED("record:lastmodified", java.sql.Types.DATE), //$NON-NLS-1$
		STATUS("record:status", java.sql.Types.VARCHAR), //$NON-NLS-1$
		STATUS_KEY("record:status_key", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SOURCE("record:source", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SOURCE_ICON("record:source_icon", java.sql.Types.BLOB); //$NON-NLS-1$
		
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
		
		public Object getValue(IntelRecord record, Locale l) {
			switch(this){
			case UUID:
				return UuidUtils.uuidToString(record.getUuid());
			case TITLE:
				return record.getTitle();
			case DESCRIPTION:
				return record.getDescription();
			case SCRATCHPAD:
				return record.getComment();
			case CREATED_BY:
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(record.getCreatedBy());
			case LAST_MODIFIED_BY:
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(record.getLastModifiedBy());
			case CREATED:
				return record.getDateCreated();
			case LAST_MODIFIED:
				return record.getDateModified();
			case STATUS:
				return SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(record.getStatus(), l);
			case STATUS_KEY:
				return record.getStatus().name();
			case SOURCE:
				return record.getRecordSource().getName();
			case SOURCE_ICON:
				return new Blob(record.getRecordSource().getIcon());			
			default:
				break;
			
			}
			return null;
		}
	}
	
	private Locale l;
	public RecordDatasetResultSetMetadata(Locale l){
		this.l =l;
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
