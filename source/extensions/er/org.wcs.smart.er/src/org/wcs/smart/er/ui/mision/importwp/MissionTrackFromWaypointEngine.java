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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.TrackFromWaypointEngine;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Import engine for generating tracks
 * from waypoints.
 * 
 * @author Emily
 *
 */
public class MissionTrackFromWaypointEngine extends TrackFromWaypointEngine {

	private MissionDay missionDay;
	
	public MissionTrackFromWaypointEngine(MissionDay missionDay){
		this.missionDay = missionDay;
	}
	
	@Override
	public String updateSourceObject(ImportOption option, ImportType type,
			Object object, List<Waypoint> data, IProgressMonitor monitor)
			throws Exception {
		String message = null;
		HashMap<MissionDay, List<MissionTrack>> tracks = new HashMap<MissionDay, List<MissionTrack>>();
		if (option == ImportOption.ALL){
			int cnt = 0;
			for (MissionDay md : missionDay.getMission().getMissionDays()){
				MissionTrack mt = MissionDataImport.createTrackFromWaypoints(md);
				if (mt != null){
					mt.setMissionDay(md);
					tracks.put(md, Collections.singletonList(mt));
				}
				cnt++;
			}
			message = MessageFormat.format(Messages.MissionTrackFromWaypointEngine_MultiDayMessage, new Object[]{cnt, tracks.size()});
		}else if (option == ImportOption.DATE){
			MissionTrack mt = MissionDataImport.createTrackFromWaypoints(missionDay);
			if (mt != null){
				mt.setMissionDay(missionDay);
				tracks.put(missionDay, Collections.singletonList(mt));
				message = MessageFormat.format(Messages.MissionTrackFromWaypointEngine_SingleDayMessage, new Object[]{DateFormat.getDateInstance().format(missionDay.getDate())});
			}else{
				message = Messages.MissionTrackFromWaypointEngine_TwoPointsRequired;
			}
		}
		MissionDataImport.saveTracks(tracks);
		return message;
	}
}
