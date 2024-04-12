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
package org.wcs.smart.cybertracker;

import java.util.Locale;

import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.json.JsonError;
import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class CyberTrackerLabelProvider implements ICyberTrackerLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == JsonError.Type.JSON_PARSE_ERROR) return Messages.CyberTrackerLabelProvider_JsonParseError1;
		if (item == JsonError.Type.FEATURES_NOT_FOUND) return Messages.CyberTrackerLabelProvider_JsonParseError2;
		if (item == JsonError.Type.INVALID_OBS_COUNTER) return Messages.CyberTrackerLabelProvider_JsonParseError3;
		if (item == JsonError.Type.FEATURE_OBJECT_NOT_FOUND) return Messages.CyberTrackerLabelProvider_JsonParseError4;
		if (item == JsonError.Type.LAT_LONG_NOT_FOUND) return Messages.CyberTrackerLabelProvider_JsonParseError5;
		
		if (item ==  JsonImportWarning.Type.CATEGORY_NOT_FOUND) return Messages.CyberTrackerLabelProvider_ObservationParseError1;
		if (item ==  JsonImportWarning.Type.LIST_ATTRIBUTE_NOT_FOUND) return Messages.CyberTrackerLabelProvider_ObservationParseError2;
		if (item ==  JsonImportWarning.Type.TREE_NODE_NOT_FOUND) return Messages.CyberTrackerLabelProvider_ObservationParseError3;
		if (item ==  JsonImportWarning.Type.ATTRIBUTE_NOT_FOUND) return Messages.CyberTrackerLabelProvider_ObservationParseError4;
		if (item ==  JsonImportWarning.Type.ATT_CAT_NOT_ASSOCIATED) return Messages.CyberTrackerLabelProvider_ObservationParseError5;
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_NOT_FOUND) return Messages.CyberTrackerLabelProvider_ObservationParseError6;
		if (item ==  JsonImportWarning.Type.INVALID_CM) return Messages.CyberTrackerLabelProvider_ObservationParseError7;
		if (item ==  JsonImportWarning.Type.INVALID_SIGNATURE) return Messages.CyberTrackerLabelProvider_ObservationParseError8;
		if (item ==  JsonImportWarning.Type.TAG_NOT_FOUND) return Messages.CyberTrackerLabelProvider_AttachmentTagNotFound; 
		if (item ==  JsonImportWarning.Type.INVALID_OBSERVER) return Messages.CyberTrackerLabelProvider_ObservationParseError9;
		if (item ==  JsonImportWarning.Type.INVALID_ATTACHMENT) return Messages.CyberTrackerLabelProvider_ObservationParseError10;
		if (item ==  JsonImportWarning.Type.INVALID_PHOTO_ATTACHMENT) return Messages.CyberTrackerLabelProvider_ObservationParseError11;
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_BOOLEAN) return Messages.CyberTrackerLabelProvider_ObservationParseError12;
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_DATE) return Messages.CyberTrackerLabelProvider_ObservationParseError13;
		if (item ==  JsonImportWarning.Type.DUPLICATE_ATTRIBUTES) return Messages.CyberTrackerLabelProvider_ObservationParseError14;
		if (item ==  JsonImportWarning.Type.OBS_ATTRIBUTE_PARSE_ERROR) return Messages.CyberTrackerLabelProvider_ObservationParseError15;
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_PARSE_ERROR) return Messages.CyberTrackerLabelProvider_ObservationParseError16;
		if (item ==  JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR) return Messages.CyberTrackerLabelProvider_ObservationParseError17;

		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_GEOMETRY) return Messages.CyberTrackerLabelProvider_ObservationParseError18;
		if (item ==  JsonImportWarning.Type.INVALID_LINESTRING_GEOMETRY) return Messages.CyberTrackerLabelProvider_ObservationParseError19;
		if (item ==  JsonImportWarning.Type.INVALID_POLYGON_GEOMETRY) return Messages.CyberTrackerLabelProvider_ObservationParseError20;
	
		return null;
	}

}
