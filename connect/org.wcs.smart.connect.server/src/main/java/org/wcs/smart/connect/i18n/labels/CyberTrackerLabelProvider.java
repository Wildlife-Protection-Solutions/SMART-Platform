/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
import org.wcs.smart.cybertracker.ICyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.SmartMobileDeviceManager;
import org.wcs.smart.cybertracker.json.JsonError;
import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class CyberTrackerLabelProvider implements ICyberTrackerLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == JsonError.Type.JSON_PARSE_ERROR) return Messages.getString("CyberTrackerLabelProvider_ParseError1", l);  //$NON-NLS-1$
		if (item == JsonError.Type.FEATURES_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseError2", l);  //$NON-NLS-1$
		if (item == JsonError.Type.INVALID_OBS_COUNTER) return Messages.getString("CyberTrackerLabelProvider_ParseError3", l);  //$NON-NLS-1$
		if (item == JsonError.Type.FEATURE_OBJECT_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseError4", l);  //$NON-NLS-1$
		if (item == JsonError.Type.LAT_LONG_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseError5", l);  //$NON-NLS-1$
		
		if (item ==  JsonImportWarning.Type.CATEGORY_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseWarn1", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.LIST_ATTRIBUTE_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseWarn2", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.TREE_NODE_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseWarn3", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.ATTRIBUTE_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseWarn4", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.ATT_CAT_NOT_ASSOCIATED) return Messages.getString("CyberTrackerLabelProvider_ParseWarn5", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider_ParseWarn6", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_CM) return Messages.getString("CyberTrackerLabelProvider_ParseWarn7", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_SIGNATURE) return Messages.getString("CyberTrackerLabelProvider_ParseWarn8", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_OBSERVER) return Messages.getString("CyberTrackerLabelProvider_ParseWarn9", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_ATTACHMENT) return Messages.getString("CyberTrackerLabelProvider_ParseWarn10", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_PHOTO_ATTACHMENT) return Messages.getString("CyberTrackerLabelProvider_ParseWarn11", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_BOOLEAN) return Messages.getString("CyberTrackerLabelProvider_ParseWarn12", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_DATE) return Messages.getString("CyberTrackerLabelProvider_ParseWarn13", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_TIME) return Messages.getString("CyberTrackerLabelProvider.TimeParseError", l); //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.DUPLICATE_ATTRIBUTES) return Messages.getString("CyberTrackerLabelProvider_ParseWarn14", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.OBS_ATTRIBUTE_PARSE_ERROR) return Messages.getString("CyberTrackerLabelProvider_ParseWarn15", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_PARSE_ERROR) return Messages.getString("CyberTrackerLabelProvider_ParseWarn16", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR) return Messages.getString("CyberTrackerLabelProvider_ParseWarn17", l);  //$NON-NLS-1$

		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_GEOMETRY) return Messages.getString("CyberTrackerLabelProvider_ParseWarn18", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_LINESTRING_GEOMETRY) return Messages.getString("CyberTrackerLabelProvider_ParseWarn19", l);  //$NON-NLS-1$
		if (item ==  JsonImportWarning.Type.INVALID_POLYGON_GEOMETRY) return Messages.getString("CyberTrackerLabelProvider_ParseWarn20", l);  //$NON-NLS-1$
		if (item == JsonImportWarning.Type.TAG_NOT_FOUND) return Messages.getString("CyberTrackerLabelProvider.TagNotFound", l); //$NON-NLS-1$
		
		if (item == SmartMobileDeviceManager.DEFAULT_NAME) return Messages.getString("CyberTrackerLabelProvider.DefaultDeviceIdName", l); //$NON-NLS-1$

		return null;
	}

}
