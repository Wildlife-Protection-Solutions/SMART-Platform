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
package org.wcs.smart.er.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.json.MissionAttributeMetadata;
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

	public static final String ID_COL_NAME = Messages.SamplingUnitEditorPage_IdColumnName;
	public static final String LENGTH_COL_NAME = Messages.SamplingUnitEditorPage_LengthColumnName;
	public static final String STATE_COL_NAME = Messages.SamplingUnitEditorPage_StateColumnName;
	
	
	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item == SamplingUnit.GeometryType.PLOT) return Messages.SamplingUnit_PointGeomType;
		if (item == SamplingUnit.GeometryType.TRANSECT) return Messages.SamplingUnit_LinearGeomType;
		if (item == SamplingUnit.State.ACTIVE) return Messages.SamplingUnit_ActiveState;
		if (item == SamplingUnit.State.INACTIVE) return Messages.SamplingUnit_InActiveState;
		if (item == SurveyDesign.State.ACTIVE) return Messages.SurveyDesign_ActiveStateLabel;
		if (item == SurveyDesign.State.INACTIVE) return Messages.SurveyDesign_InActiveStateLabel;
		if (item == MissionTrack.TrackType.TRACK) return Messages.MissionTrack_TrackGuiName;
		if (item == MissionTrack.TrackType.SAMPLING_UNIT) return Messages.MissionTrack_SuTrackGuiName;
		if (item instanceof SurveyWaypointSource) return  Messages.SurveyWaypointSource_Name;
		if (item.equals(ID_COLUMN_KEY))return ID_COL_NAME;
		if (item.equals(LENGTH_COLUMN_KEY)) return LENGTH_COL_NAME;
		if (item.equals(STATE_COLUMN_KEY)) return STATE_COL_NAME;
		if (item.equals(SU_TABLE_LONGNAME_KEY)) return Messages.ErLabelProvider_su_table_name;		
		if (item.equals(SD_TABLE_LONGNAME_KEY)) return Messages.ErLabelProvider_sd_table_name;
		if (item.equals(SD_DESCRIPTION_COL_KEY)) return Messages.ErLabelProvider_DescriptionColumn;
		if (item.equals(SD_STATUS_COL_KEY)) return Messages.ErLabelProvider_StatusColumn;
		if (item.equals(SD_KEY_COL_KEY)) return Messages.ErLabelProvider_KeyColumn;
		if (item.equals(SD_NAME_COL_KEY)) return Messages.ErLabelProvider_NameColumn;
		if (item == SURVEY_NAME)  return Messages.ErLabelProvider_SurveyLabel;
		if (item instanceof MissionJsonFeatureProcessor.Messages ) return getMessage((MissionJsonFeatureProcessor.Messages)item, l);
		
		return null;
	}

	public static Image getImage(Object item){
		if (item == SamplingUnit.GeometryType.PLOT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_TRANSECT_ICON);
		}
		if (item == SamplingUnit.GeometryType.TRANSECT){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SAMPLING_UNIT_PLOT_ICON); 
		}
		return null;
	}
	
	public HashMap<Locale, String> getNames(MissionAttributeMetadata.MissionMetadata metadataOption){
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case COMMENT:
			key = Messages.ErLabelProvider_MissionMetadataComment;
			break;
		case EMPLOYEES:
			key = Messages.ErLabelProvider_MissionMetadataMembers;
			break;
		case LEADER:
			key = Messages.ErLabelProvider_MissionMetadataLeader;
			break;
		case MISSIONID:
			key = Messages.ErLabelProvider_MissionMetadataId;
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
	}
	
	public HashMap<Locale, String> getNames(MissionAttributeMetadata.MissionWaypointMetadata metadataOption){
		HashMap<Locale, String> translations = new HashMap<>();		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
		case COMMENT:
			key = Messages.ErLabelProvider_MissionMetadataComment;
			break;
		case DISTANCE:
			key = Messages.ErLabelProvider_MissionMetadataDistance;
			break;
		case BEARING:
			key = Messages.ErLabelProvider_MissionMetadataBearing;
			break;
		case SAMPLING_UNIT:
			key = Messages.ErLabelProvider_MissionMetadataSamplingUnit;
			break;
		default:
			break;
		
		}
		addTranslations(translations, key);
		return translations;
	}
	
	public HashMap<Locale, String> getNames(MissionAttributeMetadata.MissionTrackMetadata metadataOption){
		
		HashMap<Locale, String> translations = new HashMap<>();
		
		String key = ""; //$NON-NLS-1$
		switch(metadataOption) {
			case SAMPLING_UNIT:
				key = Messages.ErLabelProvider_MissionMetadataSamplingUnit;
				break;
			default:
				break;		
		}
		addTranslations(translations, key);		
		return translations;
	}
	
	public String getMessage(MissionJsonFeatureProcessor.Messages message, Locale l) {
		switch(message){
			case MISSION_LINK_EXISTS: return Messages.ErLabelProvider_MissionJsonMessage1;
			case COMPLETE_MSG: return Messages.ErLabelProvider_MissionJsonMessage2;
			case MISSION_LINK_MISSING: return Messages.ErLabelProvider_MissionJsonMessage3;
			case INVALID_MISSION_UUID: return Messages.ErLabelProvider_MissionJsonMessage4;
			case MISSIONDAY_MISSING: return Messages.ErLabelProvider_MissionJsonMessage5;
			case MISSING_PROPERTY: return Messages.ErLabelProvider_MissionJsonMessage6;
			case MISSION_EXISTS: return Messages.ErLabelProvider_MissionJsonMessage7;
			case INVALID_SURVEY_UUID: return Messages.ErLabelProvider_MissionJsonMessage8;
			case SURVEY_NOT_FOUND: return Messages.ErLabelProvider_MissionJsonMessage9;
			case EMPLOYEE_NOT_FOUND: return Messages.ErLabelProvider_MissionJsonMessage10;
			case NO_LEADER: return Messages.ErLabelProvider_MissionJsonMessage11;
			case NO_EMPLOYEES: return Messages.ErLabelProvider_MissionJsonMessage12;
			case CUSTOM_ATTRIBUTE_ERROR: return Messages.ErLabelProvider_MissionJsonMessage13;
			case INVALID_DATA_TYPE: return Messages.ErLabelProvider_MissionJsonMessage14;
			case INVALID_FEATURE_TYPE: return Messages.ErLabelProvider_MissionJsonMessage15;
			case SU_MISSING: return Messages.ErLabelProvider_MissionJsonMessage16;
			case TRACKID: return Messages.ErLabelProvider_MissionJsonMessage17;
			case DESIGN_MISSING: return Messages.ErLabelProvider_MissionJsonMessage18;
			case SURVEY_EXISTS: return Messages.ErLabelProvider_ErLabelProvider_MissionJsonMessage19;
			case SURVEY_LINK_EXISTS: return Messages.ErLabelProvider_ErLabelProvider_MissionJsonMessage20;
		}
		return ""; //$NON-NLS-1$
	}
}
