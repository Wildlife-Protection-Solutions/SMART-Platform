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
package org.wcs.smart.intelligence.query.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Class represents one of the fixed table columns that
 * do not change from conservation area to conservation area.
 * 
 * <p>This includes items such as the patrol id, patrol type etc
 * but not items related to the datamodel.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class FixedQueryColumn extends QueryColumn {

	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID( ColumnType.STRING,"ca:id"),  //$NON-NLS-1$
		CA_NAME( ColumnType.STRING,"ca:name"),  //$NON-NLS-1$
		INTEL_NAME( ColumnType.STRING, "intel:name"),   //$NON-NLS-1$
		INTEL_DATE_RECIEVED( ColumnType.DATE, "intel:datereceived"),   //$NON-NLS-1$
		INTEL_DATE_FROM( ColumnType.DATE, "intel:fromdate"),   //$NON-NLS-1$
		INTEL_DATE_TO( ColumnType.DATE, "intel:todate"),  //$NON-NLS-1$
		INTEL_SOURCE( ColumnType.STRING, "intel:source"),   //$NON-NLS-1$
		INTEL_PATROL_SOURCE( ColumnType.STRING, "intel:patrolid"), //$NON-NLS-1$
		INTEL_INFORMANT_ID( ColumnType.STRING, "intel:informantid"),   //$NON-NLS-1$
		INTEL_DESCRIPTION( ColumnType.STRING, "intel:description");  //$NON-NLS-1$
		
		public ColumnType type;
		public String key;
		
		private FixedColumns( ColumnType type, String key){
			this.type = type;
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IIntelligenceQueryLabelProvider.class).getLabel(this, l);
		}
	}
	
	
	private FixedColumns column;
	private Locale l;
	
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public FixedQueryColumn(FixedColumns column, Locale l) {
		super(column.getGuiName(l), column.key, column.type);
		this.column = column;
		this.l = l;
	}



	/**)
	 * @see org.wcs.smart.query.model.QueryColumn#getValue(org.wcs.smart.query.common.engine.IResultItem)
	 */
	public Object getValue(IResultItem queryResultItem) {
		if (queryResultItem instanceof IntelligenceRecordResultItem){
			IntelligenceRecordResultItem r = (IntelligenceRecordResultItem)queryResultItem;
			switch(column){
				case CA_ID: return r.getConservationAreaId();
				case CA_NAME: return r.getConservationAreaName();
				case INTEL_DATE_FROM: return r.getFromDate();
				case INTEL_DATE_TO: return r.getToDate();
				case INTEL_DATE_RECIEVED:return r.getReceivedDate();
				case INTEL_DESCRIPTION:return r.getDescription();
				case INTEL_INFORMANT_ID: return r.getInformantId();
				case INTEL_NAME: return r.getName();
				case INTEL_PATROL_SOURCE: return r.getPatrolId();
				case INTEL_SOURCE: return r.getSource();
					
			}
		}
		return null; 
	}
	
	public String getValueAsString(IResultItem queryResultItem) {
		Object x = getValue(queryResultItem);
		if (x == null){
			return ""; //$NON-NLS-1$
		}else if (x instanceof String){
			return (String)x;
		}else if (x instanceof Date){
			return DateFormat.getDateInstance().format((Date)x); 
		}
		return x.toString();
	}

	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column, this.l);
		return newColumn;
	}
}
