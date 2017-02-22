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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.GpxImportEngine;
import org.wcs.smart.observation.common.importwp.IImportWizardPage;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.ImportGpxWizardPage;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;

/**
 * Import engine for gpx files
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class PatrolGpxImportEngine extends GpxImportEngine {

	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> waypoints, IProgressMonitor monitor)
			throws Exception {
		PatrolLegDay currentLeg = (PatrolLegDay) object;
		Patrol patrol = currentLeg.getPatrolLeg().getPatrol();
		String message = null;
		if(type == ImportType.WAYPOINT){
			message = PatrolGPSDataImport.saveWaypoints(option, patrol, currentLeg, waypoints);
		}else if (type == ImportType.TRACK){
			HashMap<PatrolLegDay, Track> tracks  = new HashMap<PatrolLegDay,Track>();
			if (option == ImportOption.ALL){
				tracks = PatrolGPSDataImport.convertTracks(waypoints, patrol.getLegs());
				message = MessageFormat.format(Messages.GpxImportEngine_ImportMultiTrack, new Object[]{tracks.size()});
				if (tracks.size() == 0){
					message += "\n" + Messages.PatrolGpxImportEngine_NotracksFound; //$NON-NLS-1$
				}
			}else{
				Track track = PatrolGPSDataImport.convertToTrack(waypoints);
				tracks.put(currentLeg, track);
				message = Messages.GpxImportEngine_ImportSingleTrack;
			}
			PatrolGPSDataImport.saveTracks(tracks);
		}
		return message;
	}

	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		ImportGpxWizardPage page = new ImportGpxWizardPage(wizard);
		PatrolImportGpsDataWizard w = (PatrolImportGpsDataWizard) wizard;
		PatrolOptionsLabelUtil.updateLabels(page, w);
		return page;
	}
	
}
