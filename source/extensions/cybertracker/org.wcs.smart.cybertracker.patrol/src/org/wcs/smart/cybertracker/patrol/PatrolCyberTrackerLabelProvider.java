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
package org.wcs.smart.cybertracker.patrol;

import java.util.Locale;

import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonImportWarning;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonProcessor;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.patrol.query.MobileDeviceIdPatrolQueryOption;


/**
 * @since 8.0
 */
public class PatrolCyberTrackerLabelProvider implements IPatrolCyberTrackerLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == PatrolJsonImportWarning.WarningType.STATION_NOT_FOUND) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning1;
		if (item == PatrolJsonImportWarning.WarningType.TEAM_NOT_FOUND) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning2;
		if (item == PatrolJsonImportWarning.WarningType.MANDATE_NOT_FOUND) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning3;
		if (item == PatrolJsonImportWarning.WarningType.MEMBER_NOT_FOUND) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning4;
		if (item == PatrolJsonImportWarning.WarningType.TT_NOT_FOUND_ERROR) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning52;
		if (item == PatrolJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning62;
		if (item == PatrolJsonImportWarning.WarningType.PATROL_NOT_FOUND) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning72;
		if (item == PatrolJsonImportWarning.WarningType.DUPLICATE) return Messages.PatrolCyberTrackerLabelProvider_JsonProcessingWarning82;
		
		
		if (item == PatrolJsonProcessor.StatusMessage.ADDED) return Messages.PatrolCyberTrackerLabelProvider_CreatedMessage2;
		if (item == PatrolJsonProcessor.StatusMessage.MODIFIED) return Messages.PatrolCyberTrackerLabelProvider_ModifiedMessage2;
		
		if (item == PatrolJsonProcessor.CA_ERROR) return Messages.PatrolCyberTrackerLabelProvider_CaErrorMessage2;
		
		if (item == MobileDeviceIdPatrolQueryOption.KEY) return Messages.PatrolCyberTrackerLabelProvider_DeviceIdQueryOption;
		return null;
	}

}
