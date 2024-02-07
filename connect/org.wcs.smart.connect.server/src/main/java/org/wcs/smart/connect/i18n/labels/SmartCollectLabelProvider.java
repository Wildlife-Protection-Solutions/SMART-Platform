/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollection.json.SmartCollectJsonImportWarning;
import org.wcs.smart.smartcollection.json.SmartCollectJsonProcessor;

/**
 * Label provider for SmartCollect incidents
 * @author Emily
 *
 */
public class SmartCollectLabelProvider implements ISmartCollectLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {

		if (item.getClass() == SmartCollectWaypointSource.class) return Messages.getString("SmartCollectLabelProvider_WaypointSourceLabel", l);  //$NON-NLS-1$

		if (item == SmartCollectJsonImportWarning.WarningType.NO_USER) return Messages.getString("SmartCollectLabelProvider_NoUser", l);  //$NON-NLS-1$
		if (item == SmartCollectJsonImportWarning.WarningType.MISSING_DEVICE_ID) return Messages.getString("SmartCollectLabelProvider_NoDevice", l);  //$NON-NLS-1$
		if (item == SmartCollectJsonImportWarning.WarningType.FEATURE_DISCARDED) return Messages.getString("SmartCollectLabelProvider_DiscaredFeatures", l);  //$NON-NLS-1$
		if (item == SmartCollectJsonImportWarning.WarningType.USER_BLACKLISTED_FEATURE_DISCARDED) return Messages.getString("SmartCollectLabelProvider_UserBlacklisted", l);  //$NON-NLS-1$
		
		if (item == SmartCollectJsonProcessor.FINISH_MESSAGE) return Messages.getString("SmartCollectLabelProvider_CreateFeatures", l);  //$NON-NLS-1$
		
		return null;
	}

}
