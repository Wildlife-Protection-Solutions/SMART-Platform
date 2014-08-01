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
public class SurveyQueryColumn extends QueryColumn {

	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID("Conservation Area Id", ColumnType.STRING,"ca:id"), 
		CA_NAME("Conservation Area Name", ColumnType.STRING,"ca:name"), 
		SURVEY_DESIGN("Survey Design", ColumnType.STRING, "surveydesign:id"), 
		SURVEY_DESIGN_START("Survey Design Start Date", ColumnType.DATE, "surveydesign:startdate"), 
		SURVEY_DESIGN_END("Survey Design End Date", ColumnType.DATE, "surveydesign:enddate"), 
		
		SURVEY("Survey Id", ColumnType.STRING, "survey:id"), 
		SURVEY_START("Survey Start Date", ColumnType.DATE, "survey:startdate"), 
		SURVEY_END("Survey End Date", ColumnType.DATE, "survey:enddate"), 
		
		MISSION("Mission Id", ColumnType.STRING, "mission:id"), 
		MISSION_START("Mission Start Date", ColumnType.DATE, "mission:startdate"), 
		MISSION_END("Mission End Date", ColumnType.DATE, "mission:enddate"),
		
		SAMPLING_UNIT("Sampling Unit", ColumnType.STRING, "su:id"), 
		SMAPLING_UNIT_BUFFER("Sampling Unit Buffer", ColumnType.DATE, "su:buffer"), 
		
		WAYPOINT_ID("Waypoint ID", ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE("Waypoint Date", ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME("Waypoint Type", ColumnType.TIME,"waypoint:time"), //$NON-NLS-1$
		WAYPOINT_X("X", ColumnType.NUMBER,"waypoint:x"), //$NON-NLS-1$
		WAYPOINT_Y("Y", ColumnType.NUMBER, "waypoint:y"), //$NON-NLS-1$
		WAYPOINT_DIRECTION("Direction", ColumnType.NUMBER,"waypoint:direction"), //$NON-NLS-1$
		WAYPOINT_DISTANCE("Distance", ColumnType.NUMBER,"waypoint:distance"), //$NON-NLS-1$
		WAYPOINT_COMMENT("Comment", ColumnType.STRING,"waypoint:comment"); //$NON-NLS-1$
		
		private String guiName;
		private ColumnType type;
		private String key;
		
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
	public SurveyQueryColumn(FixedColumns column) {
		super(column.guiName, column.key, column.type);
		this.column = column;
	}


	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#getValue(org.wcs.smart.patrol.query.model.PatrolQueryResultItem)
	 */
	public Object getValue(IResultItem queryResultItem) {
		//	TODO:
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		SurveyQueryColumn newColumn = new SurveyQueryColumn(this.column);
		return newColumn;
	}
}
