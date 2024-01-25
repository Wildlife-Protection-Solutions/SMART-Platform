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

		if (item.getClass() == SmartCollectWaypointSource.class) return "SMART Collect Incident"; 

		if (item == SmartCollectJsonImportWarning.WarningType.NO_USER) return "No user specified for SMARTCollect feature.  Feature will not be loaded: {0}";
		if (item == SmartCollectJsonImportWarning.WarningType.MISSING_DEVICE_ID) return "No device id specified for SMARTCollect feature.  Feature will not be loaded: {0}";
		if (item == SmartCollectJsonImportWarning.WarningType.FEATURE_DISCARDED) return "{0} features discarded.";
		if (item == SmartCollectJsonImportWarning.WarningType.USER_BLACKLISTED_FEATURE_DISCARDED) return "The user {0} is blacklisted.  The {1} features reported by this user were not loaded.";
		
		if (item == SmartCollectJsonProcessor.FINISH_MESSAGE) return "Created {0} SMARTCollect Incidents";
		
		return null;
	}

}
