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

import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.model.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Class represents one of the fixed table columns that
 * do not change from conservation area to conservation area
 * specific to survey queries.
 * 
 * <p>This includes items such as the survey design, survey id etc.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveyQueryColumn extends QueryColumn {

	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID(Messages.SurveyQueryColumn_CaIdLabel, ColumnType.STRING,"ca:id"),  //$NON-NLS-1$
		CA_NAME(Messages.SurveyQueryColumn_CaNameLabel, ColumnType.STRING,"ca:name"),  //$NON-NLS-1$
		SURVEY_DESIGN(Messages.SurveyQueryColumn_SurveyDesignLabel, ColumnType.STRING, "surveydesign:id"),  //$NON-NLS-1$
		SURVEY_DESIGN_START(Messages.SurveyQueryColumn_SurveyDesignStartdateLabel, ColumnType.DATE, "surveydesign:startdate"),  //$NON-NLS-1$
		SURVEY_DESIGN_END(Messages.SurveyQueryColumn_SurveyDesignEnddateLabel, ColumnType.DATE, "surveydesign:enddate"),  //$NON-NLS-1$
		
		SURVEY(Messages.SurveyQueryColumn_SurveyIdLabel, ColumnType.STRING, "survey:id"),  //$NON-NLS-1$
		SURVEY_START(Messages.SurveyQueryColumn_SurveyStartLabel, ColumnType.DATE, "survey:startdate"),  //$NON-NLS-1$
		SURVEY_END(Messages.SurveyQueryColumn_SurveyEndLabel, ColumnType.DATE, "survey:enddate"),  //$NON-NLS-1$
		
		MISSION(Messages.SurveyQueryColumn_MissionIdLabel, ColumnType.STRING, "mission:id"),  //$NON-NLS-1$
		MISSION_START(Messages.SurveyQueryColumn_MissionStartLabel, ColumnType.DATE, "mission:startdate"),  //$NON-NLS-2$ //$NON-NLS-1$
		MISSION_END(Messages.SurveyQueryColumn_MissionEndLabel, ColumnType.DATE, "mission:enddate"), //$NON-NLS-2$ //$NON-NLS-1$
		
		SAMPLING_UNIT(Messages.SurveyQueryColumn_SuLabel, ColumnType.STRING, "su:id"),  //$NON-NLS-1$
		SMAPLING_UNIT_BUFFER(Messages.SurveyQueryColumn_SuBufferLabel, ColumnType.DATE, "su:buffer"),  //$NON-NLS-1$
		
		WAYPOINT_ID(Messages.SurveyQueryColumn_WaypointIdLabel, ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE(Messages.SurveyQueryColumn_WpDateLabel, ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME(Messages.SurveyQueryColumn_WaypointTypeLabel, ColumnType.TIME,"waypoint:time"),  //$NON-NLS-1$
		WAYPOINT_X(Messages.SurveyQueryColumn_XLabel, ColumnType.NUMBER,"waypoint:x"),  //$NON-NLS-1$
		WAYPOINT_Y(Messages.SurveyQueryColumn_YLabel, ColumnType.NUMBER, "waypoint:y"),  //$NON-NLS-1$
		WAYPOINT_DIRECTION(Messages.SurveyQueryColumn_DirectionLabel, ColumnType.NUMBER,"waypoint:direction"),  //$NON-NLS-1$
		WAYPOINT_DISTANCE(Messages.SurveyQueryColumn_DistanceLabel, ColumnType.NUMBER,"waypoint:distance"),  //$NON-NLS-1$
		WAYPOINT_COMMENT(Messages.SurveyQueryColumn_CommentLabel, ColumnType.STRING,"waypoint:comment");  //$NON-NLS-1$
		
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
		if (queryResultItem instanceof SurveyQueryResultItem){
			SurveyQueryResultItem item = (SurveyQueryResultItem)queryResultItem;
			switch(column){
				case CA_ID: return item.getConservationAreaId();
				case CA_NAME: return item.getConservationAreaName();
				case SURVEY_DESIGN: return item.getSurveyDesign();
				case SURVEY_DESIGN_START: return item.getSurveyDesignStart();
				case SURVEY_DESIGN_END: return item.getSurveyDesignEnd();
				case SURVEY: return item.getSurveyId();
				case SURVEY_START: return item.getSurveyStart();
				case SURVEY_END: return item.getSurveyEnd();
				case MISSION: return item.getMissionId();
				case MISSION_START: return item.getMissionStart();
				case MISSION_END: return item.getMissionEnd();
				case SAMPLING_UNIT: return item.getSamplingUnitId();
				case SMAPLING_UNIT_BUFFER: return item.getSmaplingUnitBuffer();
				case WAYPOINT_ID: return item.getWaypointId();
				case WAYPOINT_DATE: return item.getWaypointTime();
				case WAYPOINT_TIME: return item.getWaypointTime(); 
				case WAYPOINT_X: return item.getWaypointX(); 
				case WAYPOINT_Y: return item.getWaypointY(); 
				case WAYPOINT_DIRECTION: return item.getWaypointDirection(); 
				case WAYPOINT_DISTANCE: return item.getWaypointDistance(); 
				case WAYPOINT_COMMENT: return item.getWaypointComment(); 
			}
		}
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
