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
package org.wcs.smart.entity.query.model.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;


/**
 * An table column in the results table that represents an attribute.
 * <p>There should be one column for each attribute
 * defined in the data model</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EntityAttributeQueryColumn extends QueryColumn {

	private String entityKey;
	private String entityAttributeKey;
	
	/**
	 * Builds the column key for an entity attribute query column
	 * 
	 * @param entityTypeKey
	 * @param entityAttributeKey
	 * @return
	 */
	public static final String buildColumnKey(String entityTypeKey,	String entityAttributeKey){
		return entityTypeKey + ":" + entityAttributeKey; //$NON-NLS-1$
	}
	/**
	 * Creates a new attribute column.
	 * 
	 * @param name the column name as it appears to the user
	 * @param key the attribute id key
	 * @param type the type of the attribute column
	 */
	public EntityAttributeQueryColumn(String name, String entityTypeKey, 
			String entityAttributeKey, AttributeType type){
		super(name, buildColumnKey(entityTypeKey,entityAttributeKey), null); 
		
		ColumnType ctype = ColumnType.STRING;
		if (type == AttributeType.NUMERIC ){
			ctype = ColumnType.NUMBER;
		}else if (type == AttributeType.BOOLEAN){
			ctype = ColumnType.BOOLEAN;
		}else {
			ctype = ColumnType.STRING;
		}
		super.setType(ctype);
		
		this.entityAttributeKey = entityAttributeKey;
		this.entityKey = entityTypeKey;
	}

	public EntityAttributeQueryColumn(String name, String entityTypeKey, 
			String entityAttributeKey, ColumnType type){
		super(name, buildColumnKey(entityTypeKey,entityAttributeKey), type); 
		this.entityAttributeKey = entityAttributeKey;
		this.entityKey = entityTypeKey;
	}

	public String getEntityKey(){
		return this.entityKey;
	}
	
	public String getEntityAttributeKey(){
		return this.entityAttributeKey;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.patrol.query.model.PatrolQueryResultItem)
	 */
	@Override
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof EntityQueryResultItem) {
			EntityQueryResultItem item = (EntityQueryResultItem) queryResultItem;
			Object x = item.getEntityAttributeValue(getKey());
			if (x != null && getType() == QueryColumn.ColumnType.BOOLEAN){
				return Boolean.valueOf((Double)x >= 0.5);
			}
			return x;
		}
		return ""; //$NON-NLS-1$
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		QueryColumn newColumn = new EntityAttributeQueryColumn(getName(),entityKey, entityAttributeKey, getType());
		newColumn.setEdit(canEdit());
		return newColumn;
	}
}
