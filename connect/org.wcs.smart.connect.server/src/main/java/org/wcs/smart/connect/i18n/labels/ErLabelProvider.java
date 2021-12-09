/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.i18n.labels;

import java.util.HashMap;
import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.er.json.MissionAttributeMetadata.MissionMetadata;
import org.wcs.smart.er.json.MissionAttributeMetadata.MissionTrackMetadata;
import org.wcs.smart.er.json.MissionAttributeMetadata.MissionWaypointMetadata;
import org.wcs.smart.er.json.MissionJsonFeatureProcessor;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypointSource;

/**
 * Label provider for survey elements.
 * 
 * @author Emily
 *
 */
public class ErLabelProvider implements IErLabelProvider{

	@Override
	public String getLabel(Object item, Locale l) {
		if (item == SamplingUnit.GeometryType.PLOT) return Messages.getString("ErLabelProvider.Point", l); //$NON-NLS-1$
		if (item == SamplingUnit.GeometryType.TRANSECT) return Messages.getString("ErLabelProvider.Line", l);  //$NON-NLS-1$
		if (item == SamplingUnit.State.ACTIVE) return Messages.getString("ErLabelProvider.Active", l); //$NON-NLS-1$
		if (item == SamplingUnit.State.INACTIVE) return Messages.getString("ErLabelProvider.InActive", l); //$NON-NLS-1$
		if (item == SurveyDesign.State.ACTIVE) return Messages.getString("ErLabelProvider.SDActive", l); //$NON-NLS-1$ 
		if (item == SurveyDesign.State.INACTIVE) return Messages.getString("ErLabelProvider.SDInactive", l); //$NON-NLS-1$
		if (item == MissionTrack.TrackType.TRACK) return Messages.getString("ErLabelProvider.MissionTrackUnassociated", l); //$NON-NLS-1$
		if (item == MissionTrack.TrackType.SAMPLING_UNIT) return Messages.getString("ErLabelProvider.MissionTrackSU", l); //$NON-NLS-1$
		if (item instanceof SurveyWaypointSource) return  Messages.getString("ErLabelProvider.MissionTrackSurvey", l); //$NON-NLS-1$
		if (item.equals(ID_COLUMN_KEY)) return Messages.getString("ErLabelProvider.IDColumnName", l); //$NON-NLS-1$
		if (item.equals(LENGTH_COLUMN_KEY)) return Messages.getString("ErLabelProvider.LengthColumName", l); //$NON-NLS-1$
		if (item.equals(STATE_COLUMN_KEY)) return Messages.getString("ErLabelProvider.StatusColumnName", l); //$NON-NLS-1$
		if (item.equals(SU_TABLE_LONGNAME_KEY)) return Messages.getString("ErLabelProvider.SuTableLogName", l); //$NON-NLS-1$
		if (item.equals(SD_TABLE_LONGNAME_KEY)) return Messages.getString("ErLabelProvider.SuveyDesignTableName", l); //$NON-NLS-1$
		if (item.equals(SD_DESCRIPTION_COL_KEY)) return Messages.getString("ErLabelProvider.DescriptionColumn", l); //$NON-NLS-1$
		if (item.equals(SD_STATUS_COL_KEY)) return Messages.getString("ErLabelProvider.StatusColumn", l); //$NON-NLS-1$
		if (item.equals(SD_KEY_COL_KEY)) return Messages.getString("ErLabelProvider.KeyColumn", l); //$NON-NLS-1$
		if (item.equals(SD_NAME_COL_KEY)) return Messages.getString("ErLabelProvider.NameColumn", l); //$NON-NLS-1$
		if (item == IErLabelProvider.SURVEY_NAME) return Messages.getString("ErLabelProvider.SurveyName", l); //$NON-NLS-1$
		if (item instanceof MissionJsonFeatureProcessor.Messages) return getMessage((MissionJsonFeatureProcessor.Messages)item, l);
		return null;
	}

