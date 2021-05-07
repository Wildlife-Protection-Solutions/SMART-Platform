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
package org.wcs.smart.patrol.internal.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.json.PatrolAttributeMetadata;
import org.wcs.smart.patrol.json.PatrolJsonFeatureProcessor;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.ui.LabelConstants;

/**
 * Implementation for patrol label provider.
 * 
 * @author Emily
 *
 */
public class PatrolLabelProvider implements IPatrolLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof PatrolType){
			return getLabel(((PatrolType)item).getType(), l);
		}else if (item instanceof PatrolType.Type){
			return LabelConstants.getLabel( (PatrolType.Type)item);
		}else if (item instanceof PatrolWaypointSource){
			return Messages.PatrolWaypointSource_PatrolWaypointSourceName;
		}
		if (item.equals(PATROLTEAM_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolTeamColumnNameKey;
		if (item.equals(PATROLTT_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolTransportTypeColumnName;
		if (item.equals(PATROLMANDATE_TABLENAME_KEY)) return Messages.PatrolLabelProvider_PatrolMandateColumnName;
		if (item.equals(TEAMNAME_KEY)) return Messages.PatrolLabelProvider_TeamColumnName;
		if (item.equals(TEAMDESCRIPTION_KEY)) return Messages.PatrolLabelProvider_DescriptionColumnName;
		if (item.equals(TEAMMANDATE_KEY)) return Messages.PatrolLabelProvider_MandateColumnName;
		if (item.equals(TEAMACTIVE_KEY)) return Messages.PatrolLabelProvider_TeamActiveColumnName;
		if (item.equals(TRANSPORTNAME_KEY)) return Messages.PatrolLabelProvider_TransportTypeColumnName;
		if (item.equals(TRANSPORTACTIVE_KEY)) return Messages.PatrolLabelProvider_TransportActiveColumnName;
		if (item.equals(TRANSPORTTYPE_KEY)) return Messages.PatrolLabelProvider_PatrolTypeColumnName;
		if (item.equals(MANDATENAME_KEY)) return Messages.PatrolLabelProvider_PatrolMandateColumnName;
		if (item.equals(MANDATEACTIVE_KEY)) return Messages.PatrolLabelProvider_MandateActiveColumnName;
		
		if (item instanceof PatrolJsonFeatureProcessor.Messages) {
			return getMessage((PatrolJsonFeatureProcessor.Messages)item, l);
		}
		return null;
	}

	@Override
	public String getMessage(PatrolJsonFeatureProcessor.Messages message, Locale l) {
		switch(message) {
		case COMPLETE_MSG: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE;
		case INVALID_DATA_TYPE: return Messages.PatrolLabelProvider_JSONPROCESSOR_1;
		case INVALID_FEATURE_TYPE: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_2;
		case MISSING_PROPERTY: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_3;
		case PATROLLEG_LINK_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_4;
		case PATROL_LINK_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_5;
		case PATROL_LINK_EXISTS: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_6;
		case PATROLLEG_LINK_EXISTS: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_7;
		case PATROLLEG_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_8;
		case TRANSPORTTYPE_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_9;
		case MANDATE_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_10;
		case MANDATE_EXISTING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_11;
		case EMPLOYEE_NOT_FOUND: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_12;
		case NO_EMPLOYEES: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_13;
		case NO_LEADER: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_14;
		case NO_PILOT: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_15;
		case INVALID_PATROL_UUID: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_16;
		case INVALID_PATROLLEG_UUID: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_17;
		case PATROL_EXISTS: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_18;
		case PATROLLEG_EXISTS: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_19;
		case TEAM_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_20;
		case STATION_MISSING: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_21;
		case CUSTOM_ATTRIBUTE_ERROR: return Messages.PatrolLabelProvider_JSONPROCESSOR_COMPLETE_22;
		}
		return ""; //$NON-NLS-1$
	}
	
	public HashMap<Locale, String> getNames(PatrolAttributeMetadata.PatrolWaypointMetadata metadataOption){
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case DISTANCE: 
			key = Messages.PatrolLabelProvider_Distance;
			break;
		case BEARING:
			key = Messages.PatrolLabelProvider_Bearing;
			break;
		case COMMENT:
			key = Messages.PatrolLabelProvider_Comment;
			break;
		default:
			break;
		}
		
		//english
		Locale en = Locale.forLanguageTag("en"); //$NON-NLS-1$
		String enl = ResourceBundle.getBundle("", Locale.ROOT).getString(key); //$NON-NLS-1$
		translations.put(en, enl);

		for (Locale locale : Locale.getAvailableLocales()) {
			if (locale.equals(en)) continue;
			try {
				ResourceBundle b = ResourceBundle.getBundle("", locale); //$NON-NLS-1$
				if (b != null) {
					String value = b.getString(key);
					translations.put(locale, value);	
				}
			}catch (Exception ex) {
			}
		}
		
		return translations;
	}

	public HashMap<Locale, String> getNames(PatrolAttributeMetadata.FixedPatrolMetadata metadataOption){
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case ARMED: 
			key = Messages.PatrolLabelProvider_ArmedMetadata;
			break;
		case COMMENT:
			key = Messages.PatrolLabelProvider_CommentMetadata;
			break;
		case EMPLOYEES:
			key = Messages.PatrolLabelProvider_EmployeesMetadata;
			break;
		case LEADER:
			key = Messages.PatrolLabelProvider_LeaderMetadata;
			break;
		case MANDATE:
			key = Messages.PatrolLabelProvider_MandateMetadata;
			break;
		case OBJECTIVE:
			key = Messages.PatrolLabelProvider_ObjectiveMetdata;
			break;
		case PATROLID:
			key = Messages.PatrolLabelProvider_PidMetadata;
			break;
		case PILOT:
			key = Messages.PatrolLabelProvider_PilotMetadata;
			break;
		case STATION:
			key = Messages.PatrolLabelProvider_StationMetadata;
			break;
		case TEAM:
			key = Messages.PatrolLabelProvider_TeamMetadata;
			break;
		case TRANSPORT_TYPE:
			key = Messages.PatrolLabelProvider_TransportTypeMetadata;
			break;
		default:
			break;
		
		}
		//english
		Locale en = Locale.forLanguageTag("en"); //$NON-NLS-1$
		String enl = ResourceBundle.getBundle("", Locale.ROOT).getString(key); //$NON-NLS-1$
		translations.put(en, enl);

		for (Locale locale : Locale.getAvailableLocales()) {
			if (locale.equals(en)) continue;
			try {
				ResourceBundle b = ResourceBundle.getBundle("", locale); //$NON-NLS-1$
				if (b != null) {
					String value = b.getString(key);
					translations.put(locale, value);	
				}
			}catch (Exception ex) {
			}
		}
		
		return translations;
	}
}
