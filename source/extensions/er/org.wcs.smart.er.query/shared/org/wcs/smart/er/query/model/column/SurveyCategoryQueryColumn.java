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
package org.wcs.smart.er.query.model.column;

import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Data model query column for survey queries.
 * 
 * @author Emily
 *
 */
public class SurveyCategoryQueryColumn extends CategoryQueryColumn {

	public SurveyCategoryQueryColumn(String name, int level) {
		super(name, level);
	}

	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof SurveyQueryResultItem){
			String[] items = ((SurveyQueryResultItem) item).getCategories();
			if (items == null){
				return ""; //$NON-NLS-1$
			}
			if (level < items.length){
				return items[level];
			}else{
				return ""; //$NON-NLS-1$
			}
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		QueryColumn clone = new SurveyCategoryQueryColumn(getName(), level);
		clone.setEdit(canEdit());
		return clone;
	}

}
