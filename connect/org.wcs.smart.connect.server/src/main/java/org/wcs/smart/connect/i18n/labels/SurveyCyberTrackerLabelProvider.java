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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
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
		if (item == MissionJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning1", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.SU_NOT_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning2", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.REST_TIME_ERROR) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning3", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.MISSION_NOT_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning4", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.SURVEY_DESIGN_NOTFOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning5", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.MISSION_ATTRIBUTE_NOT_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning6", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.MULTIPLE_ATTRIBUTES_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning7", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.LIST_ITEM_NOT_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning8", l); //$NON-NLS-1$
		if (item == MissionJsonImportWarning.WarningType.MEMBER_NOT_FOUND) return Messages.getString("SurveyCyberTrackerLabelProvider_Warning9", l); //$NON-NLS-1$
			
		if (item == MissionJsonProcessor.StatusMessage.ADDED) return Messages.getString("SurveyCyberTrackerLabelProvider_CreatedMessage", l); //$NON-NLS-1$
		if (item == MissionJsonProcessor.StatusMessage.MODIFIED) return Messages.getString("SurveyCyberTrackerLabelProvider_ModifiedMessage", l); //$NON-NLS-1$
		
		if (item == MissionJsonTrackProcessor.TRACK_LBL) return Messages.getString("SurveyCyberTrackerLabelProvider_TrackId", l); //$NON-NLS-1$
		return null;
	}

}
