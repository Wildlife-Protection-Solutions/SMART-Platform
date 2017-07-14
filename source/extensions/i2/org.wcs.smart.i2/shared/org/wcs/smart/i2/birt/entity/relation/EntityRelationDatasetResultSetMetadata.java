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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.IntelHibernateManager;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityRelationship;

/**
 * Entity record datasets results metadata
 * @author Emily
 *
 */
public class EntityRelationDatasetResultSetMetadata implements IResultSetMetaData {
	
	private List<IntelAttribute> validAttributes;
	
	public static enum Column{
		ENTITY_UUID("relation:entity_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SOURCE_RELATION_UUID("relation:source_uuid", java.sql.Types.VARCHAR), //$NON-NLS-1$
		SOURCE_RELATION_ID("relation:source_id",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		TARGET_RELATION_UUID("relation:target_uuid",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		TARGET_RELATION_ID("relation:garget_id", java.sql.Types.VARCHAR), //$NON-NLS-1$
		GROUP_NAME("relation:group_name",  java.sql.Types.VARCHAR), //$NON-NLS-1$
		GROUP_KEY("relation:group_key", java.sql.Types.VARCHAR), //$NON-NLS-1$
		RELATIONSHIP_TYPE("relation:relationship_type", java.sql.Types.VARCHAR), //$NON-NLS-1$
		RELATIONSHIP_TYPE_KEY("relation:relationship_type_key",  java.sql.Types.VARCHAR); //$NON-NLS-1$
		
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
		
		public Object getValue(UUID entity, IntelEntityRelationship relation, Locale l) {
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
			case GROUP_KEY:
				if (relation.getRelationshipType().getRelationshipGroup() == null) return ""; //$NON-NLS-1$
				return relation.getRelationshipType().getRelationshipGroup().getKeyId();
			case GROUP_NAME:
				if (relation.getRelationshipType().getRelationshipGroup() == null) return ""; //$NON-NLS-1$
				return relation.getRelationshipType().getRelationshipGroup().getName();
			case RELATIONSHIP_TYPE:
				return relation.getRelationshipType().getName();
			case RELATIONSHIP_TYPE_KEY:
				return relation.getRelationshipType().getKeyId();
			case TARGET_RELATION_ID:
				return relation.getTargetEntity().getIdAttributeAsText(l);
			case TARGET_RELATION_UUID:
				return relation.getTargetEntity().getUuid();
			case SOURCE_RELATION_ID:
				return relation.getSourceEntity().getIdAttributeAsText(l);
			case SOURCE_RELATION_UUID:
				return relation.getSourceEntity().getUuid();
			
			default:
				break;
			}
			return null;
		}
	}
	
	private String[] columnNames;
	
	public EntityRelationDatasetResultSetMetadata(List<IntelAttribute> attributes, Locale l){
		this.validAttributes = attributes;
	
		HashSet<String> duplicates = new HashSet<String>();
		columnNames = new String[Column.values().length + validAttributes.size()];
		for (int i = 0; i < Column.values().length; i ++){
			columnNames[i] = Column.values()[i].getColumnName(l);
			duplicates.add(columnNames[i]);
		}
		//configure attribute column names removing duplicates
		for (int i = 0; i < validAttributes.size(); i ++){
			String rawpart = validAttributes.get(i).getName();
			String name = rawpart;
			int k = 1;
			while(duplicates.contains(name)){
				name = rawpart + k;
				k++;
			}
			columnNames[i + Column.values().length] = name;
		}	
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
	//index is 1 based
	@Override
	public String getColumnLabel(int index) throws OdaException {
		if (index <= columnNames.length) return columnNames[index-1];
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
			return "attribute:" + validAttributes.get(index).getKeyId(); //$NON-NLS-1$
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
			return IntelHibernateManager.getAttributeSqlType(validAttributes.get(index).getType());
		}
		return -1;
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IResultSetMetaData#getColumnTypeName(int)
	 */
	@Override
	public String getColumnTypeName(int index) throws OdaException {
		 int nativeTypeCode = getColumnType( index );
	     return AbstractIntelBirtConnection.getNativeDataTypeName( nativeTypeCode, EntityRelationDataset.DATASET_TYPE );
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
