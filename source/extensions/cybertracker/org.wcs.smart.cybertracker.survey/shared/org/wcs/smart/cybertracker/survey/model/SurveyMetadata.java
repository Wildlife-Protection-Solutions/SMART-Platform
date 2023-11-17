package org.wcs.smart.cybertracker.survey.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.survey.json.JsonMission;
import org.wcs.smart.cybertracker.survey.json.MissionJsonImportWarning;
import org.wcs.smart.cybertracker.survey.json.MissionJsonImportWarning.WarningType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

public class SurveyMetadata {
	
	public static final String DATATYPE_SURVEY = "survey"; //$NON-NLS-1$

	public static enum JsonSurveyKey {
		MISSION_ATT_LIST("ml"), //$NON-NLS-1$
		MISSION_ATT("ma"), //$NON-NLS-1$
		SAMPLING_UNIT("su"); //$NON-NLS-1$
		
		public String key;
		
		private JsonSurveyKey(String key){
			this.key = key;
		}
	}
	
	public static enum JsonKey{
		SURVEY_DESIGN("SMART_SurveyDesign"), //$NON-NLS-1$
		MISSION_LEADER("SMART_Leader"), //$NON-NLS-1$
		MISSION_COMMENTS("SMART_Comments"), //$NON-NLS-1$
		MISSION_START_SAMPLING_UNIT("SMART_StartSamplingUnit"), //$NON-NLS-1$
		MISSION_SAMPLING_UNIT("SMART_SamplingUnit"), //$NON-NLS-1$
		MISSION_PROPERTY_PREFIX("SMART_MP_"), //$NON-NLS-1$
		END_MISSION_KEY("SMART_EndMission"); //$NON-NLS-1$
		
		public String key;
		
		private JsonKey(String key){
			this.key = key;
		}
	}
	
	
	
	
	/**
	 * Parses mission metadata from json data (CyberTracker json format)
	 * @param jsonDefaults default values 
	 * @param jsonValues actual selected values
	 * @param session
	 * @return
	 */
	public static JsonMission parseMissionMetadata(JSONObject jsonDefaults, 
			JSONObject jsonValues, ConservationArea ca, Session session){
		
		if (jsonValues == null) jsonValues = new JSONObject();
		
		SurveyDesign design = null;
		String surveyDesign = (String) jsonValues.get(SurveyMetadata.JsonKey.SURVEY_DESIGN.key);
		List<SurveyDesign> cas = QueryFactory.buildQuery(session, SurveyDesign.class,  
				new Object[] {"keyId", surveyDesign}, //$NON-NLS-1$
				new Object[] {"conservationArea", ca}).getResultList(); //$NON-NLS-1$
				
		if (cas.size() == 1){
			design = cas.get(0);
		}
		
		String comment = (String)jsonValues.get(SurveyMetadata.JsonKey.MISSION_COMMENTS.key);
		if (comment == null){
			comment = (String)jsonDefaults.get(SurveyMetadata.JsonKey.MISSION_COMMENTS.key);
		}
		String leader = (String)jsonValues.get(SurveyMetadata.JsonKey.MISSION_LEADER.key);
		if (leader == null){
			leader = (String)jsonDefaults.get(SurveyMetadata.JsonKey.MISSION_LEADER.key);
		}
	
		List<String> members = new ArrayList<String>();
		for (Object x : jsonValues.keySet()){
			String key = (String)x;
			if (startsWith(key, CtJsonUtil.JsonDataModelKey.EMPLOYEE.key)) members.add(key);
		}
		if( members.isEmpty()){
			for (Object x : jsonDefaults.keySet()){
				String key = (String)x;
				if (startsWith(key, CtJsonUtil.JsonDataModelKey.EMPLOYEE.key)) members.add(key);
			}	
		}
		
		SamplingUnit startSu = null;
		String su = (String)jsonValues.get(SurveyMetadata.JsonKey.MISSION_START_SAMPLING_UNIT.key);
		if (su != null){
			
			if (startsWith(su, SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key)){
				String suUuid = su.substring(SurveyMetadata.JsonSurveyKey.SAMPLING_UNIT.key.length() + 1);
				if (suUuid != null&& !suUuid.equalsIgnoreCase("null")){ //$NON-NLS-1$
					SamplingUnit suu = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(suUuid));
					if (suu != null){
						startSu = suu;
					}
				}
			}
		}
		
		JsonMission ctMission = new JsonMission();
		ctMission.setSurveyDesign(design, surveyDesign);
		ctMission.setComment(comment);
		ctMission.setStartSamplingUnit(startSu);
		ctMission.setMembers(new ArrayList<Employee>());
		for (String member: members){
			UUID uuid = UuidUtils.stringToUuid(member.substring(CtJsonUtil.JsonDataModelKey.EMPLOYEE.key.length() + 1));
			Employee employee = (Employee) session.get(Employee.class, uuid);
			if (employee != null){
				ctMission.getMembers().add(employee);
			}else{
				ctMission.addWarning(new MissionJsonImportWarning(WarningType.MEMBER_NOT_FOUND, member));
			}
			
			if (member.equals(leader)){
				ctMission.setLeader(employee);
			}
		}
		
	
		return ctMission;
	}

	private static boolean startsWith(String value, String key){
		return value.startsWith(key + CtJsonUtil.KEY_SEP);
	}
	
}
