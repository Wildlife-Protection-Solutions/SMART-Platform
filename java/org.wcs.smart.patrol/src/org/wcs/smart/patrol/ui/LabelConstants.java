package org.wcs.smart.patrol.ui;

import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.ScreenOption;

public class LabelConstants {

	public static final String TEAM_NAME = Messages.Team_Name;
	public static final String TEAM_DESCRIPTION = Messages.Team_DescriptionFieldName;
	public static final String TEAM_MANDATE = Messages.Team_MandateFieldName;
	public static final String TEAM_KEY = Messages.Team_KeyFieldName;
	

	public static final String MANDATE_NAME = Messages.PatrolMandate_MandateName;
	public static final String MANDATE_KEY = Messages.PatrolMandate_MandateKey;
	

	public static final String TRANSTYPE_NAME = Messages.PatrolTransportType_Name;
	public static final String TRANSTYPE_KEY = Messages.PatrolTransportType_Key;
	
	public static final String GROUND_NAME = Messages.PatrolType_GroundName;
	public static final String MARINE_NAME = Messages.PatrolType_WaterName;
	public static final String AIR_NAME = Messages.PatrolType_AirName;
	
	
	public static final String SC_OP_TYPE =Messages.ScreenOption_PatrolType;
	public static final String SC_OP_TRANSPORT = Messages.ScreenOption_TransportType;
	public static final String SC_OP_ARMED = Messages.ScreenOption_Armed;
	public static final String SC_OP_STATION = Messages.ScreenOption_Station;
	public static final String SC_OP_TEAM = Messages.ScreenOption_Team;
	public static final String SC_OP_MANDATE = Messages.ScreenOption_Mandate;
	public static final String SC_OP_OBJECTIVE = Messages.ScreenOption_Objective;
	public static final String SC_OP_COMMENT = Messages.ScreenOption_Comment;
	public static final String SC_OP_MEMBERS = Messages.ScreenOption_Members;
	public static final String SC_OP_LEADER = Messages.ScreenOption_Leader;
	public static final String SC_OP_PILOT = Messages.ScreenOption_Pilot;
	
	public static String getLabel(PatrolType pt){
		return getLabel(pt.getType());
	}
	
	public static String getLabel(PatrolType.Type pt){
		switch(pt){
			case AIR: return AIR_NAME;
			case GROUND: return GROUND_NAME;
			case MARINE: return MARINE_NAME;
		}
		return null;
	}
	
	public static String getLabel(ScreenOption op){
		return getLabel(op.getType());
	}
	public static String getLabel(ScreenOption.ScreenOptionMeta op){
		switch(op){
		case ARMED:return SC_OP_ARMED;
		case COMMENT: return SC_OP_COMMENT;
		case LEADER:return SC_OP_LEADER;
		case MANDATE:return SC_OP_MANDATE;
		case MEMBERS:return SC_OP_MEMBERS;
		case OBJECTIVE:return SC_OP_OBJECTIVE;
		case PILOT:return SC_OP_PILOT;
		case STATION:return SC_OP_STATION;
		case TEAM:return SC_OP_TEAM;
		case TRANSPORT:return SC_OP_TRANSPORT;
		case TYPE: return SC_OP_TYPE;
		default:
			return null;
			
		}
	}
}
