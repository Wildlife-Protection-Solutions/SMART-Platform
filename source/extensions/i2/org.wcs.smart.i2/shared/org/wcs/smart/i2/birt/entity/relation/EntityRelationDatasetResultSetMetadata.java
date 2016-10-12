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
package org.wcs.smart.i2.birt.entity.relation;

import java.util.List;
import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.i2.AttributeManager;
import org.wcs.smart.i2.birt.datasource.IntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.util.UuidUtils;

/**
 * Entity record datasets results metadata
 * @author Emily
 *
 */
public class EntityRelationDatasetResultSetMetadata implements IResultSetMetaData {
	
	private List<IntelAttribute> validAttributes;
	
	public enum Column{
		ENTITY_UUID("relation:entity_uuid", "Entity UUID", java.sql.Types.VARCHAR),
		ENTITY_ID("relation:entity_id", "Entity ID", java.sql.Types.VARCHAR),
		RELATION_UUID("relation:relation_uuid", "Relation UUID", java.sql.Types.VARCHAR),
		RELATION_ID("relation:relation_id", "Relation", java.sql.Types.VARCHAR),
		GROUP_NAME("relation:group_name", "Group", java.sql.Types.VARCHAR),
		GROUP_KEY("relation:group_key", "Group Key", java.sql.Types.VARCHAR),
		RELATIONSHIP_TYPE("relation:relationship_type", "Relationship Type", java.sql.Types.VARCHAR),
		RELATIONSHIP_TYPE_KEY("relation:relationship_type_key", "Relationship Type Key", java.sql.Types.VARCHAR);
		
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
		
		public Object getValue(UUID entity, IntelEntityRelationship relation) {
			switch(this){
			case ENTITY_UUID:
				if (entity == null){
					return relation.getSourceEntity().getUuid();
				}else if (entity.equals(relation.getSourceEntity().getUuid())){
					return relation.getSourceEntity().getUuid();
				}else if (entity.equals(relation.getTargetEntity().getUuid())){
					return relation.getTargetEntity().getUuid();
				}
				break;
			case ENTITY_ID:
				if (entity == null){
					return relation.getSourceEntity().getIdAttributeAsText();
				}else if (entity.equals(relation.getSourceEntity().getUuid())){
					return relation.getSourceEntity().getIdAttributeAsText();
				}else if (entity.equals(relation.getTargetEntity().getUuid())){
					return relation.getTargetEntity().getIdAttributeAsText();
				}
				break;
			case GROUP_KEY:
				if (relation.getRelationshipType().getRelationshipGroup() == null) return "";
				return relation.getRelationshipType().getRelationshipGroup().getKeyId();
			case GROUP_NAME:
				if (relation.getRelationshipType().getRelationshipGroup() == null) return "";
				return relation.getRelationshipType().getRelationshipGroup().getName();
			case RELATIONSHIP_TYPE:
				return relation.getRelationshipType().getName();
			case RELATIONSHIP_TYPE_KEY:
				return relation.getRelationshipType().getKeyId();
			case RELATION_ID:
				if (entity == null){
					return relation.getTargetEntity().getIdAttributeAsText();
				}else if (entity.equals(relation.getSourceEntity().getUuid())){
					return relation.getTargetEntity().getIdAttributeAsText();
				}else if (entity.equals(relation.getTargetEntity().getUuid())){
					return relation.getSourceEntity().getIdAttributeAsText();
				}
				break;
			case RELATION_UUID:
				if (entity == null){
					return UuidUtils.uuidToString(relation.getTargetEntity().getUuid());
				}else if (entity.equals(relation.getSourceEntity().getUuid())){
					return UuidUtils.uuidToString(relation.getTargetEntity().getUuid());
				}else if (entity.equals(relation.getTargetEntity().getUuid())){
					return UuidUtils.uuidToString(relation.getSourceEntity().getUuid());
				}
				break;
			default:
				break;
			}
			return null;
		}
	}
	
	public EntityRelationDatasetResultSetMetadata(List<IntelAttribute> attributes){
		this.validAttributes = attributes;
	}
	
	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnCount()
	 */
	@Override
	public int getColumnCount() throws OdaException {
		return Column.values().length + validAttributes.size();
		
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
		if (index <= Column.values().length){
			return Column.values()[index-1].name;
		}
		index = index - 1 - Column.values().length;
		if (index >= 0){
			return validAttributes.get(index).getName();
		}
		return null;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnName(int)
	 */
	@Override
	public String getColumnName(int index) throws OdaException {
		if (index <= Column.values().length){
			return Column.values()[index-1].id;
		}
		index = index - 1 - Column.values().length;
		if (index >= 0){
			return "attribute:" + validAttributes.get(index).getKeyId();
		}
		return null;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnType(int)
	 */
	@Override
	public int getColumnType(int index) throws OdaException {
		if (index <= Column.values().length){
			return Column.values()[index-1].type;
		}
		index = index - 1 - Column.values().length;
		if (index >= 0){
			return AttributeManager.INSTANCE.getAttributeSqlType(validAttributes.get(index).getType());
		}
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return IntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityRelationDataset.DATASET_TYPE );
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
