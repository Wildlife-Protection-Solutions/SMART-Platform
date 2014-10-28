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
package org.wcs.smart.er.ui.mision.importwp;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.IImportEngine;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;

/**
 * ImportDataWizard for mission waypoints and tracks
 * @author elitvin
 * @since 3.0.0
 */
public class MissionImportGpsDataWizard extends ImportGpsDataWizard {

	private MissionDay missionDay;
	
	public MissionImportGpsDataWizard(MissionDay missionDay, ImportType type) {
		super(type);
		this.missionDay = missionDay;
	}

	@Override
	public IImportEngine[] getEngines() {
		return new IImportEngine[]{new MissionGpsImportEngine(missionDay),  
				new MissionGpxImportEngine(missionDay), 
				new MissionCsvImportEngine(missionDay),
				new MissionTrackFromWaypointEngine(missionDay)};
	}

	@Override
	public boolean processFinish() {
		final ImportType type = getType();
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
									MessageDialog.openWarning(getShell(), Messages.MissionImportGpsDataWizard_DialogTitle, MessageFormat.format(Messages.MissionImportGpsDataWizard_NoData, new  Object[]{getType().guiName, getType().guiName}));
								}});
							return;
						}
						
						String message = engine.updateSourceObject(getImportOption(), getType(), missionDay, getImportedData(), monitor);
						successMessage[0] = message;
					} catch (final Exception e) {
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), e.getMessage(), e);
							}
							
						});
						successMessage[0] = null;
					}
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.MissionImportGpsDataWizard_ImportError + ex.getLocalizedMessage(), ex);
			return false;
		}
		if (successMessage[0] != null) {
			MessageDialog.openInformation(getShell(), Messages.MissionImportGpsDataWizard_DialogTitle, Messages.MissionImportGpsDataWizard_SuccessMessage + " " + successMessage[0]); //$NON-NLS-1$
			return true;
		}else{
			return false;
		}
	}

}
