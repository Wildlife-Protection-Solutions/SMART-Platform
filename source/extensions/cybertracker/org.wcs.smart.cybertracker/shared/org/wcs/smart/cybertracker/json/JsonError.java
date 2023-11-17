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
 * SMART Mobile processing errors
 * 
 * @author Emily
 *
 */
public class JsonError {

	enum Type{
		JSON_PARSE_ERROR ("Unable to parse JSON text: {0}."),
		FEATURES_NOT_FOUND("No JSON object with key ''{0}'' found"),
		INVALID_OBS_COUNTER("Invalid value for observation counter field {0}."),
		FEATURE_OBJECT_NOT_FOUND("Feature object does not have type ''{0}''"),
		LAT_LONG_NOT_FOUND("Longitude/Latitude values not found");
		
		private String text;
		
		Type(String text) {
			this.text = text;
			
		}
		public String getText() {
			return this.text;
		}
	}
	
	private Type type;
	private Object[] data;
	private String message;
	
	public JsonError(Type type, Object... data){
		this.type = type;
		this.data = data;
	}
	
	public JsonError(String message, Object... data){
		this.message = message;
		this.data = data;
	}
	
	public String getMessage() {
		if (type != null) return MessageFormat.format(type.text, data);
		return MessageFormat.format(message, data);
	}
}
