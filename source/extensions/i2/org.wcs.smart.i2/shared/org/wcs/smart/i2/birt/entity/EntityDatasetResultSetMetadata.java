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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;

/**
 * Entity dataset result set metadata
 * 
 * @author Emily
 *
 */
public class EntityDatasetResultSetMetadata implements IResultSetMetaData {
	
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
			if (this == DATE_CREATED) return entity.getDateCreated();
			if (this == DATE_MODIFIED) return entity.getDateModified();
			if (this == CREATED_BY) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(entity.getCreatedBy(), l);
			if (this == MODIFIED_BY) return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getEmployeeShortLabel(entity.getLastModifiedBy(), l);
			if (this == PRIMARY_IMAGE){
				if (entity.getPrimaryAttachment() == null){
					return null;
				}
				return "file:/" + entity.getPrimaryAttachment().getAttachmentFile().getCanonicalPath(); //$NON-NLS-1$
			}
			return null;
		}
	}
	
	private List<String> attributeColumnNames;
	private List<String> attributeColumnLabels;
	private List<IntelEntityTypeAttribute> attributes;
	private Locale l;
	
	public EntityDatasetResultSetMetadata(IntelEntityType type, Locale l){
		this.l = l;
		HashSet<String> fixedLabels = new HashSet<>();
		for (Column c : Column.values()){
			fixedLabels.add(c.getColumnName(l));
		}
		if (type.getAttributes() == null){
			attributes = Collections.emptyList();
		}else{
			attributes = type.getAttributes();
		}
			
		attributeColumnNames = new ArrayList<String>(attributes.size());
		attributeColumnLabels = new ArrayList<String>(attributes.size());
		for (IntelEntityTypeAttribute attribute : attributes){
			String corelabel = attribute.getAttribute().getName();
			String name = "attribute:" + attribute.getAttribute().getKeyId(); //$NON-NLS-1$
			
			attributeColumnNames.add(name);
			int add = 1;
			String label = corelabel;
			while(attributeColumnLabels.contains(label) || fixedLabels.contains(label)){
				label = corelabel + "_" + add; //$NON-NLS-1$
				add++;
			}
			attributeColumnLabels.add(label);
		}
		
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		int cnt = Column.values().length;
		cnt += attributes.size();
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
		if (index < Column.values().length) return Column.values()[index-1].getColumnName(l);
		index = index - Column.values().length;
		if (index < attributes.size()){
			return attributeColumnLabels.get(index);
		}
		return Column.PRIMARY_IMAGE.getColumnName(l);
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		if (index < Column.values().length) return Column.values()[index-1].id;
		index = index - Column.values().length;
		if (index < attributes.size()){
			return attributeColumnNames.get(index);
		}
		return Column.PRIMARY_IMAGE.id;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		if (index <= 8) return Column.values()[index-1].type;
		index = index - 9;
		if (index < attributes.size()){
			AttributeType attType = attributes.get(index).getAttribute().getType();
			return IntelHibernateManager.getAttributeSqlType(attType);
		}
		return Column.PRIMARY_IMAGE.type;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityDataset.DATASET_TYPE );
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
