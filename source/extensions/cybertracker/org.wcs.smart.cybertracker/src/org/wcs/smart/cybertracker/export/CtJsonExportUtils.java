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
package org.wcs.smart.cybertracker.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.ScreenOptionUuid;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Shared utilities for exporting patrol and survey metadata 
 * to json file.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class CtJsonExportUtils {
	
	/**
	 * Json key for comment metadata
	 */
	public static final String JSON_COMMENT_METADATA_KEY = "comment"; //$NON-NLS-1$
	/**
	 * JSON key for employee metadata
	 */
	public static final String JSON_EMPLOYEE_METADATA_KEY = "employees";  //$NON-NLS-1$
	/**
	 * JSON key for leader metadata
	 */
	public static final String JSON_LEADER_METADATA_KEY = "leader";  //$NON-NLS-1$
	/**
	 * JSON key for pilot metadata
	 */
	public static final String JSON_PILOT_METADATA_KEY = "pilot";  //$NON-NLS-1$
	
	/**
	 * JSON is visible property key
	 */
	public static final String JSON_ISVISIBILE_PROP_KEY = "isVisible"; //$NON-NLS-1$
	
	/**
	 * JSON default property key
	 */
	public static final String JSON_DEFAULT_PROP_KEY = "default"; //$NON-NLS-1$
	
	/**
	 * JSON options property key; for list options
	 */
	public static final String JSON_OPTION_PROP_KEY = "options"; //$NON-NLS-1$
	
	/**
	 * JSON options key for representing the part list options
	 */
	public static final String JSON_OPTION_PARENT_KEY = "parent"; //$NON-NLS-1$
	
	/**
	 * JSON options key for representing the option name
	 */
	public static final String JSON_OPTION_LABEL_KEY = "label"; //$NON-NLS-1$
	
	/**
	 * JSON options property key that identifies the type
	 */
	public static final String JSON_OPTION_TYPE_KEY = "type"; //$NON-NLS-1$
	
	/**
	 * Option field types
	 * @author Emily
	 *
	 */
	public static enum Type{
		BOOLEAN,
		TEXT,
		SINGLE_CHOICE,
		MULTI_CHOICE
	}
	
	public static final String PROJECT_FILE = "project.json"; //$NON-NLS-1$
	
	/**
	 * Convert cybertracker properties profile to JSON string.
	 * 
	 * @param profile
	 * @return
	 */
	public static String toJson(CyberTrackerPropertiesProfile profile) {
		JSONObject profileObj = new JSONObject();
		
		for (ProfileOptionID option : ProfileOptionID.values()) {
			CyberTrackerPropertiesProfileOption opValue = profile.getOptions().get(option);
			if (opValue == null) continue;
			if (isBoolean(option)) {
				profileObj.put(option.name(), opValue.getBooleanValue());
			}else if (opValue.getDoubleValue() != null) {
				profileObj.put(option.name(), opValue.getDoubleValue());
			}else if (opValue.getIntegerValue() != null) {
				profileObj.put(option.name(), opValue.getIntegerValue());
			}else if (opValue.getStringValue() != null) {
				profileObj.put(option.name(), opValue.getStringValue());
			}
		}
		return profileObj.toJSONString();
	}

	public static void writeProjectJson(String projectName, String cmFile, Path logoFile, Path outputFile) throws IOException {
		JSONObject projectJSON = new JSONObject();
		projectJSON.put("projectName",projectName); //$NON-NLS-1$
		projectJSON.put("decoder","sourceparser_smartconfigurabledatamodel"); //$NON-NLS-1$ //$NON-NLS-2$
		projectJSON.put("source",Messages.CtJsonExportUtils_SmartCtSource); //$NON-NLS-1$
		projectJSON.put("definition",cmFile); //$NON-NLS-1$
		projectJSON.put("creation_date",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date())); //$NON-NLS-1$ //$NON-NLS-2$
		projectJSON.put("logo", (logoFile == null || !Files.exists(logoFile)) ? null : logoFile.getFileName().toString()); //$NON-NLS-1$
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(projectJSON.toJSONString());
		}
	}
	
	/**
	 * Identify which options are boolean as boolean options
	 * appear the same as integer options in the database.
	 * 
	 * @param option
	 * @return
	 */
	private static boolean isBoolean(ProfileOptionID option) {
		switch(option){
		
		case USE_TITLE_BAR:
		case USE_LARGE_TITLES:
		case USE_LARGE_TABS:
		case LARGE_SCROLL_BARS:
		case AUTO_NEXT:
		case SHOW_EDIT:
		case SHOW_GPS:
		case KIOSK_MODE:	
		case CAN_PAUSE:
		case SIMPLE_CAMERA:
		case DISABLE_EDITING:
		case USE_SD_CARD:
		case TEST_TIME:
		case RESET_ON_NEXT:
		case RESET_ON_SYNC:
		case USE_MAP_ON_SKIP:
		case USE_GPS_TIME:
		case MANUAL_GPS:
		case ALLOW_SKIP_MANUAL_GPS:
		case LOCK100:
			return true;
			
		case APP_NAME:
		case DILUTION_OF_PRECISION:
		case EXIT_PIN:
		case FIELD_MAP_FILENAME:
		case GPS_TIME_ZONE:
		case MAX_PHOTO_COUNT:
		case PROJECTION:
		case SIGHTING_ACCURACY:
		case SIGHTING_FIX_COUNT:
		case SKIP_BUTTON_TIMEOUT:
		case TRACK_ACCURACY:
		case UTM_ZONE:
		case WAYPOINT_TIMER:
			return false;
		}
		
		return false;
	}
	
	/**
	 * Convert string based metadata option to json
	 * 
	 * @param screenOption the database option
	 * @param opKey the JSON option key
	 * @param session
	 * @param ca
	 * @return
	 */
	public static JSONObject convertStringOp(ScreenOption screenOption, String opKey, String opLabel, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.TEXT.name());
		objective.put(JSON_OPTION_LABEL_KEY, opLabel);
		if (screenOption != null) {
			objective.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible() && screenOption.getStringValue() != null) {
				objective.put(JSON_DEFAULT_PROP_KEY, screenOption.getStringValue());
			}
		}else {
			objective.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(opKey, objective);
		return objectiveOp;
	}
	
	/**
	 * Convert employee metadata to JSON string
	 * 
	 * @param screenOption employee screen option
	 * @param session
	 * @param ca
	 * @return
	 */
	
	public static JSONObject convertEmployees(ScreenOption screenOption, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.MULTI_CHOICE.name());
		optionType.put(JSON_OPTION_LABEL_KEY, Messages.CtJsonExportUtils_EmployeePageLabel);
		if (screenOption != null) {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible()) {
				if (screenOption.getUuidList() != null) {
					for (ScreenOptionUuid defaultOp : screenOption.getUuidList()) {
						optionType.put(JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(defaultOp.getUuidValue()));
					}
				}
			}
		}else {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONArray optionOptions = new JSONArray();
		
		List<Employee> items = QueryFactory.buildQuery(session, Employee.class, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"endEmploymentDate", null}).list(); //$NON-NLS-1$
		
		for (Employee t : items) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", UuidUtils.uuidToString(t.getUuid())); //$NON-NLS-1$
			ttype.put("label", SmartLabelProvider.getShortLabel(t)); //$NON-NLS-1$
			optionOptions.add(ttype);
		}
		optionType.put(JSON_OPTION_PROP_KEY, optionOptions);
		
		JSONObject teamTypeOp = new JSONObject();
		teamTypeOp.put(JSON_EMPLOYEE_METADATA_KEY, optionType);
		return teamTypeOp;
	}
	
	public static JSONObject convertLeaderPilot(ScreenOption screenOption, String opKey, String opLabel, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		objective.put(JSON_OPTION_PARENT_KEY, JSON_EMPLOYEE_METADATA_KEY);
		objective.put(JSON_OPTION_LABEL_KEY, opLabel);
		if (screenOption != null) {
			objective.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible() && screenOption.getUuidValue() != null) {
				objective.put(JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(screenOption.getUuid()));
			}
		}else {
			objective.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(opKey, objective);
		return objectiveOp;
	}
	
	public static JSONObject convertKeyOptions(ScreenOption screenOption, Class<? extends NamedKeyItem> clazz, String screenKey, String opLabel, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		optionType.put(JSON_OPTION_LABEL_KEY, opLabel);
		if (screenOption != null) {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible()) {
				if (screenOption.getUuidValue() != null) {
					optionType.put(JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(screenOption.getUuidValue()));
				}else {
					optionType.put(JSON_DEFAULT_PROP_KEY, null);
				}
			}
		}else {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONArray optionOptions = new JSONArray();
		
		List<? extends NamedKeyItem> items = QueryFactory.buildQuery(session, clazz, 
				new Object[] {"conservationArea", ca}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		for (NamedKeyItem t : items) {
			JSONObject ttype = new JSONObject();
			ttype.put("uuid", UuidUtils.uuidToString(t.getUuid())); //$NON-NLS-1$
			ttype.put("key", t.getKeyId()); //$NON-NLS-1$
			ttype.put("label_default", t.findName(ca.getDefaultLanguage())); //$NON-NLS-1$
			for (Label l : t.getNames()) {
				ttype.put("label_" + l.getLanguage().getCode(), l.getValue()); //$NON-NLS-1$
				
			}
			optionOptions.add(ttype);
		}
		optionType.put(JSON_OPTION_PROP_KEY, optionOptions);
		
		JSONObject teamTypeOp = new JSONObject();
		teamTypeOp.put(screenKey, optionType);
		return teamTypeOp;
	}
	
}
