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

import java.sql.Time;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.editor.SaveMissionTracksJob;
import org.wcs.smart.er.ui.mision.editor.SaveWaypointJob;
import org.wcs.smart.observation.common.importwp.GPSDataImport;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.LineString;

/**
 * Class of utilities that support
 * the importing of waypoints and tracks from a variety 
 * of sources 
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDataImport {

	/**
	 * Saves a set of waypoints to the database
	 * 
	 * @param op import option
	 * @param mission mission
	 * @param waypoints set of waypoints
	 * @return status message
	 * @throws InterruptedException
	 */
	public static String saveWaypoints(ImportOption op, final Mission mission, 
			MissionDay missionDay, List<Waypoint> waypoints) throws InterruptedException {
		
		String message = null;
		final Set<MissionDay> modified = new HashSet<MissionDay>();
		final List<SurveyWaypoint> addedWaypoints = new ArrayList<SurveyWaypoint>();
		if (op == ImportOption.ALL){
			//assign waypoints to days
			modified.addAll(assignWaypoints(waypoints, mission.getMissionDays()));

			int cnt = 0;
			for(MissionDay md : modified){
				for (SurveyWaypoint pw : md.getWaypoints()){
					if (pw.getWaypoint().getUuid() == null){
						//new waypoint add to our cnt
						cnt++;
					}
				}
				addedWaypoints.addAll(md.getWaypoints());
			}
			if (addedWaypoints.size() == 0){
				//nothing imported; not date matched
				message = MessageFormat.format(Messages.MissionDataImport_NoWaypoints, new  Object[]{ImportType.WAYPOINT.guiName, ImportType.WAYPOINT.guiName});
			}else{
				//only one leg; so this is the number of dates
				message = MessageFormat.format(Messages.MissionDataImport_NumberImported, new Object[]{cnt, modified.size()});
			}	
		}else{
			modified.add(missionDay);
			for (Waypoint w : waypoints){
				SurveyWaypoint sw = new SurveyWaypoint();
				sw.setMissionDay(missionDay);
				sw.setWaypoint(w);				
				
				missionDay.getWaypoints().add(sw);
				addedWaypoints.add(sw);
				if (op == ImportOption.SELECT){
					Date wpdt = missionDay.getDate();
					if (sw.getWaypoint().getDateTime() != null){
						wpdt = SmartUtils.combineDateTime(wpdt, new Time(sw.getWaypoint().getDateTime().getTime()));
					}
					sw.getWaypoint().setDateTime(wpdt);
				}
			}
			message = MessageFormat.format(Messages.MissionDataImport_NumberImported2, new Object[]{waypoints.size()});
		}
		
		//start up a save job
		SaveWaypointJob saveJob = new SaveWaypointJob();
		saveJob.setWaypoints(addedWaypoints);
		saveJob.schedule();
		saveJob.join();
		
		//fire events
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
			}});
		
		return message;
	}

	/**
	 * Converts waypoints to tracks based on 
	 * imported dates and the patrol leg dates.
	 * 
	 * @param waypoints
	 * @param mission
	 * @return
	 */
	public static HashMap<MissionDay, List<MissionTrack>> convertTracks(List<Waypoint> waypoints, List<MissionDay> missionDays) {
		
		HashMap<MissionDay, List<MissionTrack>> mdtracks = new HashMap<MissionDay, List<MissionTrack>>();
		
		Map<String, Item> tracksPnts = new HashMap<String, Item>();
		for (Waypoint point : waypoints){
			if (point.getDateTime() == null){
				continue;
			}
			Date wpdt = point.getDateTime();
			for (MissionDay mday : missionDays){
				if (SmartUtils.getDatePart(wpdt, false).equals(
						SmartUtils.getDatePart(mday.getDate(), false))){

					String key = mday.getDate().toString() + "_" + point.getSourceId(); //$NON-NLS-1$
					Item trackpnts = tracksPnts.get(key);
					if (trackpnts == null) {
						trackpnts = new Item();
						trackpnts.date = wpdt;
						trackpnts.missionDay = mday;
						trackpnts.waypoints = new ArrayList<Waypoint>();
						
						tracksPnts.put(key, trackpnts);
					}
					trackpnts.waypoints.add(point);
					break;
				}
			}
		}
		
		//convert to tracks
		int cnt = 0;
		for (Iterator<Item> iterator = tracksPnts.values().iterator(); iterator.hasNext();) {
			Item value = iterator.next(); 
			MissionTrack newTrack = convertToTrack(value.waypoints, false).get(0);
			String id = value.waypoints.get(0).getSourceId();
			cnt++;
			if (id == null || id.length() == 0){
				id = MessageFormat.format(Messages.MissionDataImport_TrackIdLabel, new Object[]{cnt});
			}
			newTrack.setId(id);
			newTrack.setMissionDay(value.missionDay);
			if (newTrack != null){
				List<MissionTrack> tracks = mdtracks.get(value.missionDay);
				if (tracks == null){
					tracks = new ArrayList<MissionTrack>();
					mdtracks.put(value.missionDay, tracks);
				}
				tracks.add(newTrack);
			}
		}
		return mdtracks;		
	}

	/**
	 * Converts a set of waypoints to a track.  Coordinates are first sorted
	 * by date/time.  If useSource is specified then multiple tracks are generated based on the 
	 * value of the waypoint source field.
	 * 
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static List<MissionTrack> convertToTrack(List<Waypoint> coordinates, boolean useSource){
		if (!useSource){
			LineString track = GPSDataImport.convertToLineString(coordinates, MissionTrack.ZTIMEZONE);
			MissionTrack t = new MissionTrack();
			t.setLineString(track);
			t.setId(Messages.MissionDataImport_TrackLabel);
			return Collections.singletonList(t);
		}else{
			HashMap<String, List<Waypoint>> wps = new HashMap<String, List<Waypoint>>();
			for (Waypoint wp : coordinates){
				List<Waypoint> ws = wps.get(wp.getSourceId());
				if (ws == null){
					ws = new ArrayList<Waypoint>();
					wps.put(wp.getSourceId(), ws);
				}
				ws.add(wp);
			}
			List<MissionTrack> tracks = new ArrayList<MissionTrack>();
			int cnt = 0;
			for (List<Waypoint> ws : wps.values()){
				MissionTrack mt = convertToTrack(ws, false).get(0);
				cnt++;
				String id = ws.get(0).getSourceId();
				if (id == null || id.length() == 0){
					id = MessageFormat.format(Messages.MissionDataImport_TrackIdLabel, new Object[]{cnt});
				}
				mt.setId(id);
				
				tracks.add(mt);
			}
			return tracks;
			
		}
	}

	public static void saveTracks(Map<MissionDay, List<MissionTrack>> tracks) throws InterruptedException {
		Mission mission = null;
		List<MissionTrack> tracksToSave = new ArrayList<MissionTrack>();
		for (Iterator<Entry<MissionDay, List<MissionTrack>>> iterator = tracks.entrySet().iterator(); iterator.hasNext();) {
			Entry<MissionDay, List<MissionTrack>> type = (Entry<MissionDay, List<MissionTrack>>) iterator.next();
			
			for (MissionTrack track : type.getValue()){
				MissionDay md = type.getKey();
				mission = md.getMission();
				md.getTracks().add(track);
				track.setMissionDay(md);
			}
			tracksToSave.addAll(type.getValue());
		}
		
		SaveMissionTracksJob saveJob = new SaveMissionTracksJob(tracksToSave);
		saveJob.schedule();
		saveJob.join();
		
		//fire events
		final Mission lmission = mission;
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, lmission);
			}
		});
	}

	/**
	 * For each waypoint, determines the patrol leg day and adds the waypoint
	 * to that patrol leg day.
	 * 
	 * @param waypoints  Set of waypoints to process
	 * @param patrolLegs set of patrol legs
	 * @param monitor progress monitor
	 * 
	 * @return list of patrol leg days modified
	 */
	public static Set<MissionDay> assignWaypoints(List<Waypoint> waypoints, List<MissionDay> days){
		Set<MissionDay> modified = new HashSet<MissionDay>();
		
		for (Waypoint point : waypoints){
			Date wpdt = point.getDateTime();
			if (wpdt == null){
				continue;
			}
			//find patrol leg day based on times
			for (Iterator<MissionDay> iterator = days.iterator(); iterator.hasNext();) {
				MissionDay mday = (MissionDay) iterator.next();				
				if (SmartUtils.getDatePart(wpdt, false).equals(
						SmartUtils.getDatePart(mday.getDate(), false))){
					
					SurveyWaypoint se = new SurveyWaypoint();
					se.setMissionDay(mday);
					se.setWaypoint(point);
					mday.getWaypoints().add(se);
					modified.add(mday);
					break;
				}
			}
		}
		return modified;
	}
	
	
	static class Item{
		public Date date;
		public String id;
		public MissionDay missionDay;
		public List<Waypoint> waypoints;
		
	}
}
