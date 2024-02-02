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

import org.wcs.smart.cybertracker.ICyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.json.JsonError;
import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class CyberTrackerLabelProvider implements ICyberTrackerLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == JsonError.Type.JSON_PARSE_ERROR) return "Unable to parse JSON text: {0}.";
		if (item == JsonError.Type.FEATURES_NOT_FOUND) return "No JSON object with key ''{0}'' found";
		if (item == JsonError.Type.INVALID_OBS_COUNTER) return "Invalid value for observation counter field {0}.";
		if (item == JsonError.Type.FEATURE_OBJECT_NOT_FOUND) return "Feature object does not have type ''{0}''";
		if (item == JsonError.Type.LAT_LONG_NOT_FOUND) return "Longitude/Latitude values not found";
		
		if (item ==  JsonImportWarning.Type.CATEGORY_NOT_FOUND) return "Category not found. Observation data will not be imported for this waypoint. (category uuid: {0})";
		if (item ==  JsonImportWarning.Type.LIST_ATTRIBUTE_NOT_FOUND) return "Attribute list item not found. Attribute value will not be set for observation. (uuid: {0})";
		if (item ==  JsonImportWarning.Type.TREE_NODE_NOT_FOUND) return "Tree node not found. Attribute value will not be set for observation. (uuid: {0})";
		if (item ==  JsonImportWarning.Type.ATTRIBUTE_NOT_FOUND) return "Attribute not found. Attribute value will not be set for observation. (uuid: {0})";
		if (item ==  JsonImportWarning.Type.ATT_CAT_NOT_ASSOCIATED) return "Attribute ''{0}'' not associated with category ''{1}''. Attribute value will not be set for observation.";
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_NOT_FOUND) return "Attribute not found. The default setting for this value will be ignored for the observation. (uuid: {0})";
		if (item ==  JsonImportWarning.Type.INVALID_CM) return "Source configurable model not found. Source configurable model will be null for this waypoint. (mode uuid: {0}). Ensure your desktop Conservation Area is synchronized with Connect.";
		if (item ==  JsonImportWarning.Type.INVALID_SIGNATURE) return "Signature type not found. The file will be imported as a regular attachment. (signature key: {0}).";
		if (item ==  JsonImportWarning.Type.INVALID_OBSERVER) return "Observer not found found. The observer will be empty for this waypoint. (uuid: {0})";
		if (item ==  JsonImportWarning.Type.INVALID_ATTACHMENT) return "Could not process attachment. Attachment will not be imported. (attachment number: {0})";
		if (item ==  JsonImportWarning.Type.INVALID_PHOTO_ATTACHMENT) return "Could not determine photo attachment type. Attachment will be imported with an unknown file extension. (attachment number: {0})";
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_BOOLEAN) return "Cannot parse boolean value from ''{0}''. Attribute value will not be set.";
		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_DATE) return "Cannot parse date from ''{0}''. Date must be provided in either ''{1}'' or ''{2''} format. Attribute value will not be set.";
		if (item ==  JsonImportWarning.Type.DUPLICATE_ATTRIBUTES) return "The same attribute ({0}) cannot be specified twice for a single observation.";
		if (item ==  JsonImportWarning.Type.OBS_ATTRIBUTE_PARSE_ERROR) return "Could not parse value for attribute {0}: {1}: {2}.";
		if (item ==  JsonImportWarning.Type.DEFAULT_ATTRIBUTE_PARSE_ERROR) return "Could not parse value for default values for attribute {0}: {1}: {2}. Default values will be ignored for this observation.";
		if (item ==  JsonImportWarning.Type.JSON_FEATURE_PARSE_ERROR) return "Error parsing feature information (feature will not be processed): {0}";

		if (item ==  JsonImportWarning.Type.COULD_NOT_PARSE_GEOMETRY) return "Unable to parse geometry from attribute {0}. Attribute will not be imported.";
		if (item ==  JsonImportWarning.Type.INVALID_LINESTRING_GEOMETRY) return "Invalid geometry provided for line attribute {0}. Attribute will not be imported.";
		if (item ==  JsonImportWarning.Type.INVALID_POLYGON_GEOMETRY) return "Invalid geometry provided for polygon attribute {0}. Attribute will not be imported.";
	
		return null;
	}

}
