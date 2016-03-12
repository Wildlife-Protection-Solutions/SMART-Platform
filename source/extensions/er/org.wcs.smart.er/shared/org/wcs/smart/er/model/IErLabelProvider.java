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
package org.wcs.smart.er.model;

import org.wcs.smart.ISharedLabelProvider;

/**
 * Label provider for survey items.  Must provide values for
 * SamplingUnit.GeometryType items, SamplingUnit.State items,
 * SurveyDesign.State items, and MissionTrack.TrackType items.
 * 
 * @author Emily
 *
 */
public interface IErLabelProvider extends ISharedLabelProvider{
	public static final String ID_COLUMN_KEY = "idcolumnlabelkey"; //$NON-NLS-1$
	public static final String LENGTH_COLUMN_KEY = "lengthcolumnlabelkey"; //$NON-NLS-1$
	public static final String STATE_COLUMN_KEY = "statecolumnlabelkey"; //$NON-NLS-1$
	
	public static final String SU_TABLE_LONGNAME_KEY = "sutablelongnamekey"; //$NON-NLS-1$
	public static final String SD_TABLE_LONGNAME_KEY = "surveydesigntablelongnamekey"; //$NON-NLS-1$
	
	public static final String SD_DESCRIPTION_COL_KEY = "sddescriptioncolumnkey";
	public static final String SD_ENDDATE_COL_KEY = "sdenddatecolumnkey";
	public static final String SD_STARTDATE_COL_KEY = "sdstartdatecolumnkey";
	public static final String SD_STATUS_COL_KEY = "sdstatuecolumnkey";
	public static final String SD_KEY_COL_KEY = "sdkeycolumnkey";
	public static final String SD_NAME_COL_KEY = "sdnamecolumnkey";
	
	
}
