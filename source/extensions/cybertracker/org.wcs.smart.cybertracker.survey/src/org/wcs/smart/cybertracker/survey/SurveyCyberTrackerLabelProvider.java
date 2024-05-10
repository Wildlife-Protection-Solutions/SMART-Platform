/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey;

import java.util.Locale;

import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.json.MissionJsonImportWarning;
import org.wcs.smart.cybertracker.survey.json.MissionJsonProcessor;
import org.wcs.smart.cybertracker.survey.json.MissionJsonTrackProcessor;
import org.wcs.smart.cybertracker.survey.model.ISurveyCyberTrackerLabelProvider;

/**
 * @since 8.0
 */
public class SurveyCyberTrackerLabelProvider implements ISurveyCyberTrackerLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == MissionJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning1;
		if (item == MissionJsonImportWarning.WarningType.SU_NOT_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning2;
		if (item == MissionJsonImportWarning.WarningType.REST_TIME_ERROR) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning3;
		if (item == MissionJsonImportWarning.WarningType.MISSION_NOT_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning4;
		if (item == MissionJsonImportWarning.WarningType.SURVEY_DESIGN_NOTFOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning5;
		if (item == MissionJsonImportWarning.WarningType.MISSION_ATTRIBUTE_NOT_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning6;
		if (item == MissionJsonImportWarning.WarningType.MULTIPLE_ATTRIBUTES_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning7;
		if (item == MissionJsonImportWarning.WarningType.LIST_ITEM_NOT_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning8;
		if (item == MissionJsonImportWarning.WarningType.MEMBER_NOT_FOUND) return Messages.SurveyCyberTrackerLabelProvider_JsonProcessingWarning9;
			
		if (item == MissionJsonProcessor.StatusMessage.ADDED) return Messages.SurveyCyberTrackerLabelProvider_CreatedMessage;
		if (item == MissionJsonProcessor.StatusMessage.MODIFIED) return Messages.SurveyCyberTrackerLabelProvider_ModifiedMessage;
		
		if (item == MissionJsonTrackProcessor.TRACK_LBL) return Messages.SurveyCyberTrackerLabelProvider_TrackId;
		
		if (item == MissionJsonProcessor.CA_ERROR) return Messages.SurveyCyberTrackerLabelProvider_InvalidCa;

		return null;
	}

}
