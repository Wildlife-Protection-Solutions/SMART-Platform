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

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.gpx.GpxType;
import org.wcs.smart.patrol.gpx.TrkType;
import org.wcs.smart.patrol.gpx.TrksegType;
import org.wcs.smart.patrol.gpx.WptType;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.importwp.ImportOptionsComposite.ImportOption;
import org.wcs.smart.patrol.internal.ui.importwp.gpsbabel.GPSBabel;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.ui.SavePatrolPartJob;
import org.wcs.smart.patrol.ui.SaveWaypointJob;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Class of utilties that support
 * the importing of waypoints and tracks from a variety 
 * of sources 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GPSDataImport {

	/*
	 * gpx metadata link
	 */
	private static final String GPX_METADATA_CLASSES = "org.wcs.smart.patrol.gpx"; //$NON-NLS-1$
	
	/**
	 * 
	 * Represent the type of data to import.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum ImportType{
		WAYPOINT (Messages.GPSDataImport_WaypointName, Messages.GPSDataImport_WaypointImportDescription), 
		TRACK(Messages.GPSDataImport_TrackName, Messages.GPSDataImport_TrackImportDescrption);
		
		public String guiName;
		public String importDesc;
		
		private ImportType(String gui, String importDesc){
			this.guiName = gui;
			this.importDesc = importDesc;
		}
	};
	
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
			modified.addAll(GPSDataImport.assignWaypoints(waypoints, patrol.getLegs()));

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
				message = MessageFormat.format(Messages.ImportGpsDataWizard_GPS_WarningNoneFound, new  Object[]{ImportType.WAYPOINT.guiName, ImportType.WAYPOINT.guiName});
			}else{
				message = MessageFormat.format(Messages.GPSDataImport_WaypointsImported, new Object[]{cnt, modified.size()});
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
					Date wpdt = currentLeg.getDate();
					if (pwp.getWaypoint().getDateTime() != null){
						wpdt = SmartUtils.combineDateTime(wpdt, new Time(pwp.getWaypoint().getDateTime().getTime()));
					}
					pwp.getWaypoint().setDateTime(wpdt);
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
	
	
	/**
	 * Connects to GPSBabel to get gps data from device
	 * 
	 * @param deviceType the type of gps data
	 * @param day the day to import data for; if null all data is imported
	 *
	 * @return a map of type of data imported 
	 * @throws Exception 
	 */
	public static Map<ImportType, List<Waypoint>> importGpsData(final String deviceType, final Date day, final Set<ImportType> dataType, IProgressMonitor monitor) throws Exception {
		
		final HashMap<ImportType, List<Waypoint>> data = new HashMap<ImportType, List<Waypoint>>();

		monitor.setTaskName(Messages.GPSDataImport_Progress_ImportingFromGPS);
		File f = GPSBabel.getData(deviceType, dataType);
		try {
			monitor.setTaskName(Messages.GPSDataImport_Progress_ReadingData);
			Map<ImportType, List<Waypoint>> vals = convertGpx(Collections.singletonList(f.getCanonicalPath()), day, dataType, monitor);
			for (ImportType type : dataType) {
				data.put(type, vals.get(type));
			}
		} finally {
			try {
				f.delete();
			} catch (Exception ex) {
				SmartPatrolPlugIn.log("Error deleting patrol data file.", ex); //$NON-NLS-1$
			}
		}

		return data;
	}
	
	/**
	 * Reads trackpoints from a gpx file 
	 * 
	 * @param gpsFile the gps file name
	 * @param monitor
	 * 
	 * @return list of trackpoints
	 */
	public static List<TrkType> getTracksGpx(File gpxFile, IProgressMonitor monitor) throws Exception{
		GpxType type = null;
		try {
			monitor.subTask(Messages.GPSDataImport_TrackProgress_ReadingGPXData);
			JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
			Unmarshaller un = context.createUnmarshaller();
			Object o = un.unmarshal(gpxFile);
			type = (GpxType) ((JAXBElement<?>) o).getValue();
		} catch (Exception ex) {
			throw new Exception(MessageFormat.format(
					Messages.GPSDataImport_TrackError_CouldNotReadFile, new Object[]{gpxFile.getAbsolutePath()}) + ex.getMessage(), ex);
		}
		
		if (type == null){
			throw new Exception(MessageFormat.format(Messages.GPSDataImport_TrackError_CouldNotParseFile, new Object[]{ gpxFile.getAbsolutePath()}), null);
		}
		
		monitor.subTask(Messages.GPSDataImport_Progress_ParsingTracks);
		List<TrkType> tracks = type.getTrk();
		return tracks;
	}
	
	
	private static boolean betweenDates(Date date, Date start, Date end){
		return ( date.equals(start) || date.after(start) ) &&  (date.equals(end) || date.before(end));
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
			Date d = wp.getWaypoint().getDateTime();
			
			Waypoint tmp = new Waypoint();
			tmp.setX(wp.getWaypoint().getX());
			tmp.setY(wp.getWaypoint().getY());
			tmp.setDateTime(d);
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
			Date wpdt = point.getDateTime();
			for(PatrolLeg leg : patrolLegs){
				if (betweenDates(SmartUtils.getDatePart(wpdt, false), 
						SmartUtils.getDatePart(leg.getStartDate(), false),
						SmartUtils.getDatePart(leg.getEndDate(), false))){
					//find the leg day
					
					for (PatrolLegDay legday : leg.getPatrolLegDays()){
						Date start = SmartUtils.combineDateTime(legday.getDate(), legday.getStartTime());
						Date end = SmartUtils.combineDateTime(legday.getDate(), legday.getEndTime());
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
						if (SmartUtils.getDatePart(wpdt, false).equals(
							SmartUtils.getDatePart(legday.getDate(),
								false))) {
							
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
			Date wpdt = point.getDateTime();
			if (wpdt == null){
				continue;
			}
			//find patrol leg day based on times
			for (Iterator<PatrolLeg> iterator = patrolLegs.iterator(); iterator.hasNext();) {
				PatrolLeg leg = (PatrolLeg) iterator.next();				
				if (betweenDates(SmartUtils.getDatePart(wpdt,false), SmartUtils.getDatePart(leg.getStartDate(), false), SmartUtils.getDatePart(leg.getEndDate(), false))){
					//find the leg day
					for (Iterator<PatrolLegDay> iterator2 = leg.getPatrolLegDays().iterator(); iterator2.hasNext();) {
						PatrolLegDay legday = (PatrolLegDay) iterator2.next();
						Date start = SmartUtils.combineDateTime(legday.getDate(), legday.getStartTime());
						Date end = SmartUtils.combineDateTime(legday.getDate(), legday.getEndTime());
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
						if (SmartUtils.getDatePart(wpdt, false).equals(SmartUtils.getDatePart(legday.getDate(),false))) {
							
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
	 * Reads waypoints from a gpx file.
	 * 
	 * @param gpsFile the gps file name
	 * @param monitor
	 * 
	 * @return list of waypoints in the gpx file
	 */
	public static List<WptType> getWaypointsGpx(List<String> gpxFiles, IProgressMonitor monitor){
		List<WptType> waypoints = new ArrayList<WptType>();
		
		for (String file : gpxFiles){
			File gpxFile = new File(file);
			GpxType type = null;
			try {
				monitor.subTask(Messages.GPSDataImport_WaypointProgress_ReadingGpx);
				JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
				Unmarshaller un = context.createUnmarshaller();
				Object o = un.unmarshal(gpxFile);
				type = (GpxType) ((JAXBElement<?>) o).getValue();
			} catch (Exception ex) {
				displayLog(MessageFormat.format(
						Messages.GPSDataImport_WaypointError_CouldNotReadFile,
						new Object[]{gpxFile.getAbsolutePath()}) + "\n" + ex.getMessage(), ex); //$NON-NLS-1$
				continue;
			}
		
			if (type == null){
				displayLog(MessageFormat.format(Messages.GPSDataImport_WaypointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
				continue;
			}
			monitor.subTask(Messages.GPSDataImport_Progress_ParsingWaypoints);
			waypoints.addAll(type.getWpt());
		}
		
		
		return waypoints;
	}
	
	private static void displayLog(final String message, final Exception ex){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				SmartPatrolPlugIn.displayLog(message, ex);
			}});
	}
	
	/**
	 * Reads track points from a gpx file.
	 * 
	 * @param gpsFile the gps file name
	 * @param monitor
	 * 
	 * @return list of all track points in the file with the name
	 * set to the track name
	 */
	public static List<WptType> getTrackPoints(List<String> gpxFiles, IProgressMonitor monitor){
		List<WptType> waypoints = new ArrayList<WptType>();
		for (String file : gpxFiles) {
			File gpxFile = new File(file);
			GpxType type = null;
			try {
				monitor.subTask(MessageFormat.format(Messages.GPSDataImport_Progress_ReadingGpxFileName, new Object[]{gpxFile.toString()}));
				JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
				Unmarshaller un = context.createUnmarshaller();
				Object o = un.unmarshal(gpxFile);
				type = (GpxType) ((JAXBElement<?>) o).getValue();
			} catch (Exception ex) {
				displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotRead, new Object[]{gpxFile.getAbsolutePath()}) + ex.getLocalizedMessage(), ex);
				continue;
			}

			if (type == null) {
				displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
				continue;
			}

			monitor.subTask(Messages.GPSDataImport_Progress_ParsingTrackPoints);
			for (TrkType track : type.getTrk()) {
				String name = track.getName();
				for (TrksegType seg : track.getTrkseg()) {
					for (WptType wp : seg.getTrkpt()) {
						wp.setName(name);
						waypoints.add(wp);
					}
				}
			}

		}
		return waypoints;
	}
	
	/**
	 * Converts gpx waypoints to SMART waypoints.
	 * 
	 * @param waypoints
	 * @return
	 */
	public static List<Waypoint> convertWaypoints(List<WptType> waypoints){
		ArrayList<Waypoint> neww = new ArrayList<Waypoint>();
		for(WptType wp : waypoints){
			neww.add(convertWpt(wp));
		}
		return neww;
	}
	
	/**
	 * Finds the waypoint date/time from a given wpttype object
	 * 
	 * @param wptType
	 * @return date associated with waypoint or null if date cannot be determined
	 */
	private static Date findWaypointDate(WptType wptType) {
		Date wpdt = null;
		// try to parse date
		if (wptType.getTime() != null) {
			wpdt = wptType.getTime().toGregorianCalendar().getTime();
		} else if (wptType.getCmt() != null) {
			try {
				// am-pm in system locale
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy h:mm:ssa"); //$NON-NLS-1$
				wpdt = sdf.parse(wptType.getCmt());
			} catch (ParseException e) {
			}
			
			if (wpdt == null) {
				try {
					// 24hr
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy H:mm:ss"); //$NON-NLS-1$
					wpdt = sdf.parse(wptType.getCmt());
				} catch (ParseException e) {
				}
			}
			
			if (!Locale.getDefault().equals(Locale.ENGLISH)){
				try {
					// am-pm in english locale
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy h:mm:ssa", Locale.ENGLISH); //$NON-NLS-1$
					wpdt = sdf.parse(wptType.getCmt());
				} catch (ParseException e) {
				}
				try {
					// 24hr in english locale
					SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy H:mm:ss", Locale.ENGLISH); //$NON-NLS-1$
					wpdt = sdf.parse(wptType.getCmt());
				} catch (ParseException e) {
				}
			}
			
			
			if (wpdt == null) {
				try {
					// short
					wpdt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).parse(
							wptType.getCmt());
				} catch (ParseException e) {
				}
			}
			if (wpdt == null) {
				try {
					// medium
					wpdt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).parse(
							wptType.getCmt());
				} catch (ParseException e) {
				}
			}
			if (wpdt == null) {
				try {
					// medium
					wpdt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).parse(
							wptType.getCmt());
				} catch (ParseException e) {
				}
			}

			if (wpdt == null) {
				try {
					// long
					wpdt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).parse(
							wptType.getCmt());
				} catch (ParseException e) {
				}
			}

		}
		return wpdt;
	}
	
	/**
	 * Converts a gpx waypoint to a smart waypoint.
	 * @param wptType
	 * @return
	 */
	private static Waypoint convertWpt(WptType wptType){
		Date wpdt = findWaypointDate(wptType);
		int id = -1;
		try{
			id = Integer.parseInt(wptType.getName());
		}catch (Exception ex){}

		Waypoint waypoint = new Waypoint();
		if (wpdt != null){
			waypoint.setDateTime(wpdt);
		}
		waypoint.setX(wptType.getLon().doubleValue());
		waypoint.setY(wptType.getLat().doubleValue());
		waypoint.setId(id);
		waypoint.setComment(wptType.getCmt());
		return waypoint;
	}
	
	
	
	/**
	 * 
	 * Reads data from a gpx file.  If dataType is WAYPOINT then
	 * reads all Wpts from the gpx file and returns a list of Waypoints.  
	 * If TRACK then
	 * it reads all track points, converts them to waypoints and returns this list
	 * 
	 * 
	 * <p>
	 * If day is provided then only waypoints or trackpoints that 
	 * occur on that day are imported.
	 * </p>
	 * 
	 * @param gpxFile the gpx file name 
	 * @param day the day to import data for; if null all data is imported
	 * @param monitor a progress monitor
	 * @return a hashmap that contains a key for each ImportType provided.   
	 * @throws Exception 
	 * 
	 */
	public static Map<ImportType, List<Waypoint>> convertGpx(List<String> gpxFiles, Date day, Set<ImportType> dataType, IProgressMonitor monitor) throws Exception{
		
		HashMap<ImportType, List<Waypoint>> data = new HashMap<ImportType, List<Waypoint>>();		
		Date plddt = null;
		if (day != null){
			plddt = SmartUtils.getDatePart(day, false);
		}
		for (String file : gpxFiles) {
			File gpxFile = new File(file);

			if (dataType.contains(ImportType.WAYPOINT)) {
				List<WptType> waypoints = getWaypointsGpx(Collections.singletonList(gpxFile.getAbsolutePath()), monitor);
				monitor.subTask(Messages.GPSDataImport_Progress_ParsingWaypoints);
				ArrayList<Waypoint> newwaypoints = new ArrayList<Waypoint>();
				for (Iterator<WptType> iterator = waypoints.iterator(); iterator
						.hasNext();) {
					WptType wptType = (WptType) iterator.next();
					Waypoint newwp = convertWpt(wptType);
					if (plddt == null) {
						// import all waypoints regardless of date
						newwaypoints.add(newwp);
					} else  {
						// only import waypoints whose imported date match the
						// given date
						if (newwp.getDateTime() != null && SmartUtils.getDatePart(newwp.getDateTime(), false).equals(plddt)) {
							newwaypoints.add(newwp);
						}
					}
				}
				List<Waypoint> pnts = data.get(ImportType.WAYPOINT);
				if (pnts == null){
					pnts = new ArrayList<Waypoint>();
					data.put(ImportType.WAYPOINT, pnts);
				}
				pnts.addAll(newwaypoints);
			}
			if (dataType.contains(ImportType.TRACK)) {
				monitor.subTask(Messages.GPSDataImport_Progress_ParsingTracks);
				List<Waypoint> trackCoords = new ArrayList<Waypoint>();

				List<TrkType> tracks = getTracksGpx(gpxFile, monitor);
				for (TrkType trk : tracks) {
					List<TrksegType> segments = trk.getTrkseg();
					for (TrksegType seg : segments) {
						List<WptType> trkPnt = seg.getTrkpt();
						for (WptType pnt : trkPnt) {
							double y = pnt.getLat().doubleValue();
							double x = pnt.getLon().doubleValue();
							Date datetime = findWaypointDate(pnt);

							Waypoint c = new Waypoint();
							try{
								c.setId( Integer.parseInt(pnt.getName()) );
							}catch (Exception ex){}
							c.setX(x);
							c.setY(y);
							c.setDateTime(datetime);
							c.setComment((trk.getName() == null ? "" : trk.getName()) + (pnt.getName() == null ? "" :  " - " + pnt.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							if (plddt == null) {
								// include all
								trackCoords.add(c);
							} else if (plddt != null && datetime != null) {
								// include only waytpoints which match current
								// date
								if (SmartUtils.getDatePart(datetime, false)
										.equals(plddt)) {
									trackCoords.add(c);
								}
							}

						}
					}
				}
				List<Waypoint> pnts = data.get(ImportType.TRACK);
				if (pnts == null){
					pnts = new ArrayList<Waypoint>();
					data.put(ImportType.TRACK, pnts);
				}
				pnts.addAll(trackCoords);
			}
		}
		return data;
	}


			
	/**
	 * Converts a set of waypoints to a track.  Coordinates are first sorted
	 * by date/time.
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Waypoint> coordinates){
		if (coordinates.size() < 2) {
			return null;
		}
		GeometryFactory gf = new GeometryFactory();
		Collections.sort(coordinates, new Comparator<Waypoint>() {
			@Override
			public int compare(Waypoint o1, Waypoint o2) {
				return o1.getDateTime().compareTo(o2.getDateTime());
			}
		});

		List<Coordinate> cs = new ArrayList<Coordinate>();
		for (Waypoint w : coordinates) {
			Calendar c1 = Calendar.getInstance();
			c1.setTimeInMillis(w.getDateTime().getTime());
			Calendar c2 = Calendar.getInstance();
			c2.setTimeZone(Track.ZTIMEZONE);
			c2.setTimeInMillis(0);
			c2.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH),
					c1.get(Calendar.DATE), c1.get(Calendar.HOUR_OF_DAY),
					c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));

			Coordinate c = new Coordinate(w.getX(), w.getY(), c2.getTime()
					.getTime());
			cs.add(c);
		}

		LineString track = gf.createLineString(cs
				.toArray(new Coordinate[coordinates.size()]));
		Track t = new Track();
		t.setLineString(track);
		return t;
	}
	
	
	/**
	 * Converts a set of track points to coordinates.
	 * 
	 * @param trackpoints
	 * @return
	 */
	public static List<Coordinate> convertPointsToTrack(List<WptType> trackpoints){
		ArrayList<Coordinate> trackCoords = new ArrayList<Coordinate>();
		for (WptType pnt : trackpoints){
			double y = pnt.getLat().doubleValue();
			double x = pnt.getLon().doubleValue();
			long time = 0;
			if (pnt.getTime() != null){
				time = pnt.getTime().toGregorianCalendar().getTime().getTime();
			}
			Coordinate c = new Coordinate(x, y, time);
			trackCoords.add(c);
		}
		return trackCoords;
	}

}
