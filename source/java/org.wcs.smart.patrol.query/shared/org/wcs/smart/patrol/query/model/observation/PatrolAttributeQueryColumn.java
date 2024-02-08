/*
 * Copyright (C) 2021 Wildlife Conservation Society
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column for custom patrol attributes.
 * 
 * @author Emily
 *
 */
public class PatrolAttributeQueryColumn extends QueryColumn {

	public static final String PREFIX = "pa"; //$NON-NLS-1$
	private PatrolAttribute pa; 
	
	public PatrolAttributeQueryColumn(PatrolAttribute pa) {
		super(pa.getName(), PREFIX + ":" + pa.getKeyId(), getColumnType(pa)); //$NON-NLS-1$
		this.pa = pa;
	}

	private static ColumnType getColumnType(PatrolAttribute pa) {
		switch (pa.getType()) {
			case BOOLEAN: return ColumnType.BOOLEAN;
			case DATE: return ColumnType.DATE;
			case NUMERIC: return ColumnType.NUMBER;
			case LIST: 
			case MLIST: 
			case TEXT:
			case TREE:return ColumnType.STRING;
			default: return ColumnType.STRING;
		}
	}
	
	@Override
	public Object getValue(IResultItem item) {
		if (item instanceof IPatrolQueryResultItem) {
			IPatrolQueryResultItem qitem = (IPatrolQueryResultItem) item;
			Object value = qitem.getPatrolAttribute(pa.getKeyId());
			
			if (value != null && getType() == QueryColumn.ColumnType.BOOLEAN){
				return Boolean.valueOf((Double)value >= 0.5);
			}else if (value != null && getType() == QueryColumn.ColumnType.DATE){
				return LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
			}
			return value;
	
		}
		return null;
	}

	@Override
	public QueryColumn clone() {
		PatrolAttributeQueryColumn clone = new PatrolAttributeQueryColumn(this.pa);		
		return clone;
	}

}
