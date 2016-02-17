/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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
		if (item == SurveyQueryColumn.FixedColumns.CA_ID){ return Messages.getString("SurveyQueryLabelProvider.CaId", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.CA_NAME){ return Messages.getString("SurveyQueryLabelProvider.CaNameLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN){ return Messages.getString("SurveyQueryLabelProvider.SdLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START){ return Messages.getString("SurveyQueryLabelProvider.SdStartDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END){ return Messages.getString("SurveyQueryLabelProvider.SdEndDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY){ return Messages.getString("SurveyQueryLabelProvider.SurveyIdLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_START){ return Messages.getString("SurveyQueryLabelProvider.SurveyStartDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_END){ return Messages.getString("SurveyQueryLabelProvider.SurveyEndDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION){ return Messages.getString("SurveyQueryLabelProvider.MissionIdLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_START){ return Messages.getString("SurveyQueryLabelProvider.MissionStartDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_END){ return Messages.getString("SurveyQueryLabelProvider.MissionEndDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_LEADER){ return Messages.getString("SurveyQueryLabelProvider.LeaderLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE){ return Messages.getString("SurveyQueryLabelProvider.TrackTypeLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE){ return Messages.getString("SurveyQueryLabelProvider.TrackDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKID){ return Messages.getString("SurveyQueryLabelProvider.TrackIdLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH){ return Messages.getString("SurveyQueryLabelProvider.TrackDistanceLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.SAMPLING_UNIT){ return Messages.getString("SurveyQueryLabelProvider.SuLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_ID){ return Messages.getString("SurveyQueryLabelProvider.WpIdLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DATE){ return Messages.getString("SurveyQueryLabelProvider.WpDateLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_TIME){ return Messages.getString("SurveyQueryLabelProvider.WpTimeLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_X){ return Messages.getString("SurveyQueryLabelProvider.WpXLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_Y){ return Messages.getString("SurveyQueryLabelProvider.WpYLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION){ return Messages.getString("SurveyQueryLabelProvider.DirLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE){ return Messages.getString("SurveyQueryLabelProvider.DistanceLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT){ return Messages.getString("SurveyQueryLabelProvider.CommentLabel", l);} //$NON-NLS-1$
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER){ return Messages.getString("SurveyQueryLabelProvider.ObserverLabel", l);} //$NON-NLS-1$
				
		if (item == ValueItem.TRACK_LENGTH){ return Messages.getString("SurveyQueryLabelProvider.TrackDistanctOp", l);} //$NON-NLS-1$
		if (item == ValueItem.MISSION_COUNT){ return Messages.getString("SurveyQueryLabelProvider.NumMissionsOp", l);} //$NON-NLS-1$
		if (item == ValueItem.SURVEY_COUNT){ return Messages.getString("SurveyQueryLabelProvider.NumSurveysOp", l);} //$NON-NLS-1$
		if (item == ValueItem.DAY_COUNT){ return Messages.getString("SurveyQueryLabelProvider.TotalDaysOp", l);} //$NON-NLS-1$
		if (item == ValueItem.HOUR_COUNT){ return Messages.getString("SurveyQueryLabelProvider.TotalHoursOp", l);} //$NON-NLS-1$
		if (item == ValueItem.MANHOURS_COUNT){ return Messages.getString("SurveyQueryLabelProvider.TotalPersonHoursOp", l);} //$NON-NLS-1$
		if (item == ValueItem.TRACK_LENGTH_TOTAL){ return Messages.getString("SurveyQueryLabelProvider.TotalMissionTrackDistanceOp", l);} //$NON-NLS-1$
		if (item == ValueItem.MISSION_COUNT_TOTAL){ return Messages.getString("SurveyQueryLabelProvider.TotalNumMissionOp", l);} //$NON-NLS-1$
		if (item == ValueItem.SURVEY_COUNT_TOTAL){ return Messages.getString("SurveyQueryLabelProvider.TotalNumSurveyOp", l);} //$NON-NLS-1$
		
		if (item == SamplingUnitFilter.Source.OBSERVATION) return Messages.getString("SurveyQueryLabelProvider.ObservationSuFilterSource", l); //$NON-NLS-1$
		if (item == SamplingUnitFilter.Source.TRACK) return Messages.getString("SurveyQueryLabelProvider.TrackSuFilterSource", l); //$NON-NLS-1$
		
		if (item instanceof MissionEndDateField) return Messages.getString("SurveyQueryLabelProvider.MissionEndDateFilterField", l); //$NON-NLS-1$
		if (item instanceof MissionStartDateField) return Messages.getString("SurveyQueryLabelProvider.MissionStartDateFilterField", l); //$NON-NLS-1$
		if (item instanceof MissionTrackDateField) return Messages.getString("SurveyQueryLabelProvider.MissionTrackDateDateFilterField", l); //$NON-NLS-1$
		return null;
	}

}
