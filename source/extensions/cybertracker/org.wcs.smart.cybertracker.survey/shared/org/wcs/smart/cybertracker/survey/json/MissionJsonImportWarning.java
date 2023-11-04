package org.wcs.smart.cybertracker.survey.json;

import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class MissionJsonImportWarning extends JsonImportWarning {

	public static final String TT_NOT_FOUND_ERROR = "Patrol transport type not found.";
	
	public enum WarningType{
		TRACK_POINT_MULTI_MATCHES("The track point {0} matches multiple missions [{1}].  Ensure the missions days and times do not overlap and try again"),
		SU_NOT_FOUND("Sampling unit not found. Sampling unit will not be set for this feature ({0})."),
		REST_TIME_ERROR("Could not compute rest time between pause and resume."),
		MISSION_NOT_FOUND("Misson not found for 'add to previous waypoint' observation. Data will not be imported."),
		SURVEY_DESIGN_NOTFOUND("Survey design not found ({0}). Data will not be imported."),
		MISSION_ATTRIBUTE_NOT_FOUND("Mission attribute not ({0}). Mission attribute data will not be imported."),
		MULTIPLE_ATTRIBUTES_FOUND("Multiple mission attributes found ({0}). Mission attribute data will not be imported."),
		LIST_ITEM_NOT_FOUND("Mission attribute list item not found ({0}). Mission attribute value will be empty."),
		MEMBER_NOT_FOUND("Member not found ({0}).  Member will not be added to mission");
				
		
		String message;
		WarningType(String message, Object...data){
			this.message = message;
		}
		
		public String getMessage() {
			return this.message;
		}
	}
	

	public MissionJsonImportWarning(WarningType type, Object...data) {
		super(type.getMessage(), data);
	}
}
