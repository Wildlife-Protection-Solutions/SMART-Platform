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
package org.wcs.smart.query.model;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;


/**
 * An table column in the results table that represents an attribute.
 * <p>There should be one column for each attribute
 * defined in the data model</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AttributeQueryColumn extends QueryColumn {

	protected String attributeKey = null;
	
	/**
	 * Creates a new attribute column.
	 * 
	 * @param name the column name as it appears to the user
	 * @param key the attribute id key
	 * @param type the type of the attribute column
	 */
	public AttributeQueryColumn(String name, String attributeId, AttributeType type){
		super(name, "attribute:" + attributeId, null); //$NON-NLS-1$
		this.attributeKey = attributeId;
		
		ColumnType ctype = ColumnType.STRING;
		if (type == AttributeType.NUMERIC ){
			ctype = ColumnType.NUMBER;
		}else if (type == AttributeType.BOOLEAN){
			ctype = ColumnType.BOOLEAN;
		}else {
			ctype = ColumnType.STRING;
		}
		super.setType(ctype);
	}
	
	/**
	 * Creates a new column with the given column type.
	 * @param name
	 * @param key the query column full key of the form "attribute:<ATTRIBUTEID>"
	 * @param type
	 */
	public AttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
		this.attributeKey = key.split(":")[1]; //$NON-NLS-1$
	}

}
