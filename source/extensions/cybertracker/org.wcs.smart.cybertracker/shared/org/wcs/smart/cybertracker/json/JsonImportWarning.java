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
import java.util.Locale;
import java.util.function.Function;

import org.wcs.smart.SmartContext;
import org.wcs.smart.cybertracker.ICyberTrackerLabelProvider;

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
		COULD_NOT_PARSE_GEOMETRY,  //1 parameter attribute name
		INVALID_LINESTRING_GEOMETRY, //1 parameter attribute name
		INVALID_POLYGON_GEOMETRY,   //1 parameter attribute name
		
		DUPLICATE_ATTRIBUTES, //1 parameter attribute name
		
		OBS_ATTRIBUTE_PARSE_ERROR, // 3 params (attribute name, value parsing, error info)
		DEFAULT_ATTRIBUTE_PARSE_ERROR, // 3 params (attribute name, value parsing, error info)
		
		DEFAULT_ATTRIBUTE_NOT_FOUND, //1 parameter - attribute uuid as string
		
		JSON_FEATURE_PARSE_ERROR; //1 parameter error message
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(ICyberTrackerLabelProvider.class).getLabel(this, l);
		}
	}
	
	private Type type;
	private Function<Locale, String> message;
	private Object[] data;
	
	public JsonImportWarning(Type type) {
		this(type, new Object[0]);
	}
	
	public JsonImportWarning(Type type, Object... data) {
		this.type = type;
		this.data = data;
	}
	
	public JsonImportWarning(Function<Locale, String> message, Object... data) {
		this.data = data;
		this.message = message;
	}
	
	
	public String getMessage(Locale l) {
		if (type == null) { 
			if (data == null || data.length == 0) return message.apply(l);
			return MessageFormat.format(message.apply(l), data);
		}	
		switch(type) {
			case COULD_NOT_PARSE_DATE: return MessageFormat.format(type.getMessage(l), data[0], CtJsonUtil.JSON_DATE_FORMAT_STR, CtJsonUtil.JSON_ATTRIBUTE_DATE_FORMAT_STR);
			default: return MessageFormat.format(type.getMessage(l), data);
		}
	}
}
