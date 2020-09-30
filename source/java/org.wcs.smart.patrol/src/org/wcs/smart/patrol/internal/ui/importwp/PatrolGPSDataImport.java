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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.gpx.GPSDataImport.ImportType;
import org.wcs.smart.observation.common.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.observation.common.importwp.ObservationGPSDataImport;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.SavePatrolPartJob;
import org.wcs.smart.patrol.ui.SaveWaypointJob;

/**
 * Class of utilities that support
 * the importing of waypoints and tracks from a variety 
 * of sources 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolGPSDataImport {

	
	/**
	 * Saves the generated tracks to the database 
	 * @param tracks tracks to save
	 * @throws Exception
	 */
	public static void saveTracks(final HashMap<PatrolLegDay, Track> tracks) throws Exception {

		//update object references
		for (Iterator<Entry<PatrolLegDay, Track>> iterator = tracks.entrySet().iterator(); iterator.hasNext();) {
			Entry<PatrolLegDay, Track> type = (Entry<PatrolLegDay, Track>) iterator.next();
			PatrolLegDay pld = type.getKey();
			Track t = type.getValue();
			if (t != null){
				pld.setTrack(t);
				t.setPatrolLegDay(pld);
			}else{;
				pld.setTrack(null);
			}
		}
		
		//save first
		for (Iterator<PatrolLegDay> iterator = tracks.keySet().iterator(); iterator.hasNext();) {
			PatrolLegDay pldToSave = (PatrolLegDay) iterator.next();
			SavePatrolPartJob saveJob = new SavePatrolPartJob(pldToSave.getPatrolLeg().getPatrol(), pldToSave);
			saveJob.schedule();
			saveJob.join();
		}
		
		//fire events
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				for (Iterator<PatrolLegDay> iterator = tracks.keySet().iterator(); iterator.hasNext();) {
					PatrolLegDay pldToSave = (PatrolLegDay) iterator.next();
					PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_TRACKS, pldToSave);
				}
			}
		});
	}
	
	/**
	 * Saves a set of waypoints to the database
	 * 
	 * @param op import option
	 * @param patrol patrol
	 * @param currentLeg current leg (used if import option = DATE)
	 * @param waypoints set of waypoints
	 * @return status message
	 * @throws InterruptedException
	 */
	public static String saveWaypoints(ImportOption op, Patrol patrol, final PatrolLegDay currentLeg, List<Waypoint> waypoints) throws InterruptedException{
		String message = null;
		final Set<PatrolLegDay> modified = new HashSet<PatrolLegDay>();
		final List<PatrolWaypoint> addedWaypoints = new ArrayList<PatrolWaypoint>();
		if (op == ImportOption.ALL){
			//assign waypoints to days
			modified.addAll(PatrolGPSDataImport.assignWaypoints(waypoints, patrol.getLegs()));

			int cnt = 0;
			for(PatrolLegDay pld : modified){
				for (PatrolWaypoint pw : pld.getWaypoints()){
					if (pw.getWaypoint().getUuid() == null){
						//new waypoint add to our cnt
						cnt++;
					}
				}
				addedWaypoints.addAll(pld.getWaypoints());
			}
			if (addedWaypoints.size() == 0){
				//nothing imported; not date matched
				message = MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new  Object[]{ImportType.WAYPOINT.guiName.toLowerCase(), ImportType.WAYPOINT.guiName.toLowerCase(), ImportType.WAYPOINT.guiName.toLowerCase()});
			}else{
				if (patrol.getLegs().size() == 1){
					//only one leg; so this is the number of dates
					message = MessageFormat.format(Messages.GPSDataImport_WaypointsImported, new Object[]{cnt, modified.size()});
				}else{
					message = MessageFormat.format(Messages.GPSDataImport_WaypointsImportedLegs, new Object[]{cnt, modified.size()});
				}
			}	
		}else{
			modified.add(currentLeg);
			for (Waypoint w : waypoints){
				PatrolWaypoint pwp = new PatrolWaypoint();
				pwp.setPatrolLegDay(currentLeg);
				pwp.setWaypoint(w);				
				
				currentLeg.getWaypoints().add(pwp);
				addedWaypoints.add(pwp);
				if (op == ImportOption.SELECT){
					LocalDate wpd = currentLeg.getDate();
					LocalTime wpt = LocalTime.MIN;
					
					if (pwp.getWaypoint().getDateTime() != null){
						wpt = pwp.getWaypoint().getDateTime().toLocalTime();
					}
					pwp.getWaypoint().setDateTime(LocalDateTime.of(wpd, wpt));
				}
			}
			message = MessageFormat.format(Messages.GPSDataImport_WaypointsImportedCurrentDay, new Object[]{waypoints.size()});
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
				for (PatrolLegDay day : modified){	
					PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, day);
				}
			}});
	
		
		return message;
	}
	
	
	private static boolean betweenDates(LocalDate date, LocalDate start, LocalDate end){
		return ( date.isEqual(start) || date.isAfter(start) ) &&  (date.isEqual(end) || date.isBefore(end));
	}
	
	private static boolean betweenDates(LocalDateTime date, LocalDateTime start, LocalDateTime end){
		return ( date.isEqual(start) || date.isAfter(start) ) &&  (date.isEqual(end) || date.isBefore(end));
	}
	
	/**
	 * For each patrol leg day computes the track and returns the results.
	 * 
	 * Tracks are only created for patrol leg days with more than 1 waypoints.
	 * 
	 * @param patrolLegDays
	 * @return
	 */
	public static HashMap<PatrolLegDay, Track> computeTracksFromWaypoints(Collection<PatrolLeg> patrolLegs){
		HashMap<PatrolLegDay, Track> output = new HashMap<PatrolLegDay, Track> ();
		for(PatrolLeg leg : patrolLegs){
			for (PatrolLegDay day : leg.getPatrolLegDays()){
				Track newTrack = computeTrackFromWaypoints(day);
				output.put(day, newTrack);
			}
		}
		return output;
	}


	/**
	 * Create track from the waypoints for a given patrol leg day.  If
	 * < 2 waypoints then null is returned.
	 * @param day
	 * @return
	 */
	public static Track computeTrackFromWaypoints(PatrolLegDay day) {
		if (day.getWaypoints().size() < 2){
			return null;
		}
		List<Waypoint> coords = new ArrayList<Waypoint>();
		for (PatrolWaypoint wp : day.getWaypoints()){
			Waypoint tmp = new Waypoint();
			tmp.setRawX(wp.getWaypoint().getRawX());
			tmp.setRawY(wp.getWaypoint().getRawY());
			tmp.setDateTime(wp.getWaypoint().getDateTime());
			coords.add(tmp);
		}
		Track newTrack = convertToTrack(coords);
		return newTrack;
	}
	
	
	/**
	 * Converts waypoints to tracks based on 
	 * imported date and the patrol leg dates.
	 * 
	 * @param trackpoints list of waypoints to use to make up tracks
	 * @param patrolLegs
	 * @return HashMap of patrol leg to track
	 */
	public static HashMap<PatrolLegDay, Track> convertTracks(List<Waypoint> trackpoints, List<PatrolLeg> patrolLegs){
		
		HashMap<PatrolLegDay, List<Waypoint>> tracks = new HashMap<PatrolLegDay, List<Waypoint>>();
		
		for (Waypoint point : trackpoints){
			if (point.getDateTime() == null){
				continue;
			}
			
			boolean found = false;
			LocalDateTime wpdt = point.getDateTime();
			for(PatrolLeg leg : patrolLegs){
				if (betweenDates(wpdt.toLocalDate(), leg.getStartDate(), leg.getEndDate())){
					//find the leg day
					
					for (PatrolLegDay legday : leg.getPatrolLegDays()){
						LocalDateTime start = LocalDateTime.of(legday.getDate(), legday.getStartTime());
						LocalDateTime end = LocalDateTime.of(legday.getDate(), legday.getEndTime());
						if (betweenDates(wpdt, start, end)){
							found = true;
							
							List<Waypoint> trackpnts = tracks.get(legday);
							if (trackpnts == null){
								trackpnts = new ArrayList<Waypoint>();
								tracks.put(legday, trackpnts);
							}
							trackpnts.add(point);
							break;
						}
					}
				}
				if (found)break;
			}
				
		
		
			if (!found) {
				// start time could not be found; assign based on date only
				for(PatrolLeg leg : patrolLegs){
					for (PatrolLegDay legday : leg.getPatrolLegDays()) {
						List<Waypoint> trackpnts = tracks.get(legday);
						if (trackpnts == null) {
							trackpnts = new ArrayList<Waypoint>();
							tracks.put(legday, trackpnts);
						}
						if (wpdt.toLocalDate().isEqual(legday.getDate())) {
							trackpnts.add(point);
						}
						found = true;
						break;
					}
					if(found)break;
				}
			}
		}
		
		
		HashMap<PatrolLegDay, Track> output = new HashMap<PatrolLegDay, Track>();
		//convert to tracks
		for (Iterator<Entry<PatrolLegDay, List<Waypoint>>> iterator = tracks.entrySet().iterator(); iterator.hasNext();) {
			Entry<PatrolLegDay, List<Waypoint>> value = (Entry<PatrolLegDay, List<Waypoint>>) iterator.next();
			Track newTrack = convertToTrack(value.getValue());
			if (newTrack != null){
				output.put(value.getKey(), newTrack);
			}
		}
		return output;
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
	public static Set<PatrolLegDay> assignWaypoints(List<Waypoint> waypoints, List<PatrolLeg> patrolLegs){
		Set<PatrolLegDay> modified = new HashSet<PatrolLegDay>();
		
		for (Waypoint point : waypoints){
			
			boolean found = false;
			LocalDateTime wpdt = point.getDateTime();
			if (wpdt == null){
				continue;
			}
			//find patrol leg day based on times
			for (Iterator<PatrolLeg> iterator = patrolLegs.iterator(); iterator.hasNext();) {
				PatrolLeg leg = (PatrolLeg) iterator.next();		
				
				
				if (betweenDates(wpdt.toLocalDate(), leg.getStartDate(), leg.getEndDate())){
					//find the leg day
					for (Iterator<PatrolLegDay> iterator2 = leg.getPatrolLegDays().iterator(); iterator2.hasNext();) {
						PatrolLegDay legday = (PatrolLegDay) iterator2.next();
						LocalDateTime start = LocalDateTime.of(legday.getDate(), legday.getStartTime());
						LocalDateTime end = LocalDateTime.of(legday.getDate(), legday.getEndTime());
						if (betweenDates(wpdt, start, end)){
							
							PatrolWaypoint pwp = new PatrolWaypoint();
							pwp.setPatrolLegDay(legday);
							pwp.setWaypoint(point);
							
							legday.getWaypoints().add(pwp);
							
							modified.add(legday);
							found = true;
							break;
						}
					}
					
				}
				if (found){
					break;
				}
			}
			if (!found){
				//search only for dates not times
				for (Iterator<PatrolLeg> iterator = patrolLegs.iterator(); iterator.hasNext();) {
					PatrolLeg leg = (PatrolLeg) iterator.next();
					for (Iterator<PatrolLegDay> iterator2 = leg.getPatrolLegDays().iterator(); iterator2.hasNext();) {
						PatrolLegDay legday = (PatrolLegDay) iterator2.next();
						if (wpdt.toLocalDate().isEqual(legday.getDate())) {
							
							PatrolWaypoint pwp = new PatrolWaypoint();
							pwp.setPatrolLegDay(legday);
							pwp.setWaypoint(point);
							
							legday.getWaypoints().add(pwp);
							modified.add(legday);
							found = true;
							break;
						}
					}
					if (found){
						break;
					}
				}
			}
		}
		return modified;
	}
	
	/**
	 * Converts a set of waypoints to a track.  Coordinates are first sorted
	 * by date/time.
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Waypoint> coordinates){
		if (coordinates.isEmpty()) return null;
		if (coordinates.size() == 1) return null; //throw new RuntimeException("Multiple points required to generate a track.  Only a single point found."); 
		LineString track = ObservationGPSDataImport.convertToLineString(coordinates);
		Track t = new Track();
		t.setLineStrings(Arrays.asList(track));
		return t;
	}
	
}
