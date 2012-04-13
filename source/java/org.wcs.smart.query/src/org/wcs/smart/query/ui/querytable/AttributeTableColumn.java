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
package org.wcs.smart.query.ui.querytable;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.model.QueryResultItem;


/**
 * An table column in the results table that represents an attribute.
 * <p>There should be one column for each attribute
 * defined in the data model</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTableColumn implements QueryTableColumn {

	private String name;
	private ColumnLabelProvider provider = null;
	private String key;
	private QueryTableColumn.ColumnType type;
	
	/**
	 * Creates a new attribute column.
	 * 
	 * @param name the column name as it appears to the user
	 * @param key the attribute key
	 * @param type the type of the attribute column
	 */
	public AttributeTableColumn(String name, String key, AttributeType type){
		this.name = name;
		this.key = key;
		
		if (type == AttributeType.NUMERIC ){
			this.type = ColumnType.NUMBER;
		}else if (type == AttributeType.BOOLEAN){
			this.type = ColumnType.BOOLEAN;
		}else {
			this.type = ColumnType.STRING;
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getType()
	 */
	public QueryTableColumn.ColumnType getType(){
		return this.type;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getName()
	 */
	@Override
	public String getKey() {
		return "attribute:" + this.key;
	}

	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getLabelProvider()
	 */
	@Override
	public ColumnLabelProvider getLabelProvider() {
		if (provider == null){
			provider = getAttributeLabelProvider(this);
		}
		return provider;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getValue(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	public Object getValue(QueryResultItem item) {
		return item.getAttributeValue(key);
	}
	
	private static ColumnLabelProvider getAttributeLabelProvider(final AttributeTableColumn column){
		
		ColumnLabelProvider provider = new ColumnLabelProvider(){
			/* 
			 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem){
					Object value = column.getValue((QueryResultItem)element);
					if (value == null){
						return "";
					}else{
						return value.toString();
					}
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};
		
		return provider;
	}



}
