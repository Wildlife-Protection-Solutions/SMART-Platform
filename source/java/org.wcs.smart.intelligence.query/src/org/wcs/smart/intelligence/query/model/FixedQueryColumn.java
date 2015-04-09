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

import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.query.model.IResultItem;
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
		CA_ID(Messages.FixedQueryColumn_CaIdCol, ColumnType.STRING,"ca:id"),  //$NON-NLS-1$
		CA_NAME(Messages.FixedQueryColumn_CaNameCol, ColumnType.STRING,"ca:name"),  //$NON-NLS-1$
		INTEL_NAME(Messages.FixedQueryColumn_NameCol, ColumnType.STRING, "intel:name"),   //$NON-NLS-1$
		INTEL_DATE_RECIEVED(Messages.FixedQueryColumn_ReceivedDateCol, ColumnType.DATE, "intel:datereceived"),   //$NON-NLS-1$
		INTEL_DATE_FROM(Messages.FixedQueryColumn_FromDateColumn, ColumnType.DATE, "intel:fromdate"),   //$NON-NLS-1$
		INTEL_DATE_TO(Messages.FixedQueryColumn_ToDateColumn, ColumnType.DATE, "intel:todate"),  //$NON-NLS-1$
		INTEL_SOURCE(Messages.FixedQueryColumn_SourceColumn, ColumnType.STRING, "intel:source"),   //$NON-NLS-1$
		INTEL_PATROL_SOURCE(Messages.FixedQueryColumn_PatrolColumn, ColumnType.STRING, "intel:patrolid"), //$NON-NLS-1$
		INTEL_INFORMANT_ID(Messages.FixedQueryColumn_InformantCol, ColumnType.STRING, "intel:informantid"),   //$NON-NLS-1$
		INTEL_DESCRIPTION(Messages.FixedQueryColumn_DescriptionCol, ColumnType.STRING, "intel:description");  //$NON-NLS-1$
		
		public String guiName;
		public ColumnType type;
		public String key;
		
		private FixedColumns(String name, ColumnType type, String key){
			this.guiName = name;
			this.type = type;
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
	}
	
	
	private FixedColumns column;
	
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public FixedQueryColumn(FixedColumns column) {
		super(column.guiName, column.key, column.type);
		this.column = column;
	}



	/**)
	 * @see org.wcs.smart.query.model.QueryColumn#getValue(org.wcs.smart.query.model.IResultItem)
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
		FixedQueryColumn newColumn = new FixedQueryColumn(this.column);
		return newColumn;
	}
}
