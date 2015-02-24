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
package org.wcs.smart.patrol.internal.ui.importwp;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.IImportEngine;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;

/**
 * Wizard for importing data from GPS Device
 * @author Emily
 * @author elitvin
 * @since 3.0.0
 */
public class PatrolImportGpsDataWizard extends ImportGpsDataWizard {

	private static final String GPX_FILE_ERROR = Messages.ImportGpsDataWizard_GPXFileImportError;
	private static final String IMPORT_DIALOG_TITLE = Messages.ImportGpsDataWizard_ImportDialog_Title;
	
	private PatrolLegDay currentDay;
	
	public PatrolImportGpsDataWizard(PatrolLegDay currentDay, ImportType type) {
		super(type);
		setDateOption(currentDay.getDate());
		this.currentDay = currentDay;
	}

	public PatrolLegDay getCurrentDay() {
		return currentDay;
	}
	
	@Override
	public IImportEngine[] getEngines() {
		IImportEngine[] engines = {new PatrolGpsImportEngine(),  new PatrolGpxImportEngine(), new PatrolCsvImportEngine(), new PatrolTrackFromWaypointEngine()}; 
		return engines;
	}

	@Override
	public boolean processFinish() {
		final ImportType type = getType();
		final ImportOption currentOption = getImportOption();
		if (type == ImportType.TRACK){
			if (currentOption == ImportOption.DATE || currentOption == ImportOption.SELECT){
				if (currentDay.getTrack() != null){
					//warn user
					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.PatrolLegDayInputComposite_SetTrackDialog_Message)){
						return false;
					}
				}
			}
			if (currentOption == ImportOption.ALL){
				boolean warn = false;
				for(PatrolLeg l : currentDay.getPatrolLeg().getPatrol().getLegs()){
					for(PatrolLegDay d : l.getPatrolLegDays()){
						if (d.getTrack() != null){
							warn = true;
							break;
						}
					}
					if (warn) break;
				}
				
				if (warn){
					//warn user
					if (!MessageDialog.openConfirm(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_TrackWarningOverwriteNew)){
						return false;
					}
				}
			}
		}
		
		final String[] successMessage =  new String[]{null};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					IImportEngine engine = getImportEngine();
					try {
						if (getImportOption() == ImportOption.DATE || getImportOption() == ImportOption.ALL) {
							List<Waypoint> waypoints = engine.getWaypoints(getImportOption(), type, getDateOption(), monitor);
							setImportedData(waypoints);
						}
						
						if (getImportedData() != null && getImportedData().size() == 0){
							//nothing found
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									MessageDialog.openWarning(getShell(), IMPORT_DIALOG_TITLE, MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new  Object[]{getType().guiName, getType().guiName}));
								}});
							return;
						}
						
						String message = engine.updateSourceObject(getImportOption(), getType(), currentDay, getImportedData(), monitor);
						successMessage[0] = message;
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPatrolPlugIn.displayLog(e.getMessage(), e);
							}
							
						});
						successMessage[0] = null;
					}
				}
			});
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(GPX_FILE_ERROR + ex.getLocalizedMessage(), ex);
			return false;
		}
		if(successMessage[0] != null){
			MessageDialog.openInformation(getShell(), IMPORT_DIALOG_TITLE, Messages.ImportGpsDataWizard_ImportCompleteMessage + " " + successMessage[0]); //$NON-NLS-1$
			return true;
		}else{
			return false;
		}
	}

}
