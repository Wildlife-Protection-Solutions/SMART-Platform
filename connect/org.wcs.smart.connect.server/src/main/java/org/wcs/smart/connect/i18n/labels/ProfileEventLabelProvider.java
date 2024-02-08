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
import org.wcs.smart.event.i2.IProfileEventLabelProvider;

public class ProfileEventLabelProvider implements IProfileEventLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
				
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg1) return Messages.getString("ProfileEventLabelProvider_RecordCreateMessage1", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg2) return Messages.getString("ProfileEventLabelProvider_RecordCreateMessage2", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg3) return Messages.getString("ProfileEventLabelProvider_RecordCreateMessage3", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg4) return Messages.getString("ProfileEventLabelProvider_RecordCreateMessage4", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg5) return Messages.getString("ProfileEventLabelProvider_RecordCreateMessage5", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_AttributeNotFound) return Messages.getString("ProfileEventLabelProvider_KeyNotFound", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_InvalidProfile) return Messages.getString("ProfileEventLabelProvider_InvalidProfile", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileNotFound) return Messages.getString("ProfileEventLabelProvider_ProfileNotFound", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateEntityActionType_ProfileParameterNotSet) return Messages.getString("ProfileEventLabelProvider_ProfileNotFound2", l);  //$NON-NLS-1$
		
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_InvalidProfile) return Messages.getString("ProfileEventLabelProvider_RecordInvalidProfile", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileNotFound) return Messages.getString("ProfileEventLabelProvider_RecordProfileNotFound", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_ProfileParameterNotSet) return Messages.getString("ProfileEventLabelProvider_RecordPRofileNotFound2", l);  //$NON-NLS-1$
		if (item == IProfileEventLabelProvider.EventMessages.CreateRecordActionType_WaypointIdLabel) return Messages.getString("ProfileEventLabelProvider_WpIdLabel", l);  //$NON-NLS-1$
		
		return null;
	}

}
