/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.json;

import java.text.MessageFormat;

/**
 * SMART Mobile processing warnings
 * 
 * @author Emily
 *
 */
public class JsonImportWarning {
	

	public enum Type{
		CATEGORY_NOT_FOUND, //1 parameter - category uuid as string
		LIST_ATTRIBUTE_NOT_FOUND, //1 parameter - list item uuid as string
		TREE_NODE_NOT_FOUND, //1 parameter - tree node uuid as string
		ATTRIBUTE_NOT_FOUND, //1 parameter - attribute uuid as string
		ATT_CAT_NOT_ASSOCIATED,//2 parameter - attribute name, category name
		INVALID_CM, // 1 parameter - cm uuid as string
		INVALID_SIGNATURE, //1 parameter - key
		INVALID_OBSERVER, //1 parameter - employee uuid as string
		INVALID_ATTACHMENT, //1 parameter - attachment ordinal
		INVALID_PHOTO_ATTACHMENT, //1 parameter - attachment ordinal
		
		COULD_NOT_PARSE_BOOLEAN, //1 parameter value being parsed
		COULD_NOT_PARSE_DATE,  //1 parameter value being parsed
		
		DUPLICATE_ATTRIBUTES, //1 parameter attribute name
		
		OBS_ATTRIBUTE_PARSE_ERROR, // 3 params (attribute name, value parsing, error info)
		DEFAULT_ATTRIBUTE_PARSE_ERROR, // 3 params (attribute name, value parsing, error info)
		
		DEFAULT_ATTRIBUTE_NOT_FOUND, //1 parameter - attribute uuid as string
		
		JSON_FEATURE_PARSE_ERROR; //1 parameter error message
	}
	
	private Type type;
	private String message;
	private Object[] data;
	
	public JsonImportWarning(Type type) {
		this(type, new Object[0]);
	}
	
	public JsonImportWarning(Type type, Object... data) {
		this.type = type;
		this.data = data;
	}
	
	public JsonImportWarning(String message, Object... data) {
		this.data = data;
		this.message = message;
	}
	
	
	public String getMessage() {
		if (type == null) { 
			if (data == null || data.length == 0) return message;
			return MessageFormat.format(message, data);
		}	
		switch(type) {
			case CATEGORY_NOT_FOUND: return MessageFormat.format("Category not found. Observation data will not be imported for this waypoint. (category uuid: {0})", data);
			case LIST_ATTRIBUTE_NOT_FOUND: return MessageFormat.format("Attribute list item not found. Attribute value will not be set for observation. (uuid: {0})", data);
			case TREE_NODE_NOT_FOUND: return MessageFormat.format("Tree node not found. Attribute value will not be set for observation. (uuid: {0})", data);
			case ATTRIBUTE_NOT_FOUND: return MessageFormat.format("Attribute not found. Attribute value will not be set for observation. (uuid: {0})", data);
			case ATT_CAT_NOT_ASSOCIATED: return MessageFormat.format("Attribute ''{0}'' not associated with category ''{1}''. Attribute value will not be set for observation.", data);

			case DEFAULT_ATTRIBUTE_NOT_FOUND: return MessageFormat.format("Attribute not found. The default setting for this value will be ignored for the observation. (uuid: {0})", data);
			
			case INVALID_CM: return MessageFormat.format("Source configurable model not found. Source configurable model will be null for this waypoint. (mode uuid: {0}). Ensure your desktop Conservation Area is synchronized with Connect.", data);
			case INVALID_SIGNATURE: return MessageFormat.format("Signature type not found. The file will be imported as a regular attachment. (signature key: {0}).", data);
			case INVALID_OBSERVER: return MessageFormat.format("Observer not found found. The observer will be empty for this waypoint. (uuid: {0})", data);
			
			case INVALID_ATTACHMENT: return MessageFormat.format("Could not process attachment. Attachment will not be imported. (attachment number: {0})", data);
			case INVALID_PHOTO_ATTACHMENT: return MessageFormat.format("Could not determine photo attachment type. Attachment will be imported with an unknown file extension. (attachment number: {0})", data);
			
			case COULD_NOT_PARSE_BOOLEAN: return MessageFormat.format("Cannot parse boolean value from ''{0}''. Attribute value will not be set.", data);
			case COULD_NOT_PARSE_DATE: return MessageFormat.format("Cannot parse date from ''{0}''. Date must be provided in either ''{1}'' or ''{2''} format. Attribute value will not be set.", data[0], CtJsonUtil.JSON_DATE_FORMAT_STR, CtJsonUtil.JSON_ATTRIBUTE_DATE_FORMAT_STR);
			
			case DUPLICATE_ATTRIBUTES: return MessageFormat.format("The same attribute ({0}) cannot be specified twice for a single observation.", data);
			
			case OBS_ATTRIBUTE_PARSE_ERROR: return MessageFormat.format("Could not parse value for attribute {0}: {1}: {2}.", data);
			case DEFAULT_ATTRIBUTE_PARSE_ERROR: return MessageFormat.format("Could not parse value for default values for attribute {0}: {1}: {2}. Default values will be ignored for this observation.", data);
			
			case JSON_FEATURE_PARSE_ERROR: return MessageFormat.format("Error parsing feature information (feature will not be processed): {0}",data);
		}
		return "Unknown Warning";
	}
}
