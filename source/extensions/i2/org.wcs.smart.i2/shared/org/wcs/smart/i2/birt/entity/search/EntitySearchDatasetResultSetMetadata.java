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
package org.wcs.smart.i2.birt.entity.search;

import java.io.IOException;
import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelEntity;

/**
 * Entity search dataset result set metadata
 * 
 * @author Emily
 *
 */
public class EntitySearchDatasetResultSetMetadata implements IResultSetMetaData {
	
	public static enum Column{
		ENTITY_UUID("entity:entity_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		ID("entity:id",java.sql.Types.VARCHAR), //$NON-NLS-1$
		TYPE_KEY("entity:type_key",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		TYPE("entity:type", java.sql.Types.VARCHAR), //$NON-NLS-1$
		DATE_CREATED("entity:date_created", java.sql.Types.DATE), //$NON-NLS-1$
		DATE_MODIFIED("entity:date_modified",  java.sql.Types.DATE), //$NON-NLS-1$
		CREATED_BY("entity:created_by",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		MODIFIED_BY("entity:modified_by", java.sql.Types.VARCHAR), //$NON-NLS-1$
		PRIMARY_IMAGE("entity:primary_image",  java.sql.Types.VARCHAR); //$NON-NLS-1$
		
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
		
		public Object getValue(IntelEntity entity, Locale l) throws IOException {
			if (this == ENTITY_UUID) return entity.getUuid();
			if (this == ID) return entity.getIdAttributeAsText(l);
			if (this == TYPE_KEY) return entity.getEntityType().getKeyId();
			if (this == TYPE) return entity.getEntityType().getName();
			if (this == DATE_CREATED) return entity.getDateCreatedAtLocal();
			if (this == DATE_MODIFIED) return entity.getDateModifiedAtLocal();
			if (this == CREATED_BY) return entity.getCreatedBy() == null ? "" :  SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(entity.getCreatedBy(), l); //$NON-NLS-1$
			if (this == MODIFIED_BY) return entity.getLastModifiedBy() == null ? "" :  SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(entity.getLastModifiedBy(), l); //$NON-NLS-1$
			if (this == PRIMARY_IMAGE){
				if (entity.getPrimaryAttachment() == null){
					return null;
				}
				return entity.getPrimaryAttachment().getAttachmentFile();
			}
			return null;
		}
	}

	private Locale l;
	
	public EntitySearchDatasetResultSetMetadata(Locale l){
		this.l = l;
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
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntitySearchDataset.DATASET_TYPE );
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
