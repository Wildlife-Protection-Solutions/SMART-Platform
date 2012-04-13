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
import org.wcs.smart.query.model.QueryResultItem;

/**
 * 
 * A column in the results table that contains 
 * a category from the datamodel.
 * 
 * <p>There should be one column for each
 * "level" in the datamodel category tree.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CategoryTableColumn implements QueryTableColumn{

	
	private String name;
	private String key;
	private ColumnLabelProvider provider = null;
	private int level;	//the category level in the database.
		
	/**
	 * Creates a new category column
	 * 
	 * @param name the name
	 * @param level the level in the data model this column represents
	 */
	public CategoryTableColumn(String name, int level){
		this.name = name;
		this.level = level;
		this.key = "category:" + level;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getName()
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getKey()
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getType()
	 */
	public QueryTableColumn.ColumnType getType(){
		return ColumnType.STRING;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getLabelProvider()
	 */
	public ColumnLabelProvider getLabelProvider(){
		if (provider == null){
			provider = getCategoryLabelProvider(this.level);
			
		}
		return provider;
	}
	
	/**
	 * @see org.wcs.smart.query.ui.querytable.QueryTableColumn#getValue(org.wcs.smart.query.model.QueryResultItem)
	 */
	@Override
	public Object getValue(QueryResultItem item) {
		return getItemValue(item, level);
	}
	
	
	private static ColumnLabelProvider getCategoryLabelProvider(final int cat){
		
		ColumnLabelProvider provider = new ColumnLabelProvider(){
			/* 
			 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
			 */
			public String getText(Object element) {
				if (element instanceof QueryResultItem){
					return getItemValue((QueryResultItem)element, cat);
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		};
		
		return provider;
	}

	private static String getItemValue(QueryResultItem item, int level) {
		String[] items = item.getCategories();
		if (items == null){
			return "";
		}
		if (level < items.length){
			return items[level];
		}else{
			return "";
		}
	}
}
