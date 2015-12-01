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

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Sampling unit attribute query column.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeQueryColumn  extends QueryColumn {
	
	public static final String KEY_PREFIX = "suatt"; //$NON-NLS-1$
	
	/**
	 * Creates a new query column based on the mission attribute.
	 * 
	 * @param mp
	 */
	public SamplingUnitAttributeQueryColumn(String name, SamplingUnitAttribute sua){
		super(name, KEY_PREFIX + ":" + sua.getKeyId(), null);  //$NON-NLS-1$
		if (sua.getType() == AttributeType.NUMERIC){
			super.setType(ColumnType.NUMBER);
		}else if (sua.getType() == AttributeType.TEXT){
			super.setType(ColumnType.STRING);
		}else if (sua.getType() == AttributeType.LIST){
			super.setType(ColumnType.STRING);
		}
	}
	
	/**
	 * Creates a new query column with the given attribute.
	 * 
	 * @param name column name
	 * @param key column key
	 * @param type column type
	 */
	protected SamplingUnitAttributeQueryColumn(String name, String key, ColumnType type){
		super(name, key, type);
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof SurveyQueryResultItem){
			SurveyQueryResultItem i = (SurveyQueryResultItem) item;			
			String attributeKey = getKey().split(":")[1]; //$NON-NLS-1$
			return i.getSamplingUnitAttributeValue(attributeKey);
//		}else if (item instanceof MissionTrackResultItem){
//			MissionTrackResultItem i = (MissionTrackResultItem) item;			
//			String attributeKey = getKey().split(":")[1]; //$NON-NLS-1$
//			return i.getSamplingUnitAttributeValue(attributeKey);
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		return new SamplingUnitAttributeQueryColumn(getName(), getKey(), getType());
	}

}
