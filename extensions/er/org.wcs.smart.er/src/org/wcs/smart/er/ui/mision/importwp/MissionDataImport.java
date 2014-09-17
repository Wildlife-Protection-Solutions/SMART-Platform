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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Display;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.ui.mision.editor.SaveMissionJob;
import org.wcs.smart.er.ui.mision.editor.SaveWaypointJob;
import org.wcs.smart.observation.common.importwp.GPSDataImport;
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
	public static String saveWaypoints(ImportOption op, final Mission mission, Date date, List<Waypoint> waypoints) throws InterruptedException {
		String message = null;
		final List<SurveyWaypoint> addedWaypoints = new ArrayList<SurveyWaypoint>();
		for (Waypoint w : waypoints) {
			SurveyWaypoint swp = new SurveyWaypoint();
			swp.setMission(mission);
			swp.setWaypoint(w);				
			
			mission.getWaypoints().add(swp);
			addedWaypoints.add(swp);
			if (op == ImportOption.SELECT && date != null) {
				Date wpdt = date;
				if (swp.getWaypoint().getDateTime() != null) {
					wpdt = SmartUtils.combineDateTime(wpdt, new Time(swp.getWaypoint().getDateTime().getTime()));
				}
				swp.getWaypoint().setDateTime(wpdt);
			}
		}
		message = MessageFormat.format(Messages.MissionDataImport_ResultMessage, new Object[]{addedWaypoints.size()});
		
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

	public static List<MissionTrack> convertTracks(List<Waypoint> waypoints, Mission mission) {
		Date start = SmartUtils.getDatePart(mission.getStartDate(), false);
		Date end = SmartUtils.getDatePart(mission.getEndDate(), false);

		Map<Date, List<Waypoint>> tracks = new HashMap<Date, List<Waypoint>>();
		
		for (Waypoint point : waypoints) {
			if (point.getDateTime() == null) {
				continue;
			}
			Date wpdt = point.getDateTime();
			wpdt = SmartUtils.getDatePart(wpdt, false);
			if (betweenDates(wpdt, start, end)) {
				List<Waypoint> trackpnts = tracks.get(wpdt);
				if (trackpnts == null) {
					trackpnts = new ArrayList<Waypoint>();
					tracks.put(wpdt, trackpnts);
				}
				trackpnts.add(point);
			}
		}
		
		List<MissionTrack> output = new ArrayList<MissionTrack>();
		//convert to tracks
		for (Iterator<Entry<Date, List<Waypoint>>> iterator = tracks.entrySet().iterator(); iterator.hasNext();) {
			Entry<Date, List<Waypoint>> value = (Entry<Date, List<Waypoint>>) iterator.next();
			MissionTrack newTrack = convertToTrack(value.getValue());
			newTrack.setDate(value.getKey());
			if (newTrack != null){
				output.add(newTrack);
			}
		}
		return output;
		
	}

	private static boolean betweenDates(Date date, Date start, Date end) {
		return ( date.equals(start) || date.after(start) ) &&  (date.equals(end) || date.before(end));
	}
	
	/**
	 * Converts a set of waypoints to a track.  Coordinates are first sorted
	 * by date/time.
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static MissionTrack convertToTrack(List<Waypoint> coordinates){
		LineString track = GPSDataImport.convertToLineString(coordinates, MissionTrack.ZTIMEZONE);
		MissionTrack t = new MissionTrack();
		t.setLineString(track);
		return t;
	}

	public static void saveTracks(final Mission mission, List<MissionTrack> tracks) throws InterruptedException {
		for (MissionTrack track : tracks) {
			track.setMission(mission);
			mission.getTracks().add(track);
		}
		
		SaveMissionJob saveJob = new SaveMissionJob(mission);
		saveJob.schedule();
		saveJob.join();
		
		//fire events
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				SurveyEventHandler.getInstance().fireEvent(EventType.MISSION_MODIFIED, mission);
			}
		});
	}

	
}
