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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.GpxImportEngine;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;

/**
 * GpxImportEngine for mission
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionGpxImportEngine extends GpxImportEngine {

	private Date date;
	
	public MissionGpxImportEngine() {
		//empty
	}
	
	public MissionGpxImportEngine(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> waypoints, IProgressMonitor monitor)
			throws Exception {
		Mission mission = (Mission) object;
		String message = null;
		if (type == ImportType.WAYPOINT) {
			message = MissionDataImport.saveWaypoints(option, mission, date, waypoints);
		} else if (type == ImportType.TRACK) {
			List<MissionTrack> tracks  = new ArrayList<MissionTrack>();
			if (option == ImportOption.ALL) {
				tracks = MissionDataImport.convertTracks(waypoints, mission);
				message = MessageFormat.format(Messages.MissionImportEngine_ImportMultiTrack, new Object[]{tracks.size()});
			}else if (option == ImportOption.DATE){
				List<MissionTrack> track = MissionDataImport.convertToTrack(waypoints, true);
				for (MissionTrack t : track){
					t.setDate(date);
					tracks.add(t);
				}
				message = MessageFormat.format(Messages.MissionImportEngine_ImportMultiTrack, new Object[]{tracks.size()});
			} else {
				MissionTrack track = MissionDataImport.convertToTrack(waypoints, false).get(0);
				track.setDate(date);
				tracks.add(track);
				message = Messages.MissionImportEngine_ImportSingleTrack;
			}
			
			MissionDataImport.saveTracks(mission, tracks);
		}
		return message;
	}
	
}
