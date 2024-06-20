/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol;

import java.util.Locale;

import org.wcs.smart.qa.patrol.internal.Messages;

/**
 * Image provider for patrol data providers
 * 
 * @author Emily
 *
 */
public class PatrolLabelProvider extends ILabelProvider {
	
	@Override
	public String getString(Key key, Locale l){
		switch(key){
		case LoadingString:
			return Messages.PatrolLabelProvider_LoadingDataMsg;
		case PatrolSpeedRoutineType_Desc:
			return Messages.PatrolLabelProvider_MaxSpeedDescription;
		case PatrolSpeedRoutineType_InvalidMaxSpeed:
			return Messages.PatrolLabelProvider_InvalidMaxSpeed;
		case PatrolSpeedRoutineType_Name:
			return Messages.PatrolLabelProvider_MaxSpeedName;
		case PatrolSpeedRoutineType_Param_MaxSpeedName:
			return Messages.PatrolLabelProvider_MaxSpeedLbl;
		case PatrolSpeedRoutineType_Param_SpeedUnits:
			return Messages.PatrolLabelProvider_SpeedUnits;
		case PatrolSpeedRoutineType_Param_TypeName:
			return Messages.PatrolLabelProvider_PatrolTypes;
		case PatrolSpeedRoutineType_TrackSpeedExceeded:
			return Messages.PatrolLabelProvider_TrackSpeedExceeded;
		case PatrolSpeedRoutineType_WpSpeedExceeded:
			return Messages.PatrolLabelProvider_WpSpeedExceeded;
		case PatrolTrackDataProvider_Name:
			return Messages.PatrolLabelProvider_PatrolProviderName;
		case PatrolTrackDataProvider_TrackNotFound:
			return Messages.PatrolLabelProvider_TrackNotFound;
		case PatrolTrackDataProvider_LegLabel:
			return Messages.PatrolLabelProvider_LegLabel;
		case PatrolWaypointDataProvider_WpNotFound:
			return Messages.PatrolLabelProvider_WaypointNotFound;
		case PatrolWaypointDataProvider_Name:
			return Messages.PatrolLabelProvider_WaypointDataProviderName;
		case PatrolWaypointDataProvider_WpIdLabel:
			return Messages.PatrolLabelProvider_WaypointIdLbl;
		case PatrolSpeedRoutineType_TrackError:
			return Messages.PatrolLabelProvider_ErrorValidatingPatrolTrack;
		case EmptyEndPatrolDaysType_Name: 
			return Messages.PatrolLabelProvider_EmptyEndDaysRoutineName;
		case EmptyEndPatrolDaysType_Desc: 
			return Messages.PatrolLabelProvider_EmptyEndDaysRoutineDesc;
		case PatrolDataProvider_Name:
			return Messages.PatrolLabelProvider_PatrolDataProviderName;
		case PatrolDataProvider_PatrolNotFound:
			return Messages.PatrolLabelProvider_PatrolNotFoundMsg;
		case EmptyEndPatrolDaysType_EmptyDays:
			return Messages.PatrolLabelProvider_EmpytDaysMsg;
		}
		return ""; //$NON-NLS-1$
	}
}
