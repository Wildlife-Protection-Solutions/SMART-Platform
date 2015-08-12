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
package org.wcs.smart.shared.labels;

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
		if (item == SurveyQueryColumn.FixedColumns.CA_ID){ return "Conservation Area ID";}
		if (item == SurveyQueryColumn.FixedColumns.CA_NAME){ return "Conservation Area Name";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN){ return "Survey Design";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_START){ return "Survey Design Start Date";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_DESIGN_END){ return "Survey Design End Date";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY){ return "Survey ID";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_START){ return "Survey Start Date";}
		if (item == SurveyQueryColumn.FixedColumns.SURVEY_END){ return "Survey End Date";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION){ return "Mission ID";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_START){ return "Mission Start Date";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_END){ return "Mission End Date";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_LEADER){ return "Mission Leader";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKTYPE){ return "Track Type";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKDATE){ return "Track Date";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKID){ return "Track ID";}
		if (item == SurveyQueryColumn.FixedColumns.MISSION_TRACKLENGTH){ return "Track Distance (km)";}
		if (item == SurveyQueryColumn.FixedColumns.SAMPLING_UNIT){ return "Sampling Unit";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_ID){ return "Waypoint ID";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DATE){ return "Waypoint Date";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_TIME){ return "Waypoint Time";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_X){ return "X";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_Y){ return "Y";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DIRECTION){ return "Direction";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_DISTANCE){ return "Distance";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_COMMENT){ return "Comment";}
		if (item == SurveyQueryColumn.FixedColumns.WAYPOINT_OBSERVER){ return "Observer";}
				
		if (item == ValueItem.TRACK_LENGTH){ return "Mission Track Distance (km)";}
		if (item == ValueItem.MISSION_COUNT){ return "Number of Missions";}
		if (item == ValueItem.SURVEY_COUNT){ return "Number of Surveys";}
		if (item == ValueItem.DAY_COUNT){ return "Total Mission Days";}
		if (item == ValueItem.HOUR_COUNT){ return "Total Mission Hours";}
		if (item == ValueItem.MANHOURS_COUNT){ return "Total Mission Person Hours";}
		if (item == ValueItem.TRACK_LENGTH_TOTAL){ return "Total Mission Track Distance (km)";}
		if (item == ValueItem.MISSION_COUNT_TOTAL){ return "Total Number of Missions";}
		if (item == ValueItem.SURVEY_COUNT_TOTAL){ return "Total Number of Surveys";}
		
		if (item == SamplingUnitFilter.Source.OBSERVATION) return "OBSERVATION";
		if (item == SamplingUnitFilter.Source.TRACK) return "TRACK";
		
		if (item instanceof MissionEndDateField) return "Mission End Date";
		if (item instanceof MissionStartDateField) return "Mission Start Date";
		if (item instanceof MissionTrackDateField) return "Mission Track Date";
		return null;
	}

}
