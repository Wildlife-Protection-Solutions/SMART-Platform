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
package org.wcs.smart.er;

import java.text.MessageFormat;
import java.util.Date;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationOptions;

/**
 * Permission manager for surveys.
 * 
 * @author Emily
 *
 */
public class SurveyPermissionManager {

	public static final SurveyPermissionManager INSTANCE = new SurveyPermissionManager();
	
	private SurveyPermissionManager(){
		
	}
	
	/**
	 * 
	 * @return error string if the current user cannot delete a survey design
	 */
	public String canDeleteSurveyDesign(){
		SmartUserLevel level = SmartDB.getCurrentEmployee().getSmartUserLevel();
		if (level == SmartUserLevel.DATA_ENTRY ||
			level == SmartUserLevel.ANALYST){
			return Messages.SurveyPermissionManager_InsufficientPrivileges;
		}
		return null;		
	}
	
	/**
	 * Determines if the given survey and be modified or deleted.  Returns
	 * null if can be deleted or modified otherwise it returns a string
	 * describing why it cannot be deleted.
	 * 
	 * @param survey
	 * @param op
	 * @return
	 */
	public String canEditSurvey(Survey survey, ObservationOptions op){
		SmartUserLevel level = SmartDB.getCurrentEmployee().getSmartUserLevel();
		
		if (level == SmartUserLevel.MANAGER ||
				level == SmartUserLevel.ADMIN){
			return null;
		}
		if (level == SmartUserLevel.DATA_ENTRY ||
				level == SmartUserLevel.ANALYST){
			if (op.getEditTime() == null || op.getEditTime() < 0){
				return null;
			}else if (survey.getStartDate() == null){
				return null;
			}else if (level == Employee.SmartUserLevel.DATA_ENTRY || 
					level == Employee.SmartUserLevel.ANALYST){
				Date d = new Date();
				d.setTime( d.getTime() - (long)op.getEditTime() * 24 * 60 * 60 * 1000 );
				if (survey.getStartDate().after(d)){
					return null;
				}else{
					return MessageFormat.format(Messages.SurveyPermissionManager_SurveyToOld, new Object[]{op.getEditTime()}) ;
				}
			}else{
				return null;
			}
		}
		return Messages.SurveyPermissionManager_InvalidUserType;
	}

	/**
	 * Determines if the current user can edit the given mission.  Will
	 * return null if can edit otherwise it will return a string
	 * describing why the mission cannot be modified.
	 * 
	 * @param mission
	 * @param op
	 * @return
	 */
	public String canEditMission(Mission mission, ObservationOptions op){
		SmartUserLevel level = SmartDB.getCurrentEmployee().getSmartUserLevel();
		if (level == SmartUserLevel.MANAGER ||
				level == SmartUserLevel.ADMIN){
			return null;
		}
		if (level == SmartUserLevel.DATA_ENTRY || level == SmartUserLevel.ANALYST){
			if (op.getEditTime() == null || op.getEditTime() < 0){
				return null;
			}else if (mission.getStartDate() == null){
				return null;
			}else if (level == Employee.SmartUserLevel.DATA_ENTRY ||
					level == Employee.SmartUserLevel.ANALYST){
				Date d = new Date();
				d.setTime( d.getTime() - (long)op.getEditTime() * 24 * 60 * 60 * 1000 );
				if (mission.getStartDate().after(d)){
					return null;
				}else{
					return MessageFormat.format(Messages.SurveyPermissionManager_MissionToOld, new Object[]{op.getEditTime()}) ;
				}
			}else{
				return null;
			}
		}
		return Messages.SurveyPermissionManager_InvalidUserType;
	}
	
	/**
	 * Determines if the current user can edit any survey designs.
	 * If can edit it will return null, otherwise it will return a string
	 * describing why the survey design cannot be modified.
	 * 
	 * @param mission
	 * @param op
	 * @return
	 */
	public String canEditSurveyDesign(){
		SmartUserLevel level = SmartDB.getCurrentEmployee().getSmartUserLevel();
		if (level == SmartUserLevel.MANAGER ||
				level == SmartUserLevel.ADMIN){
			return null;
		}
		if (level == SmartUserLevel.ANALYST || 
				level == SmartUserLevel.DATA_ENTRY){
			return Messages.SurveyPermissionManager_InsufficientPrivileges;
		}
		return Messages.SurveyPermissionManager_InvalidUserType;
	}
	
	/**
	 * Determines if the current user can
	 * create a new survey design.  If can create
	 * it will return null, otherwise it will return 
	 * a description explaining why cannot be created.
	 * 
	 * @return
	 */
	public String canCreateSurveyDesign(){
		return canEditSurveyDesign();
	}
	
	
	/**
	 * 
	 * @return true if the current user can create/edit new surveys and mission
	 */
	public boolean canEditMissionSurvery(){
		SmartUserLevel level = SmartDB.getCurrentEmployee().getSmartUserLevel();
		if (level == SmartUserLevel.MANAGER ||
				level == SmartUserLevel.ADMIN || 
				level == SmartUserLevel.DATA_ENTRY ||
				level == SmartUserLevel.ANALYST){
			return true;
		}
		return false;
	}
}
