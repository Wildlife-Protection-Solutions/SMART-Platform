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

import java.util.List;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedKeyItem;
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
			if (opValue.getBooleanValue() != null) {
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
	
	/**
	 * Convert string based metadata option to json
	 * 
	 * @param screenOption the database option
	 * @param opKey the JSON option key
	 * @param session
	 * @param ca
	 * @return
	 */
	public static JSONObject convertStringOp(ScreenOption screenOption, String opKey, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
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
	
	public static JSONObject convertLeaderPilot(ScreenOption screenOption, String opKey, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
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
	
	public static JSONObject convertKeyOptions(ScreenOption screenOption, Class<? extends NamedKeyItem> clazz, String screenKey, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		
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
