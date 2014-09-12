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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.IImportWizardPage;
import org.wcs.smart.observation.common.importwp.ImportFromWaypointWizardPage;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.TrackFromWaypointEngine;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;

/**
 * Import engine for generating tracks
 * from waypoints.
 * 
 * @author Emily
 *
 */
public class PatrolTrackFromWaypointEngine extends TrackFromWaypointEngine {

	/**
	 * updates the patrol tracks, computing tracks
	 * from waypoints
	 */
	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> data, IProgressMonitor monitor)
			throws Exception {
		PatrolLegDay currentLeg = (PatrolLegDay) object;
		Patrol patrol = currentLeg.getPatrolLeg().getPatrol();
		HashMap<PatrolLegDay, Track> tracks = new HashMap<PatrolLegDay, Track>();;
		if (option == ImportOption.ALL){
			tracks = PatrolGPSDataImport.computeTracksFromWaypoints(patrol.getLegs());
		}else{
			Track t = PatrolGPSDataImport.computeTrackFromWaypoints(currentLeg);
			if (t == null){
				return Messages.PatrolLegDayInputComposite_ImportTrackError_DialogMessage;
			}else{
				tracks.put(currentLeg, t);
			}
			
		}
		PatrolGPSDataImport.saveTracks(tracks);
		return MessageFormat.format(Messages.TrackFromWaypointEngine_GeneratedTracks, new Object[]{tracks.size()});
	}

	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		ImportFromWaypointWizardPage page = new ImportFromWaypointWizardPage(wizard);
		PatrolImportGpsDataWizard w = (PatrolImportGpsDataWizard) wizard;
		if (w.getCurrentDay().getPatrolLeg().getPatrol().getLegs().size() > 1) {
			String op = DateFormat.getDateInstance(DateFormat.MEDIUM).format(w.getCurrentDay().getDate());
			op += " (" + Messages.ImportOptionsComposite_LegPrefix + ": " + w.getCurrentDay().getPatrolLeg().getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String[] lblOptions = new String[] {
					Messages.ImportFromWaypointWizardPage_OpGenerateAllTracks,
					MessageFormat.format(Messages.ImportFromWaypointWizardPage_OpGenerateDayTracks1, new Object[]{w.getType().guiName.toLowerCase(), op})
				};

			page.setOptions(new ImportOption[]{ImportOption.ALL, ImportOption.DATE}, lblOptions);
		}
		return page;
	}
	
}
