package org.wcs.smart.cybertracker.json;

import java.text.MessageFormat;

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
