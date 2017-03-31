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
package org.wcs.smart.cybertracker.survey.export;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.survey.export.SurveyScreensUtil.JsonSurveyKey;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey;
import org.wcs.smart.cybertracker.survey.model.CyberTrackerSurvey.SurveyMeta;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

/**
 * Parses patrol metadata values from json strings 
 * @author Emily
 *
 */
public class SurveyJsonUtils {
	
	/**
	 * 
	 * @param jsonDefaults default values 
	 * @param jsonValues actual selected values
	 * @param session
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static CyberTrackerSurvey  parseSurveyMetadata(JSONObject jsonDefaults, JSONObject jsonValues, Session session){
		
		if (jsonValues == null) jsonValues = new JSONObject();
		
		SurveyDesign design = null;
		String surveyDesign = (String) jsonValues.get(SurveyScreensUtil.RESULT_SURVEY_DESIGN);
		List<SurveyDesign> cas = session.createCriteria(SurveyDesign.class)
				.add(Restrictions.eq("keyId", surveyDesign)) //$NON-NLS-1$
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list(); //$NON-NLS-1$
		if (cas.size() == 1){
			design = cas.get(0);
		}
		
		String comment = (String)jsonValues.get(SurveyScreensUtil.RESULT_MISSION_COMMENTS);
		if (comment == null){
			comment = (String)jsonDefaults.get(SurveyScreensUtil.RESULT_MISSION_COMMENTS);
		}
		String leader = (String)jsonValues.get(SurveyScreensUtil.RESULT_MISSION_LEADER);
		if (leader == null){
			leader = (String)jsonDefaults.get(SurveyScreensUtil.RESULT_MISSION_LEADER);
		}
	
		List<String> members = new ArrayList<String>();
		for (Object x : jsonValues.keySet()){
			String key = (String)x;
			if (startsWith(key, JsonKey.EMPLOYEE.key)) members.add(key);
		}
		if( members.isEmpty()){
			for (Object x : jsonDefaults.keySet()){
				String key = (String)x;
				if (startsWith(key, JsonKey.EMPLOYEE.key)) members.add(key);
			}	
		}
		
		SamplingUnit startSu = null;
		String su = (String)jsonValues.get(SurveyScreensUtil.RESULT_MISSION_START_SAMPLING_UNIT);
		if (su != null){
			if (startsWith(su, JsonSurveyKey.SAMPLING_UNIT.key)){
				String suUuid = su.substring(JsonSurveyKey.SAMPLING_UNIT.key.length() + 1);
				if (suUuid != null&& !suUuid.equalsIgnoreCase("null")){ //$NON-NLS-1$
					SamplingUnit suu = (SamplingUnit) session.get(SamplingUnit.class, UuidUtils.stringToUuid(suUuid));
					if (suu != null){
						startSu = suu;
					}
				}
			}
		}
		
		CyberTrackerSurvey ctMission = new CyberTrackerSurvey(null, null);
		ctMission.setSurveyDesign(design);
		ctMission.setComment(comment);
		ctMission.setStartSamplingUnit(startSu);
		ctMission.setMembers(new ArrayList<Employee>());
		for (String member: members){
			UUID uuid = UuidUtils.stringToUuid(member.substring(JsonKey.EMPLOYEE.key.length() + 1));
			Employee employee = (Employee) session.get(Employee.class, uuid);
			if (employee != null){
				ctMission.getMembers().add(employee);
			}else{
				ctMission.addWarning(SurveyMeta.MEMBERS, Messages.SurveyJsonUtils_MemberNotFound);
			}
			
			if (member.equals(leader)){
				ctMission.setLeader(employee);
			}
		}
		
	
		return ctMission;
	}

	private static boolean startsWith(String value, String key){
		return value.startsWith(key + CyberTrackerConfExporter.KEY_SEP);
	}
}
