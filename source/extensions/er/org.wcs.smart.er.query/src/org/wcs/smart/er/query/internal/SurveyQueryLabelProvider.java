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
package org.wcs.smart.er.query.internal;

import java.util.Locale;

import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.MissionTrackDateField;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.summary.MissionValueItem.ValueItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;

/**
 * Survey label provider implementation.
 * 
 * @author Emily
 *
 */
public class SurveyQueryLabelProvider implements ISurveyQueryLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		// 
		if (item == SurveyQueryColumn.FixedColumns.CA_ID){ return Messages.SurveyQueryColumn_CaIdLabel;}
		if (item == SurveyQueryColumn.FixedColumns.CA_NAME){ return Messages.SurveyQueryColumn_CaNameLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN){ return Messages.SurveyQueryColumn_SurveyDesignLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START){ return Messages.SurveyQueryColumn_SurveyDesignStartdateLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END){ return Messages.SurveyQueryColumn_SurveyDesignEnddateLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY){ return Messages.SurveyQueryColumn_SurveyIdLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_START){ return Messages.SurveyQueryColumn_SurveyStartLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_END){ return Messages.SurveyQueryColumn_SurveyEndLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION){ return Messages.SurveyQueryColumn_MissionIdLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_START){ return Messages.SurveyQueryColumn_MissionStartLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_END){ return Messages.SurveyQueryColumn_MissionEndLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_LEADER){ return Messages.SurveyQueryColumn_MissionLeaderLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE){ return Messages.SurveyQueryColumn_TrackTypeLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE){ return Messages.SurveyQueryColumn_TrackDateLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKID){ return Messages.SurveyQueryColumn_TrackIdLabel;}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH){ return Messages.SurveyQueryColumn_TrackLengthLabel;}
		if (item == SurveyQueryColumn.FixedColumns.SAMPLING_UNIT){ return Messages.SurveyQueryColumn_SuLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_ID){ return Messages.SurveyQueryColumn_WaypointIdLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DATE){ return Messages.SurveyQueryColumn_WpDateLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_TIME){ return Messages.SurveyQueryColumn_WaypointTypeLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_X){ return Messages.SurveyQueryColumn_XLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_Y){ return Messages.SurveyQueryColumn_YLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION){ return Messages.SurveyQueryColumn_DirectionLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE){ return Messages.SurveyQueryColumn_DistanceLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT){ return Messages.SurveyQueryColumn_CommentLabel;}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER){ return Messages.SurveyQueryColumn_ObserverLabel;}
				
		if (item == ValueItem.TRACK_LENGTH){ return Messages.MissionLegnthValueDropItem_TrackLengthLabel;}
		if (item == ValueItem.MISSION_COUNT){ return Messages.MissionValueItem_NumberOfMissionsLabel;}
		if (item == ValueItem.SURVEY_COUNT){ return Messages.MissionValueItem_NumberOfSurveyLabel;}
		if (item == ValueItem.DAY_COUNT){ return Messages.MissionValueItem_NumberOfDays;}
		if (item == ValueItem.HOUR_COUNT){ return Messages.MissionValueItem_NumberOfHours;}
		if (item == ValueItem.MANHOURS_COUNT){ return Messages.MissionValueItem_NumberOfPersonHours;}
		if (item == ValueItem.TRACK_LENGTH_TOTAL){ return Messages.MissionValueItem_TotalTrackLength;}
		if (item == ValueItem.MISSION_COUNT_TOTAL){ return Messages.MissionValueItem_TotalNumMissions;}
		if (item == ValueItem.SURVEY_COUNT_TOTAL){ return Messages.MissionValueItem_TotalNumSurveys;}
		
		if (item == SamplingUnitFilter.Source.OBSERVATION) return Messages.SamplingUnitFilter_ObservationSuFilterLabel;
		if (item == SamplingUnitFilter.Source.TRACK) return Messages.SamplingUnitFilter_TrackSuFilterLabel;
		
		if (item instanceof MissionEndDateField) return Messages.MissionEndDateField_Name;
		if (item instanceof MissionStartDateField) return Messages.MissionStartDateField_Name;
		if (item instanceof MissionTrackDateField) return Messages.MissionTrackDateField_Name;
		return null;
	}

}
