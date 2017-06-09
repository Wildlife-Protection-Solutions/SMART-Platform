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
package org.wcs.smart.patrol.query.model.observation;

import org.wcs.smart.patrol.query.model.PatrolQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

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
public class PatrolCategoryQueryColumn extends CategoryQueryColumn{

	
	/**
	 * Creates a new category column
	 * 
	 * @param name the name
	 * @param level the level in the data model this column represents
	 */
	public PatrolCategoryQueryColumn(String name, int level){
		super(name,level);
	}
	

	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.patrol.query.model.PatrolQueryResultItem)
	 */
	@Override
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof PatrolQueryResultItem) {
			PatrolQueryResultItem item = (PatrolQueryResultItem) queryResultItem;
			return getItemValue(item, level);
		}
		return ""; //$NON-NLS-1$
	}
	
	private static String getItemValue(PatrolQueryResultItem item, int level) {
		String[] items = item.getCategories();
		if (items == null){
			return ""; //$NON-NLS-1$
		}
		if (level < items.length){
			return items[level];
		}else{
			return ""; //$NON-NLS-1$
		}
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		PatrolCategoryQueryColumn newColumn = new PatrolCategoryQueryColumn(super.getName(), level);
		newColumn.setEdit(canEdit());
		return newColumn;
	}

}
