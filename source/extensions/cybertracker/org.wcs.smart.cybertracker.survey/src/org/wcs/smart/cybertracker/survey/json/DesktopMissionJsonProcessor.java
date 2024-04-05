/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.survey.json;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.importer.json.IDesktopJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public class DesktopMissionJsonProcessor extends MissionJsonProcessor implements IDesktopJsonProcessor {
	
	public DesktopMissionJsonProcessor() {
		super(SmartDB.getCurrentConservationArea());
	}
	
	@Override
	public void afterSave(){
		for (Mission p : modifiedMissions){
			try{
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.MissionJsonProcessor_ErrorTitle, ex.getMessage(), ex);
			}
		}
		for (Mission p : newMissions){
			try{
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_ADDED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.MissionJsonProcessor_ErrorTitle, ex.getMessage(), ex);
			}
		}
	}

	@Override
	protected void logException(String message, Exception ex) {
		CyberTrackerPlugIn.log(message, ex);
	}


	@Override
	protected void processMissionWarnings(List<JsonImportWarning> warnings) throws UserCancelledException {
		displayWarnings(warnings);
		
	}


	@Override
	protected void processTrackWarnings(List<JsonImportWarning> warnings) throws UserCancelledException {
		displayWarnings(warnings);
	}
	
	/*
	 * displays warning dialog to user allowing them to cancel the processing
	 */
	private void displayWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		 if (!warnings.isEmpty()){
			 	final boolean[] cont = {false};
			 	List<String> all = warnings.stream().map(e->e.getMessage(Locale.getDefault())).toList();
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
								Messages.MissionJsonProcessor_WarningTitle, 
								Messages.MissionJsonProcessor_WarningMsg,
								all,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}	
				});
				if (!cont[0]){
					throw new UserCancelledException(Messages.MissionJsonProcessor_UserCancelled2);
				}
		 }
	}
	
	/**
	 * Should throw exception if processing should stop 
	 * 
	 * @throws UserCancelledException
	 */
	@Override
	protected void assignMissions(Session session) throws UserCancelledException{
		final boolean[] cancel = new boolean[]{false};
		//we need to ask the user if they want to create a new patrol or add to an existing patrol
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				try{
					MissionDialog pd = new MissionDialog(Display.getDefault().getActiveShell(),
							newMissionLinks, session);
					if (pd.open() == Window.CANCEL){
						cancel[0] = true;
					}else{
						modifiedMissions.addAll(pd.getMergedMissions());
						newMissions = pd.getNewMissions();
					}
				}catch (Exception ex){
					CyberTrackerPlugIn.displayError("Error", "Error occured while assigning missions to surveys: " + ex.getMessage(), ex);
					cancel[0] = true;
				}
			}	
		});
		if (cancel[0]) throw new UserCancelledException("User cancelled operation while assigning missions to surveys."); 
	}
		
}
