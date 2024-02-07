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
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonImportWarning;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonProcessor;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;


/**
 * @since 8.0
 */
public class PatrolCyberTrackerLabelProvider implements IPatrolCyberTrackerLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == PatrolJsonImportWarning.WarningType.STATION_NOT_FOUND) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning1", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.TEAM_NOT_FOUND) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning2", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.MANDATE_NOT_FOUND) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning3", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.MEMBER_NOT_FOUND) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning4", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.TT_NOT_FOUND_ERROR) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning5", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning6", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.PATROL_NOT_FOUND) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning7", l);  //$NON-NLS-1$
		if (item == PatrolJsonImportWarning.WarningType.DUPLICATE) return Messages.getString("PatrolCyberTrackerLabelProvider_Warning8", l);  //$NON-NLS-1$
		
		
		if (item == PatrolJsonProcessor.StatusMessage.ADDED) return Messages.getString("PatrolCyberTrackerLabelProvider_CreatedMessage", l);  //$NON-NLS-1$
		if (item == PatrolJsonProcessor.StatusMessage.MODIFIED) return Messages.getString("PatrolCyberTrackerLabelProvider_ModifiedMessage", l);  //$NON-NLS-1$
		
		if (item == PatrolJsonProcessor.CA_ERROR) return Messages.getString("PatrolCyberTrackerLabelProvider_CaNotFound", l);  //$NON-NLS-1$
		
		return null;
	}

}
