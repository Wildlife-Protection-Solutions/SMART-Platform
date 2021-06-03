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

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.ca.EmployeeTeamMember;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttribute.HelpImageLocation;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.dataentry.model.ScreenOptionUuid;
import org.wcs.smart.dataentry.model.xml.generated.AttributeOptionType;
import org.wcs.smart.dataentry.model.xml.generated.NodeType;
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
	 * JSON uuid property key value
	 */
	public static final String JSON_PROP_UUID = "uuid"; //$NON-NLS-1$

	/**
	 * JSON key propery key value
	 */
	public static final String JSON_PROP_KEY = "key"; //$NON-NLS-1$

	/**
	 * The SMART JSON format version
	 */
	public static final String SMART_JSON_VERSION = "7.0"; //$NON-NLS-1$
	
	/**
	 * Distance Direction option for profile file
	 */
	private static final String DISTANCE_DIRECTION_OP = "RECORD_DISTANCE_BEARING"; //$NON-NLS-1$

	private static final String OBSERVER_OP = "RECORD_OBSERVER"; //$NON-NLS-1$

	/**
	 * JSON is required property key
	 */
	public static final String JSON_REQUIRED_PROP_KEY = "isRequired"; //$NON-NLS-1$
	

	/**
	 * JSON options key for representing if metaitem 
	 * is a patrol attribute or leg attribute 
	 */
	public static final String JSON_FIXED_PROP_KEY = "isFixed"; //$NON-NLS-1$
	
	/**
	 * JSON is visible property key
	 */
	public static final String JSON_ISVISIBILE_PROP_KEY = "isVisible"; //$NON-NLS-1$
	
	/**
	 * JSON default property key
	 */
	public static final String JSON_DEFAULT_PROP_KEY = "default"; //$NON-NLS-1$
	/**
	 * JSON defaults property key
	 * for multi-options (employees)
	 */
	public static final String JSON_DEFAULTS_PROP_KEY = "defaults"; //$NON-NLS-1$
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
	//public static final String JSON_OPTION_LABEL_KEY = "label"; //$NON-NLS-1$
	public static final String JSON_OPTION_LABEL_DEFAULT_KEY = "label_default"; //$NON-NLS-1$
	public static final String JSON_OPTION_LABEL_PREFIX_KEY = "label_"; //$NON-NLS-1$
	
	/**
	 * JSON options property key that identifies the type
	 */
	public static final String JSON_OPTION_TYPE_KEY = "type"; //$NON-NLS-1$
	
	/**
	 * JSON options property key that identifies the type
	 */
	public static final String JSON_OPTION_GENERATED_KEY = "generated"; //$NON-NLS-1$
	
	public static final String JSON_SERVER_DATA_KEY = "DATA_SERVER"; //$NON-NLS-1$
	public static final String JSON_SERVER_URL_KEY= "URL"; //$NON-NLS-1$
	public static final String JSON_SERVER_PASSWORD_KEY= "PASSWORD"; //$NON-NLS-1$
	public static final String JSON_SERVER_USERNAME_KEY= "USERNAME"; //$NON-NLS-1$
	public static final String JSON_SERVER_FREQUENCY_KEY= "FREQUENCY_MIN"; //$NON-NLS-1$
	public static final String JSON_SERVER_PROTOCOL_KEY= "PROTOCOL"; //$NON-NLS-1$
	
	public static final String JSON_SMART_VERSION_KEY= "smart_version"; //$NON-NLS-1$
	
	/**
	 * JSON options property key that identifies the type
	 */
	public static final String JSON_EMPLOYEE_METADATA_KEY = ScreensUtil.COMMON_PREFIX + "Employees"; //$NON-NLS-1$
	
	/**
	 * JSON options property key for map definition
	 */
	public static final String MAP_KEY = "map"; //$NON-NLS-1$
	
	/**
	 * JSON options property key for basemap definition
	 */
	public static final String BASEMAP_KEY = "basemap"; //$NON-NLS-1$
	/**
	 * JSON options property key for map layers 
	 */
	public static final String MAPLAYERS_KEY = "layers"; //$NON-NLS-1$
	
	
	/**
	 * Option field types
	 * @author Emily
	 *
	 */
	public static enum Type{
		BOOLEAN,
		TEXT,
		NUMERIC,
		SINGLE_CHOICE,
		MULTI_CHOICE,
		UUID,
		DATE,
		TIME
	}
	
	public static final String PROJECT_FILE = "project.json"; //$NON-NLS-1$
	

	private static HashMap<String,String> getTranslations(String defaultLabel, String key, ConservationArea ca){
		HashMap<String,String> translations = new HashMap<>();
		
		//english
		String enl = ResourceBundle.getBundle(Messages.BUNDLE_NAME, Locale.ROOT).getString(key);
		translations.put("en", enl); //$NON-NLS-1$
		
		Locale locale = Locale.getDefault();
		if (!locale.getLanguage().equalsIgnoreCase("en")) { //$NON-NLS-1$
			if(!defaultLabel.equalsIgnoreCase(enl))
				translations.put(locale.getLanguage(), defaultLabel);	
		}
		
		for (Language l : ca.getLanguages()) {
			locale = new Locale(l.getCode());
			locale = new Locale(locale.getLanguage());
			
			if (locale.getLanguage().equalsIgnoreCase("en")) continue; //$NON-NLS-1$
			if (locale.getLanguage().equalsIgnoreCase(Locale.getDefault().getLanguage())) continue;
			
			try {
				ResourceBundle b = ResourceBundle.getBundle(Messages.BUNDLE_NAME, locale);
				if (b != null) {
					String value = b.getString(key);
					if (value.equalsIgnoreCase(defaultLabel) || value.equalsIgnoreCase(enl)) continue;
					translations.put(locale.getLanguage(), value);	
				}
			}catch (Exception ex) {
			}
		}
		
		return translations;
	}
	
	/**
	 * Convert cybertracker properties profile to JSON string.
	 * 
	 * @param profile
	 * @return
	 */
	public static String toJson(CyberTrackerPropertiesProfile profile, boolean distanceDirection,
			boolean collectObserver, HashMap<String, Object> additions, 
			IEclipseContext context, Session session) {
		
		JSONObject profileObj = new JSONObject();
		
		for (ProfileOptionID option : ProfileOptionID.values()) {
			CyberTrackerPropertiesProfileOption opValue = profile.getOptions().get(option);
			
			if (opValue == null) {
				Object defaultValue = profile.getDefaultValue(option);
				profileObj.put(option.name(), defaultValue);
			}else {
				if (isBoolean(option)) {
					profileObj.put(option.name(), opValue.getBooleanValue());
				}else if (isColor(option) && opValue.getIntegerValue() != null) {
					Color c = new Color(opValue.getIntegerValue());
					profileObj.put(option.name(), Integer.toHexString(c.getRGB()).substring(2));
				}else if (opValue.getDoubleValue() != null) {
					profileObj.put(option.name(), opValue.getDoubleValue());
				}else if (opValue.getIntegerValue() != null) {
					profileObj.put(option.name(), opValue.getIntegerValue());
				}else if (opValue.getStringValue() != null) {
					profileObj.put(option.name(), opValue.getStringValue());
				}
			}
		}
		profileObj.put(DISTANCE_DIRECTION_OP, distanceDirection);
		profileObj.put(OBSERVER_OP, collectObserver); 
		 
		if(additions != null) {
			for (Entry<String, Object> e : additions.entrySet()) {
				profileObj.put(e.getKey(), e.getValue());
			}
		}
		return profileObj.toJSONString();
	}

	
	/**
	 * 
	 * @param projectName required projection name
	 * @param version required package version
	 * @param cmFile required configurable model file
	 * @param logoFile optional conservation area logo for styling
	 * @param outputFile required output file name
	 * @param metadataFilename optional metadata file
	 * @param projectAdditions optional addition key/value pairs to add.  Value can be single value or json object
	 * @throws IOException
	 */
	public static void writeProjectJson(String projectName, String version, String cmFile, 
			Path logoFile, Path outputFile, Path metadataFilename, HashMap<String, Object> projectAdditions) throws IOException {
		JSONObject projectJSON = new JSONObject();
		projectJSON.put("projectName",projectName); //$NON-NLS-1$
		projectJSON.put("decoder","sourceparser_smartconfigurabledatamodel"); //$NON-NLS-1$ //$NON-NLS-2$
		projectJSON.put("source",Messages.CtJsonExportUtils_SmartCtSource); //$NON-NLS-1$
		projectJSON.put("definition",cmFile); //$NON-NLS-1$
		if (metadataFilename != null) projectJSON.put("metadata", metadataFilename.getFileName().toString()); //$NON-NLS-1$
		if (version != null) projectJSON.put("version",  version); //$NON-NLS-1$ 
		projectJSON.put("creation_date",DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(ZonedDateTime.now())); //$NON-NLS-1$ //$NON-NLS-2$
		projectJSON.put("logo", (logoFile == null || !Files.exists(logoFile)) ? null : logoFile.getFileName().toString()); //$NON-NLS-1$
		projectJSON.put(JSON_SMART_VERSION_KEY, SMART_JSON_VERSION);
		if(projectAdditions != null) {
			for (Entry<String, Object> e : projectAdditions.entrySet()) {
				projectJSON.put(e.getKey(), e.getValue());
			}
		}
		
		try(BufferedWriter fw = Files.newBufferedWriter(outputFile)){
			fw.write(projectJSON.toJSONString());
		}
	}
	
	public static Path copyFiles(Path srcDirectory, Path targetDir) throws IOException {
		if (srcDirectory == null) return null;
		if (!Files.exists(srcDirectory)) return null;
		
		Files.createDirectories(targetDir);
		//copy over all map files
		Files.walkFileTree(srcDirectory, new SimpleFileVisitor<Path>() {
			 @Override
			    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
			        Files.createDirectories(targetDir.resolve(srcDirectory.relativize(dir)));
			        return FileVisitResult.CONTINUE;
			    }

			    @Override
			    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			    	Files.copy(file, targetDir.resolve(srcDirectory.relativize(file)));
			    	return FileVisitResult.CONTINUE;
			    }
		});
		return targetDir;
	}
	private static boolean isColor(ProfileOptionID option) {
		return option == ProfileOptionID.THEME_COLOR_1 
				|| option == ProfileOptionID.THEME_COLOR_2
				|| option == ProfileOptionID.THEME_COLOR_3
				|| option == ProfileOptionID.THEME_COLOR_4
				|| option == ProfileOptionID.TRACK_COLOR;
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
		case RESIZE_IMAGE:
		case INCIDENT_GROUP_UI:
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
		case WAYPOINT_TIMER_TYPE:
		case WAYPOINT_TIMER:
		case THEME_COLOR_1:
		case THEME_COLOR_2:
		case THEME_COLOR_3:
		case THEME_COLOR_4:
			return false;
		
		default:
			break;
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
	public static JSONObject convertStringOp(ScreenOption screenOption, String opKey, 
			String defaultLabel, HashMap<String,String> translations,
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.TEXT.name());
		objective.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			objective.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		objective.put(JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(JSON_FIXED_PROP_KEY, isFixed);
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
	
	public static JSONObject convertStringOp(MetadataFieldValue metadataValue, String opKey,
			String defaultLabel, HashMap<String,String> translations,
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.TEXT.name());
		
		objective.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			objective.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		objective.put(JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(JSON_FIXED_PROP_KEY, isFixed);
		if (metadataValue != null) {
			objective.put(JSON_ISVISIBILE_PROP_KEY, metadataValue.isVisible());
			if (!metadataValue.isVisible() && metadataValue.getStringValue() != null) {
				objective.put(JSON_DEFAULT_PROP_KEY, metadataValue.getStringValue());
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
	
	public static JSONObject convertEmployees(ScreenOption screenOption, boolean isRequired, 
			boolean isFixed, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.MULTI_CHOICE.name());
		optionType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_EmployeePageLabel);
		for (Entry<String,String> t : getTranslations(Messages.CtJsonExportUtils_EmployeePageLabel, "CtJsonExportUtils_EmployeePageLabel", ca).entrySet()) { //$NON-NLS-1$
			optionType.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		optionType.put(JSON_REQUIRED_PROP_KEY, isRequired);
		optionType.put(JSON_FIXED_PROP_KEY, isFixed);
		if (screenOption != null) {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible()) {
				if (screenOption.getUuidList() != null) {
					JSONArray defaultValues = new JSONArray();
					for (ScreenOptionUuid defaultOp : screenOption.getUuidList()) {
						defaultValues.add(UuidUtils.uuidToString(defaultOp.getUuidValue()));
					}
					optionType.put(JSON_DEFAULTS_PROP_KEY, defaultValues);
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
	
	public static JSONObject convertEmployees(MetadataFieldValue screenOption, boolean isRequired, 
			boolean isFixed, Session session, ConservationArea ca) {
		//find all employees - they are either directly linked or linked through a employee team
		Set<Employee> allEmployees = new HashSet<>();
		if (screenOption != null) {
			for (MetadataFieldUuidValue uuid : screenOption.getUuidList()) {
				UUID item = uuid.getUuidValue();
				Employee e = session.get(Employee.class, item);
				if (e != null) {
					allEmployees.add(e);
					continue;
				}
				EmployeeTeam team = session.get(EmployeeTeam.class, item);
				if (team != null) {
					for (EmployeeTeamMember tm : team.getMembers()) {
						allEmployees.add(tm.getEmployee());
					}
				}
			}
		}
		
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.MULTI_CHOICE.name());
		optionType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_EmployeePageLabel);
		for (Entry<String,String> t : getTranslations(Messages.CtJsonExportUtils_EmployeePageLabel, "CtJsonExportUtils_EmployeePageLabel", ca).entrySet()) { //$NON-NLS-1$
			optionType.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		optionType.put(JSON_REQUIRED_PROP_KEY, isRequired);
		optionType.put(JSON_FIXED_PROP_KEY, isFixed);
		if (screenOption != null) {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible()) {
				JSONArray defaultValues = new JSONArray();
				for (Employee e : allEmployees) {
					defaultValues.add(UuidUtils.uuidToString(e.getUuid()));
				}
				optionType.put(JSON_DEFAULTS_PROP_KEY, defaultValues);
			}
		}else {
			optionType.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		ArrayList<Employee> sortede = new ArrayList<>(allEmployees);
		sortede.sort( (a,b)-> Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a),SmartLabelProvider.getShortLabel(b)));

		JSONArray optionOptions = new JSONArray();
		for (Employee t : sortede) {
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
	
	public static JSONObject convertLeaderPilot(ScreenOption screenOption, String opKey, 
			String defaultLabel, HashMap<String,String> translations,  
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		objective.put(JSON_OPTION_PARENT_KEY, JSON_EMPLOYEE_METADATA_KEY);
//		objective.put(JSON_OPTION_LABEL_KEY, opLabel);
		objective.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			objective.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		objective.put(JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(JSON_FIXED_PROP_KEY, isFixed);
		if (screenOption != null) {
			objective.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible() && screenOption.getUuidValue() != null) {
				objective.put(JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(screenOption.getUuidValue()));
			}
		}else {
			objective.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(opKey, objective);
		return objectiveOp;
	}
	
	public static JSONObject convertLeaderPilot(MetadataFieldValue screenOption, String opKey, 
			String defaultLabel, HashMap<String,String> translations,   
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject objective = new JSONObject();
		objective.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		objective.put(JSON_OPTION_PARENT_KEY, JSON_EMPLOYEE_METADATA_KEY);
		
		objective.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			objective.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		objective.put(JSON_REQUIRED_PROP_KEY, isRequired);
		objective.put(JSON_FIXED_PROP_KEY, isFixed);
		if (screenOption != null) {
			objective.put(JSON_ISVISIBILE_PROP_KEY, screenOption.isVisible());
			if (!screenOption.isVisible() && screenOption.getUuidValue() != null) {
				objective.put(JSON_DEFAULT_PROP_KEY, UuidUtils.uuidToString(screenOption.getUuidValue()));
			}
		}else {
			objective.put(JSON_ISVISIBILE_PROP_KEY, true);
		}
		
		JSONObject objectiveOp = new JSONObject();
		objectiveOp.put(opKey, objective);
		return objectiveOp;
	}
	
	public static JSONObject convertKeyOptions(ScreenOption screenOption, 
			Class<? extends NamedKeyItem> clazz, String screenKey, 
			String defaultLabel, HashMap<String,String> translations,   
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
		optionType.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			optionType.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		optionType.put(JSON_REQUIRED_PROP_KEY, isRequired);
		optionType.put(JSON_FIXED_PROP_KEY, isFixed);
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
			ttype.put(JSON_PROP_UUID, UuidUtils.uuidToString(t.getUuid()));
			ttype.put(JSON_PROP_KEY, t.getKeyId());
			ttype.put(JSON_OPTION_LABEL_DEFAULT_KEY, t.findName(ca.getDefaultLanguage())); 
			for (Label l : t.getNames()) {
				ttype.put(JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue()); 
				
			}
			optionOptions.add(ttype);
		}
		optionType.put(JSON_OPTION_PROP_KEY, optionOptions);
		
		JSONObject teamTypeOp = new JSONObject();
		teamTypeOp.put(screenKey, optionType);
		return teamTypeOp;
	}
	
	public static JSONObject convertKeyOptions(MetadataFieldValue screenOption, 
			Class<? extends NamedKeyItem> clazz, String screenKey, 
			String defaultLabel, HashMap<String,String> translations, 
			boolean isRequired, boolean isFixed, Session session, ConservationArea ca) {
		JSONObject optionType = new JSONObject();
		optionType.put(JSON_OPTION_TYPE_KEY, Type.SINGLE_CHOICE.name());
//		optionType.put(JSON_OPTION_LABEL_KEY, opLabel);
		
		optionType.put(JSON_OPTION_LABEL_DEFAULT_KEY, defaultLabel);
		for (Entry<String,String> t : translations.entrySet()) {
			optionType.put(JSON_OPTION_LABEL_PREFIX_KEY + t.getKey(), t.getValue());
		}
		optionType.put(JSON_REQUIRED_PROP_KEY, isRequired);
		optionType.put(JSON_FIXED_PROP_KEY, isFixed);
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
			ttype.put(JSON_PROP_UUID, UuidUtils.uuidToString(t.getUuid()));
			ttype.put(JSON_PROP_KEY, t.getKeyId()); 
			ttype.put(JSON_OPTION_LABEL_DEFAULT_KEY, t.findName(ca.getDefaultLanguage()));
			for (Label l : t.getNames()) {
				ttype.put(JSON_OPTION_LABEL_PREFIX_KEY + l.getLanguage().getCode(), l.getValue());
				
			}
			optionOptions.add(ttype);
		}
		optionType.put(JSON_OPTION_PROP_KEY, optionOptions);
		
		JSONObject teamTypeOp = new JSONObject();
		teamTypeOp.put(screenKey, optionType);
		return teamTypeOp;
	}
	
	public static JSONObject createPatrolId() {
		JSONObject dataType = new JSONObject();
		dataType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_IdentifierLabel);
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.UUID.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, true);
		dataType.put(JSON_REQUIRED_PROP_KEY, true);
		JSONObject typeOp = new JSONObject();
		typeOp.put(ScreensUtil.RESULT_ID, dataType);
		return typeOp;
	}
	
	public static JSONObject createStartDate() {
		JSONObject dataType = new JSONObject();
		dataType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_StartDateLabel);
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.DATE.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, true);
		dataType.put(JSON_REQUIRED_PROP_KEY, true);
		JSONObject typeOp = new JSONObject();
		typeOp.put(ScreensUtil.RESULT_START_DATE, dataType);
		return typeOp;
	}
	
	
	public static JSONObject createStartTime() {
		JSONObject dataType = new JSONObject();
		dataType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_StartTimeLabel);
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.TIME.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, true);
		dataType.put(JSON_REQUIRED_PROP_KEY, true);
		JSONObject typeOp = new JSONObject();
		typeOp.put(ScreensUtil.RESULT_START_TIME, dataType);
		return typeOp;
	}
	
	public static JSONObject createDataType(String outputDataType) {
		JSONObject dataType = new JSONObject();
		dataType.put(JSON_OPTION_LABEL_DEFAULT_KEY, Messages.CtJsonExportUtils_DataTypeLabel);
		dataType.put(CtJsonExportUtils.JSON_OPTION_TYPE_KEY, CtJsonExportUtils.Type.TEXT.name());
		dataType.put(CtJsonExportUtils.JSON_ISVISIBILE_PROP_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_OPTION_GENERATED_KEY, false);
		dataType.put(CtJsonExportUtils.JSON_DEFAULT_PROP_KEY, outputDataType);
		dataType.put(JSON_REQUIRED_PROP_KEY, true);
		JSONObject typeOp = new JSONObject();
		typeOp.put(ScreensUtil.RESULT_DATATYPE, dataType);
		return typeOp;
	}
	
	public static List<Path> addHelpFiles(ConfigurableModel model, org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xmlModel, Path targetDir) throws IOException {
		//create html help files 
		List<NodeType> xmlnodes = new ArrayList<>();
		xmlnodes.addAll(xmlModel.getNodes().getNode());
		List<Path> filesToAdd = new ArrayList<>();
		
		while (!xmlnodes.isEmpty()) {
			NodeType l = xmlnodes.remove(0);
			xmlnodes.addAll(l.getNode());

			for (org.wcs.smart.dataentry.model.xml.generated.AttributeType at : l.getAttribute()) {

				String imageLocation = HelpImageLocation.BEFORE.name();
				String helpText = null;
				String imageFormat = null;
				UUID uuid = UuidUtils.stringToUuid(at.getId());

				for (AttributeOptionType op : at.getOption()) {
					if (op.getId().equals(CmAttributeOption.ID_HELP_IMAGE_FORMAT))
						imageFormat = op.getStringValue();
					if (op.getId().equals(CmAttributeOption.ID_HELP_IMAGE_LOCATION))
						imageLocation = op.getStringValue();
					if (op.getId().equals(CmAttributeOption.ID_HELP_TEXT))
						helpText = op.getStringValue();
				}
				if (helpText == null && imageFormat == null)
					continue;

				if ((helpText != null && !helpText.strip().isEmpty()) || imageFormat != null) {
					CmAttribute temp = new CmAttribute();
					temp.setUuid(uuid);
					temp.setHelpFormat(imageFormat);
					temp.setHelpText(helpText);
					temp.setHelpImageLocation(HelpImageLocation.valueOf(imageLocation));

					String html = temp.getHelpTextAsHtml(false);
					if (html == null || html.isEmpty())
						continue;

					Path p = targetDir.resolve(Paths.get("cm_help_" + UuidUtils.uuidToString(temp.getUuid()) + ".html")); //$NON-NLS-1$ //$NON-NLS-2$
					Files.writeString(p, html);
					filesToAdd.add(p);
					
					
					Path imgFile = null;
					if (temp.getHelpFormat() != null) {
						if (temp.getImportHelpFile() != null) {
							imgFile = temp.getImportHelpFile();
						}else {
							imgFile = model.getFileDataStoreLocation().resolve(temp.getHelpImage());
						}
					}
					if (imgFile != null) {
						Path target = targetDir.resolve(imgFile.getFileName());
						Files.copy(imgFile, target);
						filesToAdd.add(target);
					}

					AttributeOptionType op = new AttributeOptionType();
					op.setId("HELP_HTML_FILE"); //$NON-NLS-1$
					op.setStringValue(p.getFileName().toString());
					at.getOption().add(op);

				}
			}
		}
		return filesToAdd;
	}
}