	@Override
	public String getMessage(MissionJsonFeatureProcessor.Messages message, Locale l) {
		switch(message){
		case MISSION_LINK_EXISTS: return Messages.getString("ErLabelProvider.JsonProcessorMessage1", l); //$NON-NLS-1$
		case COMPLETE_MSG: return Messages.getString("ErLabelProvider.JsonProcessorMessage2", l); //$NON-NLS-1$
		case MISSION_LINK_MISSING: return Messages.getString("ErLabelProvider.JsonProcessorMessage3", l); //$NON-NLS-1$
		case INVALID_MISSION_UUID: return Messages.getString("ErLabelProvider.JsonProcessorMessage4", l); //$NON-NLS-1$
		case MISSIONDAY_MISSING: return Messages.getString("ErLabelProvider.JsonProcessorMessage5", l); //$NON-NLS-1$
		case MISSING_PROPERTY: return Messages.getString("ErLabelProvider.JsonProcessorMessage6", l); //$NON-NLS-1$
		case MISSION_EXISTS: return Messages.getString("ErLabelProvider.JsonProcessorMessage7", l); //$NON-NLS-1$
		case INVALID_SURVEY_UUID: return Messages.getString("ErLabelProvider.JsonProcessorMessage8", l); //$NON-NLS-1$
		case SURVEY_NOT_FOUND: return Messages.getString("ErLabelProvider.JsonProcessorMessage9", l); //$NON-NLS-1$
		case EMPLOYEE_NOT_FOUND: return Messages.getString("ErLabelProvider.JsonProcessorMessage10", l); //$NON-NLS-1$
		case NO_LEADER: return Messages.getString("ErLabelProvider.JsonProcessorMessage11", l); //$NON-NLS-1$
		case NO_EMPLOYEES: return Messages.getString("ErLabelProvider.JsonProcessorMessage12", l); //$NON-NLS-1$
		case CUSTOM_ATTRIBUTE_ERROR: return Messages.getString("ErLabelProvider.JsonProcessorMessage13", l); //$NON-NLS-1$
		case INVALID_DATA_TYPE: return Messages.getString("ErLabelProvider.JsonProcessorMessage14", l); //$NON-NLS-1$
		case INVALID_FEATURE_TYPE: return Messages.getString("ErLabelProvider.JsonProcessorMessage15", l); //$NON-NLS-1$
		case SU_MISSING: return Messages.getString("ErLabelProvider.JsonProcessorMessage16", l); //$NON-NLS-1$
		case TRACKID: return Messages.getString("ErLabelProvider.JsonProcessorMessage17", l); //$NON-NLS-1$
		case DESIGN_MISSING: return Messages.getString("ErLabelProvider.JsonProcessorMessage18", l); //$NON-NLS-1$
		case SURVEY_EXISTS: return Messages.getString("ErLabelProvider.ErLabelProvider.JsonProcessorMessage19", l); //$NON-NLS-1$
		case SURVEY_LINK_EXISTS: return Messages.getString("ErLabelProvider.ErLabelProvider.JsonProcessorMessage20", l); //$NON-NLS-1$
		case OBSERVATION_EXISTS: return Messages.getString("ErLabelProvider.ObservationExists", l); //$NON-NLS-1$
		case OBSERVATION_NOT_FOUND: return Messages.getString("ErLabelProvider.ObservationNotFound", l); //$NON-NLS-1$
		case WAYPOINT_NOT_FOUND: return Messages.getString("ErLabelProvider.WaypointnotFound", l); //$NON-NLS-1$
		case CANNOT_UPDATE_DATE: return Messages.getString("ErLabelProvider.CannotChangeDate", l); //$NON-NLS-1$
		case CANNOT_UPDATE_SU: return Messages.getString("ErLabelProvider.CannotChangeSamplingUnit", l); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public HashMap<Locale, String> getNames(MissionMetadata metadataOption) {
	HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case COMMENT: key = "ErLabelProvider.CommentMetadata"; //$NON-NLS-1$
			break;
		case EMPLOYEES: key = "ErLabelProvider.MembersMetadata"; //$NON-NLS-1$
			break;
		case LEADER: key = "ErLabelProvider.LeaderMetadata"; //$NON-NLS-1$
			break;
		case MISSIONID: key = "ErLabelProvider.MissionIdMetadata"; //$NON-NLS-1$
			break;
		case SURVEY: key = "ErLabelProvider.SurveyMetadata"; //$NON-NLS-1$
			break; 
		case SURVEYDESIGN: key = "ErLabelProvider.SurveyDesignMetadata"; //$NON-NLS-1$
			break;
		default:
			break;
	
		}
		addTranslations(translations, key);
		return translations;
	}

	@Override
	public HashMap<Locale, String> getNames(MissionWaypointMetadata metadataOption) {
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case SAMPLING_UNIT: key = "ErLabelProvider.SuMetadata"; //$NON-NLS-1$
			break;
		case BEARING: key = "ErLabelProvider.BearingMetadata"; //$NON-NLS-1$
			break;
		case COMMENT: key = "ErLabelProvider.CommentMetadata"; //$NON-NLS-1$
			break;
		case DISTANCE: key = "ErLabelProvider.DistanceMetadata"; //$NON-NLS-1$
			break;
		default:
			break;
		}
		addTranslations(translations, key);
		return translations;
	}

	@Override
	public HashMap<Locale, String> getNames(MissionTrackMetadata metadataOption) {
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case SAMPLING_UNIT: key = "ErLabelProvider.SamplingUnitMetadata"; //$NON-NLS-1$
			break;
		default:
			break;
		
		
		}
		addTranslations(translations, key);
		return translations;
	}
	
	private void addTranslations(HashMap<Locale, String> translations, String key) {
		//english
		Locale en = Locale.forLanguageTag("en"); //$NON-NLS-1$
		String enl = Messages.getString(key, Locale.ROOT);
		translations.put(en, enl);

		for (Locale locale : Locale.getAvailableLocales()) {
			if (locale.equals(en)) continue;
			
			String value = Messages.getLocaleString(key, locale);
			if (value != null) {
				translations.put(locale, value);
			}
		}
	}
}
