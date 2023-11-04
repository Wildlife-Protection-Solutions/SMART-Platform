package org.wcs.smart.smartcollection.json;

import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class SmartCollectJsonImportWarning extends JsonImportWarning {

	public enum WarningType{
		NO_USER("No user specified for SMARTCollect feature.  Feature will not be loaded: {0}"),
		MISSING_DEVICE_ID("No device id specified for SMARTCollect feature.  Feature will not be loaded: {0}"),
		FEATURE_DISCARDED("{0} features discarded."),
		USER_BLACKLISTED_FEATURE_DISCARDED("The user {0} is blacklisted.  The {1} features reported by this user were not loaded.");
		
		String message;
		WarningType(String message, Object...data){
			this.message = message;
		}
		
		public String getMessage() {
			return this.message;
		}
	}
	

	public SmartCollectJsonImportWarning(WarningType type, Object...data) {
		super(type.getMessage(), data);
	}
}
