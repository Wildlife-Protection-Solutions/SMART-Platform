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
package org.wcs.smart.query.parser.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

/**
 * A query date filter.
 * 
 * <p>Date filters take the form</p>
 *
 * DateField DateFilter<br>
 * or<br>
 * DateField DateFilter Date1 Date2<br>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateFilter implements IFilter {

	/**
	 * Possible date fields for date filters.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public static enum DATE_FIELD_OP{
		WAYPOINT(Messages.DateFilter_WaypointOp, "patrol:waypointdate", "patrol_day"),	 //$NON-NLS-2$ //$NON-NLS-1$
		PATROL_START(Messages.DateFilter_StartDateOp, "patrol:startdate", "start_date"), //$NON-NLS-2$ //$NON-NLS-1$
		PATROL_END(Messages.DateFilter_EndDateOp, "patrol:enddate", "end_date"); //$NON-NLS-2$ //$NON-NLS-1$
			
		public String guiName;
		public String key;
		public String columnName;
			
		private DATE_FIELD_OP(String name, String key, String columnName){
			this.guiName = name;
			this.key = key;
			this.columnName = columnName;
		}
	}
	
	
	
	private DATE_FIELD_OP dateField;
	private PatrolQueryOptions.DATE_FILTER_OP dateFilter;
	
	private java.sql.Date startDate;
	private java.sql.Date endDate;
	
	public DateFilter(DATE_FIELD_OP dateField, PatrolQueryOptions.DATE_FILTER_OP dateFilter){
		this.dateField  = dateField;
		this.dateFilter = dateFilter;
	}
	
	public DateFilter(DATE_FIELD_OP dateField, PatrolQueryOptions.DATE_FILTER_OP dateFilter, java.sql.Date startDate, java.sql.Date endDate){
		this.dateField  = dateField;
		this.startDate = startDate;
		this.endDate = endDate;
		this.dateFilter = dateFilter;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		String str = dateField.key + " " + dateFilter.key; //$NON-NLS-1$
		if (startDate != null){
			str += " " + startDate.toString(); //$NON-NLS-1$
		}else if (endDate != null){
			str += " " + endDate.toString(); //$NON-NLS-1$
		}
		return str;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping, HashMap<IFilter, String> filterTables){
		String tablePrefix = ""; //$NON-NLS-1$
		if (this.dateField == DATE_FIELD_OP.PATROL_END || this.dateField == DATE_FIELD_OP.PATROL_START){
			tablePrefix = tableMapping.get(Patrol.class);
		}else if (this.dateField == DATE_FIELD_OP.WAYPOINT){
			tablePrefix = tableMapping.get(PatrolLegDay.class);
		}else{
			assert false;
			//error
		}
		String field = tablePrefix + "." + dateField.columnName; //$NON-NLS-1$
		return asSql(field);
	}
	
	/**
	 * @return the date filter option represented by this date filter
	 */
	public PatrolQueryOptions.DATE_FILTER_OP getDateFilterOption(){
		return this.dateFilter;
	}
	
	/**
	 * @return the date field option represented by this date filter
	 */
	public DateFilter.DATE_FIELD_OP getDateFieldOption(){
		return this.dateField;
	}
	
	
	/**
	 * Determine start and end date of date filter  
	 * 
	 * @param op the date filter
	 * @return the dates associated with the date filter
	 */
	public java.sql.Date[] getDates() {
		java.sql.Date[] dates = dateFilter.getDates();
		if (dates == null){
			if (dateFilter == PatrolQueryOptions.DATE_FILTER_OP.CUSTOM){
				return new java.sql.Date[]{startDate, endDate};
			}
		}
		return dates;

	}
	
	private String asSql(String field){
		java.sql.Date[] bits = getDates(); 
		String f = ""; //$NON-NLS-1$
		if (bits == null){
			return ""; //$NON-NLS-1$
		}
		if (bits.length == 1){
			f = " ( " +field + " >= '" + bits[0].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (bits.length == 2 && (
				dateFilter == PatrolQueryOptions.DATE_FILTER_OP.LAST_MONTH || 
				dateFilter == PatrolQueryOptions.DATE_FILTER_OP.LAST_YEAR ||
				dateFilter == PatrolQueryOptions.DATE_FILTER_OP.LAST_QUARTER)){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " < '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else if (bits.length == 2){
			f = " ( " + field + " >= '" + bits[0].toString() + "' and " + field  + " <= '" + bits[1].toString() + "' ) "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		return f;
	}


	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * There are no drop items for dates
	 * @return null
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		return null;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}
	
}
