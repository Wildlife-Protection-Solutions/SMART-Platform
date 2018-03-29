/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.map.engine;

import java.text.MessageFormat;

import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.ui.views.map.CombinedOverviewColumn;
import org.wcs.smart.asset.ui.views.map.StationData;

/**
 * Specialized column engine for computing combined columns
 */
public class CombinedColumnEngine {

	public static void computeValue(StationData data, CombinedOverviewColumn column) {
		try {
			IExpression filter = column.getParsedExpression();
			Double expression = asSql(filter, data);
			if (expression == null) {
				data.setData(column, null);
				return;
			}
			data.setData(column, expression);
		}catch (Exception ex) {
			AssetPlugIn.log(ex.getMessage(), ex);
			data.setData(column, ex);
		}
	}
	
	private static Double asSql(IExpression filter, StationData data) throws Exception{
		if (filter instanceof CombinedExpression) {
			return asSql((CombinedExpression)filter, data);
		}else if (filter instanceof ColumnExpression) {
			return asSql((ColumnExpression)filter, data);
		}else if (filter instanceof BracketExpression) {
			return asSql((BracketExpression)filter, data);
		}
		throw new Exception(MessageFormat.format(Messages.CombinedColumnEngine_expressionNotSupported, filter.getClass().getName()));
	}
	
	
	private static Double asSql(CombinedExpression filter, StationData data) throws Exception {
		Double part1 = asSql(filter.getFilter1(), data);
		Double part2 = asSql(filter.getFilter2(), data);
		if (part1 == null || part2 == null) return null;
		switch(filter.getOperator().operator) {
			case DIVIDE: return part1 / part2;
			case MINUS: return part1 - part2;
			case PLUS: return part1 + part2;
			case TIMES: return part1 * part2;
			default: 
		}
		throw new Exception(MessageFormat.format(Messages.CombinedColumnEngine_OpNotSupported, filter.getOperator().operator.key));
		
	}
	
	private static Double asSql(ColumnExpression filter, StationData data) throws Exception {
		String columnKey = filter.getColumnKey();
		Object value = data.getColumnValue(columnKey);
		
		if (value == null ) return null;
		if (value instanceof Exception ) throw (Exception)value;
		if (value instanceof Double) return ((Double)value);
		if (value instanceof Integer) return ((Integer)value).doubleValue();
		if (value instanceof Long) return ((Long)value).doubleValue();
		if (value instanceof Float) return ((Float)value).doubleValue();
		
		throw new Exception(MessageFormat.format(Messages.CombinedColumnEngine_ValueCannotBeConverted, value.getClass().toString()));
	}
	
	private static Double asSql(BracketExpression filter, StationData data) throws Exception{
		return asSql(filter.getFilter(), data);
	}
	
}
