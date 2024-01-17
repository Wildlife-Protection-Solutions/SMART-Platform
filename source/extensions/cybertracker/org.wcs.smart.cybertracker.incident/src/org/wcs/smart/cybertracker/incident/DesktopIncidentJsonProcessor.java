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
package org.wcs.smart.cybertracker.incident;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.importer.json.IDesktopJsonProcessor;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Parser for parsing patrol data from CT JSON data. 
 * 
 * @author Emily
 *
 */
public class DesktopIncidentJsonProcessor extends IncidentJsonProcessor implements IDesktopJsonProcessor {
	
	public DesktopIncidentJsonProcessor() {
		super(SmartDB.getCurrentConservationArea());
	}

	

	@Override
	public void afterSave(){
		for (Waypoint p : modifiedIncidents){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.IncidentJsonProcessor_NotificationError, ex.getMessage(), ex);
			}
		}
		for (Waypoint p : newIncidents){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_ADDED, p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError(Messages.IncidentJsonProcessor_NotificationError2, ex.getMessage(), ex);
			}
		}
	}

	@Override
	protected void logException(String message, Exception ex) {
		CyberTrackerPlugIn.log(message, ex);
		
	}

	@Override
	protected void processWarnings(List<JsonImportWarning> warnings) throws UserCancelledException {
		displayWarnings(warnings);
		
	}
	
	/*
	 * displays warning dialog to user allowing them to cancel the processing
	 */
	private void displayWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		 if (!warnings.isEmpty()){
			 List<String> allWarnings = warnings.stream().map(e->e.getMessage(Locale.getDefault())).toList();
			 
			 	final boolean[] cont = {false};
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
								Messages.IncidentJsonProcessor_WaringsTitle, 
								Messages.IncidentJsonProcessor_WarningsMessage,
								allWarnings,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}	
				});
				if (!cont[0]){
					throw new UserCancelledException(Messages.IncidentJsonProcessor_CanceledMsg);
				}
		 }
	}
	
}