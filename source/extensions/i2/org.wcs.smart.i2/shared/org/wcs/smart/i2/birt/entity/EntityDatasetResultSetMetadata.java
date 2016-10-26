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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.AttributeManager;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute.IAttributeType;
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
		ENTITY_UUID("entity:entity_uuid", "Entity UUID", java.sql.Types.VARCHAR),
		ID("entity:id", "ID", java.sql.Types.VARCHAR),
		TYPE_KEY("entity:type_key", "Entity Type Key", java.sql.Types.VARCHAR),
		TYPE("entity:type", "Entity Type", java.sql.Types.VARCHAR),
		DATE_CREATED("entity:date_created", "Date Created", java.sql.Types.DATE),
		DATE_MODIFIED("entity:date_modified", "Date Modified", java.sql.Types.DATE),
		CREATED_BY("entity:created_by", "Created By", java.sql.Types.VARCHAR),
		MODIFIED_BY("entity:modified_by", "Last Modified By", java.sql.Types.VARCHAR),
		PRIMARY_IMAGE("entity:primary_image", "Primay Image", java.sql.Types.VARCHAR);
		
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
		public Object getValue(IntelEntity entity) throws IOException {
			if (this == ENTITY_UUID) return entity.getUuid();
			if (this == ID) return entity.getIdAttributeAsText();
			if (this == TYPE_KEY) return entity.getEntityType().getKeyId();
			if (this == TYPE) return entity.getEntityType().getName();
			if (this == DATE_CREATED) return entity.getDateCreated();
			if (this == DATE_MODIFIED) return entity.getDateModified();
			if (this == CREATED_BY) return MessageFormat.format("{0} {1}", entity.getCreatedBy().getGivenName(), entity.getCreatedBy().getFamilyName());
			if (this == MODIFIED_BY) return MessageFormat.format("{0} {1}", entity.getLastModifiedBy().getGivenName(), entity.getLastModifiedBy().getFamilyName());
			if (this == PRIMARY_IMAGE) return "file:/" + entity.getPrimaryAttachment().getAttachmentFile().getCanonicalPath();
			return null;
		}
	}
	
	private IntelEntityType type;
	
	private List<String> attributeColumnNames;
	private List<String> attributeColumnLabels;
	
	public EntityDatasetResultSetMetadata(IntelEntityType type){
		this.type = type;
		
		HashSet<String> fixedLabels = new HashSet<>();
		for (Column c : Column.values()){
			fixedLabels.add(c.name);
		}
		attributeColumnNames = new ArrayList<String>(type.getAttributes().size());
		attributeColumnLabels = new ArrayList<String>(type.getAttributes().size());
		for (IntelEntityTypeAttribute attribute : type.getAttributes()){
			String corelabel = attribute.getAttribute().getName();
			String name = "attribute:" + attribute.getAttribute().getKeyId();
			
			attributeColumnNames.add(name);
			int add = 1;
			String label = corelabel;
			while(attributeColumnLabels.contains(label) || fixedLabels.contains(label)){
				label = corelabel + "_" + add;
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
		if (index <= 8) return Column.values()[index-1].name;
		index = index - 9;
		if (index < type.getAttributes().size()){
			return attributeColumnLabels.get(index);
		}
		return Column.PRIMARY_IMAGE.name;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		if (index <= 8) return Column.values()[index-1].id;
		index = index - 9;
		if (index < type.getAttributes().size()){
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
		if (index < type.getAttributes().size()){
			IAttributeType attType = type.getAttributes().get(index).getAttribute().getType();
			return AttributeManager.INSTANCE.getAttributeSqlType(attType);
		}
		return Column.PRIMARY_IMAGE.type;
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
