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
package org.wcs.smart.er.query.model;

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.query.common.engine.IResultItem;
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

	private static final String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		{"waypoint", "wp"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"su_id", "samplingunit_id"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"su_buffer", "samplingunit_buffer"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"wp_time", "wp_date"} //$NON-NLS-1$ //$NON-NLS-2$
	};
		
	/**
	 * The defined fixed columns.
	 */
	public enum FixedColumns{
		CA_ID( ColumnType.STRING,"ca:id"),  //$NON-NLS-1$
		CA_NAME(ColumnType.STRING,"ca:name"),  //$NON-NLS-1$
		SURVEY_DESIGN(ColumnType.STRING, "surveydesign:name"),  //$NON-NLS-1$
		SURVEY_DESIGN_START( ColumnType.DATE, "surveydesign:startdate"),  //$NON-NLS-1$
		SURVEY_DESIGN_END( ColumnType.DATE, "surveydesign:enddate"),  //$NON-NLS-1$
		
		SURVEY( ColumnType.STRING, "survey:id"),  //$NON-NLS-1$
		SURVEY_START( ColumnType.DATE, "survey:startdate"),  //$NON-NLS-1$
		SURVEY_END( ColumnType.DATE, "survey:enddate"),  //$NON-NLS-1$
		
		MISSION( ColumnType.STRING, "mission:id"),  //$NON-NLS-1$
		MISSION_START( ColumnType.DATE, "mission:startdate"),  //$NON-NLS-1$
		MISSION_END( ColumnType.DATE, "mission:enddate"), //$NON-NLS-1$
		MISSION_LEADER(ColumnType.STRING, "mission:leader"), //$NON-NLS-1$
		
		MISSION_TRACKTYPE( ColumnType.STRING, "mission:tracktype"),  //$NON-NLS-1$
		MISSION_TRACKDATE( ColumnType.DATE, "mission:trackdate"),  //$NON-NLS-1$
		
		MISSION_TRACKID( ColumnType.STRING, "mission:trackid"),  //$NON-NLS-1$
		MISSION_TRACKLENGTH( ColumnType.NUMBER, "mission:tracklength"), //$NON-NLS-1$
		
		SAMPLING_UNIT(ColumnType.STRING, "su:id"),  //$NON-NLS-1$
		
		WAYPOINT_ID( ColumnType.INTEGER,"waypoint:id"), //$NON-NLS-1$
		WAYPOINT_DATE( ColumnType.DATE,"waypoint:date"), //$NON-NLS-1$
		WAYPOINT_TIME( ColumnType.TIME,"waypoint:time"),  //$NON-NLS-1$
		WAYPOINT_X( ColumnType.NUMBER,"waypoint:x"),  //$NON-NLS-1$
		WAYPOINT_Y( ColumnType.NUMBER, "waypoint:y"),  //$NON-NLS-1$
		WAYPOINT_DIRECTION( ColumnType.NUMBER,"waypoint:direction"),  //$NON-NLS-1$
		WAYPOINT_DISTANCE( ColumnType.NUMBER,"waypoint:distance"),  //$NON-NLS-1$
		WAYPOINT_COMMENT( ColumnType.STRING,"waypoint:comment"),  //$NON-NLS-1$
		WAYPOINT_OBSERVER( ColumnType.STRING, "waypoint:observer");   //$NON-NLS-1$
		
		private ColumnType type;
		private String key;
		
		private FixedColumns(ColumnType type, String key){
			this.type = type;
			this.key = key;	
		}
		
		public String getKey(){
			return this.key;
		}
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(ISurveyQueryLabelProvider.class).getLabel(this, l);
		}
	}
	
	
	private FixedColumns column;
	private Locale l;
	/**
	 * Creates a new fixed table column.
	 * 
	 * @param column the column definition
	 */
	public SurveyQueryColumn(FixedColumns column, Locale l) {
		super(column.getGuiName(l), column.key, column.type);
		this.column = column;
		this.l = l;
	}

	@Override
	public String getTooltip(){
		if (column == FixedColumns.WAYPOINT_X || column == FixedColumns.WAYPOINT_Y){
			return getProjectionTooltip();
		}
		return null;
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
				case MISSION_LEADER: return item.getMissionLeader();
				case SAMPLING_UNIT: return item.getSamplingUnitId();
				case WAYPOINT_ID: return item.getWaypointId();
				case WAYPOINT_DATE: return item.getWaypointDateTime();
				case WAYPOINT_TIME: return item.getWaypointDateTime(); 
				case WAYPOINT_X: return item.getWaypointX(getProjection()); 
				case WAYPOINT_Y: return item.getWaypointY(getProjection()); 
				case WAYPOINT_DIRECTION: return item.getWaypointDirection(); 
				case WAYPOINT_DISTANCE: return item.getWaypointDistance(); 
				case WAYPOINT_COMMENT: return item.getWaypointComment(); 
				case WAYPOINT_OBSERVER: return item.getWaypointObserver();
				default: return null;
			}
		}else if (queryResultItem instanceof MissionTrackResultItem){
			MissionTrackResultItem item = (MissionTrackResultItem)queryResultItem;
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
				case MISSION_TRACKDATE: return item.getTrackDate();
				case MISSION_TRACKTYPE: return item.getTrackType().getGuiName(l);
				case MISSION_TRACKID: return item.getTrackId();
				case SAMPLING_UNIT: return item.getSamplingUnitId();
				case MISSION_TRACKLENGTH: return item.getTrackLength();
				default: return null;
			}
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.patrol.query.model.observation.QueryColumn#clone()
	 */
	@Override
	public QueryColumn clone() {
		SurveyQueryColumn newColumn = new SurveyQueryColumn(this.column, l);
		return newColumn;
	}

	public static String getDbColumnName(String key) {
		key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
		for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
			key = key.replace(data[0], data[1]);
		}
		return key;
	}
	
}
