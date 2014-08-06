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
package org.wcs.smart.er.query.ui.columns;

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Column that represents a data model attribute.
 * 
 * @author Emily
 *
 */
public class SurveyAttributeQueryColumn extends AttributeQueryColumn {

	public SurveyAttributeQueryColumn(String name, String attributeId, AttributeType type) {
		super(name, attributeId, type);
	}

	/**
	 * Creates a new column with the given column type.
	 * @param name
	 * @param key the query column full key of the form "attribute:<ATTRIBUTEID>"
	 * @param type
	 */
	public SurveyAttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
		this.attributeKey = key.split(":")[1]; //$NON-NLS-1$
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof SurveyQueryResultItem){
			return ((SurveyQueryResultItem) item).getAttributeValue(this.attributeKey);
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new SurveyAttributeQueryColumn(getName(), getKey(), getType());
	}

}
