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
package org.wcs.smart.connect.qa;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.qa.patrol.ILabelProvider;

/**
 * Image provider for patrol data providers
 * 
 * @author Emily
 *
 */
public class QaPatrolLabelProvider extends ILabelProvider {

	@Override
	public String getString(Key key, Locale l){
		switch(key){
		case LoadingString:
			return Messages.getString("QaPatrolLabelProvider.LoadingString", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_Desc:
			return Messages.getString("QaPatrolLabelProvider.SpeedDescription", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_InvalidMaxSpeed:
			return Messages.getString("QaPatrolLabelProvider.SpeedInvalid", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_Name:
			return Messages.getString("QaPatrolLabelProvider.SpeedName", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_Param_MaxSpeedName:
			return Messages.getString("QaPatrolLabelProvider.SpeedMaxSpeedLbl", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_Param_SpeedUnits:
			return Messages.getString("QaPatrolLabelProvider.SpeedUnits", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_Param_TypeName:
			return Messages.getString("QaPatrolLabelProvider.SpeedPTypes", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_TrackSpeedExceeded:
			return Messages.getString("QaPatrolLabelProvider.SpeedTrackExceeded", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_WpSpeedExceeded:
			return Messages.getString("QaPatrolLabelProvider.SpeedWpExceeded", l); //$NON-NLS-1$
		case PatrolTrackDataProvider_Name:
			return Messages.getString("QaPatrolLabelProvider.TrackProviderName", l); //$NON-NLS-1$
		case PatrolTrackDataProvider_TrackNotFound:
			return Messages.getString("QaPatrolLabelProvider.TrackNotfound", l); //$NON-NLS-1$
		case PatrolTrackDataProvider_LegLabel:
			return Messages.getString("QaPatrolLabelProvider.LegLabel", l); //$NON-NLS-1$
		case PatrolWaypointDataProvider_WpNotFound:
			return Messages.getString("QaPatrolLabelProvider.WaypointNotFound", l); //$NON-NLS-1$
		case PatrolWaypointDataProvider_Name:
			return Messages.getString("QaPatrolLabelProvider.WaypointProviderName", l); //$NON-NLS-1$
		case PatrolWaypointDataProvider_WpIdLabel:
			return Messages.getString("QaPatrolLabelProvider.WpIdLabel", l); //$NON-NLS-1$
		case PatrolSpeedRoutineType_TrackError:
			return Messages.getString("QaPatrolLabelProvider.TrackValidationError", l); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
