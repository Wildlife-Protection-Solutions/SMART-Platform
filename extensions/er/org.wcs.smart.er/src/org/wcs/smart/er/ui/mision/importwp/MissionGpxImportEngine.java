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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionDay;
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

	private MissionDay missionDay;
	
	public MissionGpxImportEngine() {
		//empty
	}
	
	public MissionGpxImportEngine(MissionDay missionDay) {
		this.missionDay = missionDay;
	}

	public MissionDay getDate() {
		return missionDay;
	}
	public void setDate(MissionDay missionDay) {
		this.missionDay = missionDay;
	}
	
	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> waypoints, IProgressMonitor monitor)
			throws Exception {
		String message = null;
		MissionDay missionDay = (MissionDay) object;
		if (type == ImportType.WAYPOINT) {
			message = MissionDataImport.saveWaypoints(option, missionDay.getMission(), missionDay, waypoints);
		} else if (type == ImportType.TRACK) {
			HashMap<MissionDay, List<MissionTrack>> tracks = new HashMap<MissionDay, List<MissionTrack>>();
			if (option == ImportOption.ALL) {
				tracks = MissionDataImport.convertTracks(waypoints, missionDay.getMission().getMissionDays());
				int cnt = 0;
				for (List<MissionTrack> mt: tracks.values()){
					cnt += mt.size();
				}
				message = MessageFormat.format(Messages.MissionImportEngine_ImportMultiTrack, new Object[]{cnt});
			}else if (option == ImportOption.DATE){
				List<MissionTrack> track = MissionDataImport.convertToTrack(waypoints, true);
				for (MissionTrack t : track){
					t.setMissionDay(missionDay);
				}
				tracks.put(missionDay, track);
				message = MessageFormat.format(Messages.MissionImportEngine_ImportMultiTrack, new Object[]{track.size()});
			} else {
				MissionTrack track = MissionDataImport.convertToTrack(waypoints, false).get(0);
				track.setMissionDay(missionDay);
				tracks.put(missionDay, Collections.singletonList(track));
				
				message = Messages.MissionImportEngine_ImportSingleTrack;
			}
			MissionDataImport.saveTracks(tracks);
		}
		return message;
	}
	
}
