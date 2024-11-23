/*
 * Copyright (C) 2012 Wildlife Conservation Society
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

package org.wcs.smart.patrol.ui;

import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.meta.PatrolScreenOptionMeta;

public class LabelConstants {

	public static final String TRACKPOINTS = Messages.LabelConstants_PatrolTrackpointsLabel;
	
	public static final String CLEAR_IMAGE = Messages.PatrolMandatePropertyPage_ClearImage;
	
	public static final String TEAM_NAME = Messages.Team_Name;
	public static final String TEAM_DESCRIPTION = Messages.Team_DescriptionFieldName;
	public static final String TEAM_MANDATE = Messages.Team_MandateFieldName;
	public static final String TEAM_KEY = Messages.Team_KeyFieldName;

	public static final String TRACK_TYPE= Messages.LabelConstants_TrackType;
	
	public static final String ARMED= Messages.LabelConstants_ArmedField;
	public static final String COMMENT= Messages.LabelConstants_CommentField;
	public static final String MEMBERS= Messages.LabelConstants_MembersField;
	public static final String LEADER= Messages.LabelConstants_LeaderField;
	public static final String PILOT= Messages.LabelConstants_PilotField;
	public static final String OBJECTIVE= Messages.LabelConstants_ObjectiveField;
	
	public static final String REFRESH_JOB_NAME = Messages.LabelConstants_RefreshJob;
	
	public static final String STATION_NAME = Messages.LabelConstants_StationField;
	public static final String CUSTOM_METADATA_NAME = Messages.LabelConstants_CustomMetadataField;
	public static final String DATES = Messages.LabelConstants_DatesField;
	public static final String ID = Messages.LabelConstants_IDField;
	
	public static final String MANDATE_NAME = Messages.PatrolMandate_MandateName;
	public static final String MANDATE_KEY = Messages.PatrolMandate_MandateKey;

	public static final String ENVIRONMENT_NAME = Messages.LabelConstants_EnvironmentField;
	
	public static final String TRANSPORT_MODE = Messages.PatrolTransportType_Name2;
	public static final String TRANSTYPE_KEY = Messages.PatrolTransportType_Key;
	
	public static final String GROUND_NAME = Messages.PatrolType_GroundName;
	public static final String MARINE_NAME = Messages.PatrolType_WaterName;
	public static final String AIR_NAME = Messages.PatrolType_AirName;
	public static final String MIXED_NAME =Messages.PatrolType_MixedName;
	
	public static final String SC_OP_TRANSPORT = TRANSPORT_MODE;
	public static final String SC_OP_ARMED = Messages.ScreenOption_Armed2;
	public static final String SC_OP_STATION = Messages.ScreenOption_Station;
	public static final String SC_OP_TEAM = Messages.ScreenOption_Team;
	public static final String SC_OP_MANDATE = Messages.ScreenOption_Mandate2;
	public static final String SC_OP_OBJECTIVE = Messages.ScreenOption_Objective2;
	public static final String SC_OP_COMMENT = Messages.ScreenOption_Comment2;
	public static final String SC_OP_MEMBERS = Messages.ScreenOption_Members2;
	public static final String SC_OP_LEADER = Messages.ScreenOption_Leader2;
	public static final String SC_OP_PILOT = Messages.ScreenOption_Pilot2;
	
		
	public static String getLabel(PatrolScreenOptionMeta op){
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
		default:
			return null;
			
		}
	}
}
