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
package org.wcs.smart.qa;

import java.util.Locale;

import org.wcs.smart.SmartContext;

/**
 * Image provider to implement & set in SmartContext to support labels for core
 * qa plugin.
 * 
 * @author Emily
 *
 */
public abstract class ILabelProvider {

	public static enum Key {
		ValidationEngine_TaskName,
		ValidationEngine_SubTaskName,
		QaErrorGeoResourceInfo_Name,
		QaErrorGeoResourceInfo_Description,
		QaErrorService_Name,
		QaErrorService_Description,
		IgnoreAction_Name,
		LocationRoutineType_Name,
		LocationRoutineType_Description,
		LocationRoutineType_FileParamDescription,
		LocationRoutineType_AreaParamDescription,
		LocationRoutineType_WktParamDescription,
		LocationRoutineType_Error,
		LocationRoutineType_LoadingDataMsg,
		LocationRoutineType_NoGeomFound,
		LocationRoutineType_ValidatingDataTaskName,
		LocationRoutineType_WpOutsideArea,
		LocationRoutineType_WpOutsideArea2,
		LocationRoutineType_PrjWpOutsideArea,
		LocationRoutineType_TrackOutsideArea,
		LocationRoutineType_TrackOutsideArea2,
		QaError_Status_New,
		QaError_Status_Ignored,
		QaError_Status_Deleted,
		QaError_Status_Error,
		QaError_Status_Fixed,
		QaError_Status_Unknown,
	}

	public abstract String getString(Key key, Locale l);
	
	public static final String getLabel(Key key, Locale l){
		return SmartContext.INSTANCE.getClass(ILabelProvider.class).getString(key, l);
	}
	
}