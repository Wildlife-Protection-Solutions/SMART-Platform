package org.wcs.smart.cybertracker.patrol.json;

import org.wcs.smart.cybertracker.json.JsonImportWarning;

public class PatrolJsonImportWarning extends JsonImportWarning {

	public static final String TT_NOT_FOUND_ERROR = "Patrol transport type not found.";
	
	public enum WarningType{
		STATION_NOT_FOUND("Station value not found. Station will be empty."),
		TEAM_NOT_FOUND("Team value not found. Station will be empty."),
		MANDATE_NOT_FOUND("Mandate value not found. Station will be empty."),
		MEMBER_NOT_FOUND("Member not found. Member will not be added to patrol."),
		TT_NOT_FOUND_ERROR("Patrol transport type not found. Patrol part will not be imported."),
		TRACK_POINT_MULTI_MATCHES("The track point {0} matches multiple patrols [{1}].  Ensure the patrol days and times do not overlap and try again."),
		PATROL_NOT_FOUND("No patrol found for 'add to previous waypoint' observation."),
		DUPLICATE("Possible duplicate processing of file. The patrol {0} linked with this SMART Mobile data already exists in the database with an observation counter greater than the observation counter in the file ({1} > {2})");
		
		String message;
		WarningType(String message, Object...data){
			this.message = message;
		}
		
		public String getMessage() {
			return this.message;
		}
	}
	

	public PatrolJsonImportWarning(WarningType type, Object...data) {
		super(type.getMessage(), data);
	}
}
