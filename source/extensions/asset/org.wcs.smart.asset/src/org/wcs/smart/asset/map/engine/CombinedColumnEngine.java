package org.wcs.smart.asset.map.engine;

import org.wcs.smart.asset.ui.views.map.CombinedOverviewColumn;
import org.wcs.smart.asset.ui.views.map.StationData;

public class CombinedColumnEngine {

	
	public static Double computeValue(StationData data, CombinedOverviewColumn column) {
		
		IFilter filter = column.getParsedExpression();
		Double expression = asSql(filter, data);
		if (expression == null) return Double.NaN;
		
		return expression;
//		
//		Object value1 = data.getData(column1);
//		Object value2 = data.getData(column2);
//		if (value1 == null || value2 == null) return 0;
//		
//		if (value1 != null && value2 != null && value1 instanceof Number && value2 instanceof Number) {
//			if (((Number)value2).doubleValue() == 0) return Double.NaN;
//			return ((Number)value1).doubleValue() / ((Number)value2).doubleValue() ;
//			
//		}
//		return Double.NaN;
	}
	
	private static Double asSql(IFilter filter, StationData data) {
		if (filter instanceof CombinedFilter) {
			return asSql((CombinedFilter)filter, data);
		}else if (filter instanceof ColumnFilter) {
			return asSql((ColumnFilter)filter, data);
		}else if (filter instanceof BracketFilter) {
			return asSql((BracketFilter)filter, data);
		}
		return null;
	}
	
	
	private static Double asSql(CombinedFilter filter, StationData data) {
		Double part1 = asSql(filter.getFilter1(), data);
		Double part2 = asSql(filter.getFilter2(), data);
		if (part1 == null || part2 == null) return null;
		switch(filter.getOperator().operator) {
		case DIVIDE: return part1 / part2;
		case MINUS: return part1 - part2;
		case PLUS: return part1 + part2;
		case TIMES: return part1 * part2;
		}
		return null;
	}
	
	private static Double asSql(ColumnFilter filter, StationData data) {
		String columnKey = filter.getColumnKey();
		Object value = data.getColumnValue(columnKey);
		if (value == null) return null;
		if (value instanceof Double) return ((Double)value);
		if (value instanceof Integer) return ((Integer)value).doubleValue();
		if (value instanceof Long) return ((Long)value).doubleValue();
		if (value instanceof Float) return ((Float)value).doubleValue();
		return null;
	}
	
	private static Double asSql(BracketFilter filter, StationData data) {
//		String value = asSql(filter, data);
//		if (value == null) return null;
//		return " ( " + value + " ) " ;
		return asSql(filter.getFilter(), data);
	}
	
}
