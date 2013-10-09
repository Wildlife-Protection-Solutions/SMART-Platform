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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.GPSDataImport.ImportType;
import org.wcs.smart.patrol.internal.ui.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;

/**
 * Import engine for importing data from a GPS device
 * @author Emily
 *
 */
public class GpsImportEngine implements IImportEngine{

	public String deviceType = null;
	
	@Override
	public String getName() {
		return Messages.ImportWpTypeWizardPage_GPSOp;
	}

	/**
	 * Supports both tracks and waypoints
	 */
	@Override
	public boolean supportsType(ImportType type) {
		return type == ImportType.TRACK || type == ImportType.WAYPOINT;
	}

	/**
	 * Reads waypoints from gps device
	 */
	@Override
	public List<Waypoint> getWaypoints(ImportOption options, ImportType type, Date currentDate,
			IProgressMonitor monitor) throws Exception {
		
		Date day = null;
		if (options == ImportOption.DATE){
			day = currentDate;
		}
		
		List<Waypoint> waypoints = GPSDataImport.importGpsData(deviceType, day, Collections.singleton(type), monitor).get(type);
		return waypoints;
	}
		
	/**
	 * GPS device type
	 * @param deviceType
	 */
	public void setDeviceType(String deviceType){
		this.deviceType = deviceType;
	}

	@Override
	public IImportWizardPage getFirstWizardPage(ImportGpsDataWizard wizard) {
		return new ImportGPSWizardPage(wizard);
	}

	/**
	 * Updates patrol and saves the results to the database
	 */
	@Override
	public String updatePatrol(ImportOption option, ImportType type,
			Patrol patrol, PatrolLegDay currentLeg, List<Waypoint> waypoints,
			IProgressMonitor monitor) throws Exception {

		String message = null;
		if(type == ImportType.WAYPOINT){
			message = GPSDataImport.saveWaypoints(option, patrol, currentLeg, waypoints);
		}else if (type == ImportType.TRACK){
			HashMap<PatrolLegDay, Track> tracks  = new HashMap<PatrolLegDay,Track>();
			if (option == ImportOption.ALL){
				tracks = GPSDataImport.convertTracks(waypoints, patrol.getLegs());
				message = MessageFormat.format(Messages.GpsImportEngine_MultiTrackImport, new Object[]{tracks.size()});
			}else{
				Track track = GPSDataImport.convertToTrack(waypoints, currentLeg.getDate());
				tracks.put(currentLeg, track);
				message = Messages.GpsImportEngine_SingleImport;
			}
			GPSDataImport.saveTracks(tracks);
		}
		return message;
	}
}

