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

import org.wcs.smart.SmartContext;

/**
 * Image provider to implement & set in SmartContext to add icons to patrol
 * data providers.
 * 
 * @author Emily
 *
 */
public abstract class ILabelProvider {

	public static enum Key{
		PatrolSpeedRoutineType_Name,
		PatrolSpeedRoutineType_Desc,
		PatrolSpeedRoutineType_Param_MaxSpeedName,
		PatrolSpeedRoutineType_Param_TypeName,
		PatrolSpeedRoutineType_Param_SpeedUnits,
		PatrolSpeedRoutineType_InvalidMaxSpeed,
		PatrolSpeedRoutineType_WpSpeedExceeded,
		PatrolSpeedRoutineType_TrackSpeedExceeded,
		PatrolSpeedRoutineType_TrackError,
		PatrolTrackDataProvider_TrackNotFound,
		PatrolTrackDataProvider_Name,
		PatrolTrackDataProvider_LegLabel,
		PatrolWaypointDataProvider_WpNotFound,
		PatrolWaypointDataProvider_Name,
		PatrolDataProvider_Name,
		PatrolDataProvider_PatrolNotFound,
		PatrolWaypointDataProvider_WpIdLabel,
		LoadingString,
		EmptyEndPatrolDaysType_Name,
		EmptyEndPatrolDaysType_Desc,
		EmptyEndPatrolDaysType_EmptyDays
		
	}
	
	public abstract String getString(Key key, Locale l);
	
	public final static String getLabel(Key key, Locale l){
		return SmartContext.INSTANCE.getClass(ILabelProvider.class).getString(key, l);
	}
}
