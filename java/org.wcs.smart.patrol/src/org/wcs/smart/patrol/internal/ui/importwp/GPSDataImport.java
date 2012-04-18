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
import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.gpx.GpxType;
import org.wcs.smart.patrol.gpx.TrkType;
import org.wcs.smart.patrol.gpx.TrksegType;
import org.wcs.smart.patrol.gpx.WptType;
import org.wcs.smart.patrol.internal.ui.importwp.gpsbabel.GPSBabel;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Class that imports data from gps device.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GPSDataImport {

	private static final String GPX_METADATA_CLASSES = "org.wcs.smart.patrol.gpx";
	
	/**
	 * 
	 * Represent the type of data to import.
	 * 
	 * @author Emily
	 * @since 1.0.0
	 */
	public enum ImportType{
		WAYPOINT ("Waypoint", "Select the waypoints to import.  The name and date/time of the waypoint is listed below.  If the date/time is not listed it could not be determined."), 
		TRACK("Track", "Select the track points to import.  Track points listed below contain the track name and the datetime (if available) of the track point.");
		
		public String guiName;
		public String importDesc;
		
		private ImportType(String gui, String importDesc){
			this.guiName = gui;
			this.importDesc = importDesc;
		}
	};
	
	
	/**
	 * Display the wizard that imports data from GPS device.  Then parses the input
	 * and returns the parsed data.
	 * 
	 * @param deviceType the type of gps data
	 * @param day the day to import data for; if null all data is imported
	 *
	 * @return a map of type of data imported 
	 */
	public static Map<ImportType, Object> importGpsData(final String deviceType, final Date day, final Set<ImportType> dataType, IProgressMonitor monitor) throws IOException {
		
		final HashMap<ImportType, Object> data = new HashMap<ImportType, Object>();

		monitor.setTaskName("Importing Data from GPS Device");
		File f = GPSBabel.getData(deviceType, dataType);
		try {
			monitor.setTaskName("Reading data");
			Map<ImportType, Object> vals = convertGpx(f, day, dataType, monitor);
			for (ImportType type : dataType) {
				data.put(type, vals.get(type));
			}
		} finally {
			try {
				f.delete();
			} catch (Exception ex) {
				SmartPatrolPlugIn.log("Error deleting patrol data file.", ex);
			}
		}

		return data;
	}

	/**
	 * Reads waypoints from a gpx file.
	 * @param gpsFile the gps file name
	 * @param monitor
	 * 
	 * @return list of waypoints in the gpx file
	 */
	public static List<TrkType> getTracksGpx(File gpxFile, IProgressMonitor monitor){
		GpxType type = null;
		try {
			monitor.subTask("Reading gpx data.");
			JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
			Unmarshaller un = context.createUnmarshaller();
			Object o = un.unmarshal(gpxFile);
			type = (GpxType) ((JAXBElement) o).getValue();
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog("Could not read data from file "
					+ gpxFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
			return null;
		}
		
		if (type == null){
			SmartPatrolPlugIn.displayLog("Could not parse file " + gpxFile.getAbsolutePath(), null);
			return null;
		}
		
		monitor.subTask("Parsing tracks.");
		List<TrkType> tracks = type.getTrk();
		return tracks;
	}
	
	
	private static boolean betweenDates(Date date, Date start, Date end){
		return ( date.equals(start) || date.after(start) ) &&  (date.equals(end) || date.before(end));
	}
	
	
	/**
	 * Converts coordinates to tracks based on date (provided in Z) and the patrol leg dates.
	 * 
	 * @param trackpoints
	 * @param patrolLegs
	 * @return HashMap of patrol leg to track
	 */
	public static HashMap<PatrolLegDay, Track> convertTracks(List<Coordinate> trackpoints, List<PatrolLeg> patrolLegs){
		
		HashMap<PatrolLegDay, List<Coordinate>> tracks = new HashMap<PatrolLegDay, List<Coordinate>>();
		
		for (Coordinate point : trackpoints){
			if (Double.isNaN(point.z)){
				continue;
			}
			Date wpdt = new Date((long)point.z);
			for(PatrolLeg leg : patrolLegs){
				if (betweenDates(wpdt, leg.getStartDate(), leg.getEndDate())){
					//find the leg day
					boolean found = false;
					for (PatrolLegDay legday : leg.getPatrolLegDays()){
						Date start = SmartUtils.combineDateTime(legday.getDate(), legday.getStartTime());
						Date end = SmartUtils.combineDateTime(legday.getDate(), legday.getEndTime());
						if (betweenDates(wpdt, start, end)){
							found = true;
							
							List<Coordinate> trackpnts = tracks.get(legday);
							if (trackpnts == null){
								trackpnts = new ArrayList<Coordinate>();
								tracks.put(legday, trackpnts);
							}
							trackpnts.add(point);
						}
					
					
					}
					
					if (!found) {
						// start time could not be found; assign based on date only
						for (PatrolLegDay legday : leg.getPatrolLegDays()) {
							List<Coordinate> trackpnts = tracks.get(legday);
							if (trackpnts == null) {
								trackpnts = new ArrayList<Coordinate>();
								tracks.put(legday, trackpnts);
							}
							if (SmartUtils.getDatePart(wpdt, false).equals(
									SmartUtils.getDatePart(legday.getDate(),
											false))) {
								trackpnts.add(point);
							}
						}
					}
				}
			}
		}		
		
		HashMap<PatrolLegDay, Track> output = new HashMap<PatrolLegDay, Track>();
		//convert to tracks
		for (Iterator<Entry<PatrolLegDay, List<Coordinate>>> iterator = tracks.entrySet().iterator(); iterator.hasNext();) {
			Entry<PatrolLegDay, List<Coordinate>> value = (Entry<PatrolLegDay, List<Coordinate>>) iterator.next();
			Track newTrack = convertToTrack(value.getValue());
			output.put(value.getKey(), newTrack);
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
			
			Date wpdt = point.getImportedDate();
			
			if (wpdt == null){
				continue;
			}
			for(PatrolLeg leg : patrolLegs){
				if (betweenDates(SmartUtils.getDatePart(wpdt,false), leg.getStartDate(), leg.getEndDate())){
					//find the leg day
					boolean found = false;
					for (PatrolLegDay legday : leg.getPatrolLegDays()){
						Date start = SmartUtils.combineDateTime(legday.getDate(), legday.getStartTime());
						Date end = SmartUtils.combineDateTime(legday.getDate(), legday.getEndTime());
						if (betweenDates(wpdt, start, end)){
							legday.getWaypoints().add(point);
							point.setPatrolLegDay(legday);
							if (point.getTime() == null){
								point.setTime(new Time(SmartUtils.getMidnight().getTime()));
							}
							modified.add(legday);
							found = true;
							break;
						}
					}
					if (!found) {
						// start time could not be found; assign based on date
						// only
						for (PatrolLegDay legday : leg.getPatrolLegDays()) {
							if (SmartUtils.getDatePart(wpdt, false).equals(SmartUtils.getDatePart(legday.getDate(),false))) {
								legday.getWaypoints().add(point);
								modified.add(legday);
								point.setPatrolLegDay(legday);
								if (point.getTime() == null) {
									point.setTime(new Time(SmartUtils
											.getMidnight().getTime()));
								}
								break;
							}
						}
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
	public static List<WptType> getWaypointsGpx(File gpxFile, IProgressMonitor monitor){
		GpxType type = null;
		try {
			monitor.subTask("Reading gpx data.");
			JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
			Unmarshaller un = context.createUnmarshaller();
			Object o = un.unmarshal(gpxFile);
			type = (GpxType) ((JAXBElement) o).getValue();
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog("Could not read data from file "
					+ gpxFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
			return null;
		}
		
		if (type == null){
			SmartPatrolPlugIn.displayLog("Could not parse file " + gpxFile.getAbsolutePath(), null);
			return null;
		}
		
		monitor.subTask("Parsing waypoints.");
		List<WptType> waypoints = type.getWpt();
		return waypoints;
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
	public static List<WptType> getTrackPoints(File gpxFile, IProgressMonitor monitor){
		GpxType type = null;
		try {
			monitor.subTask("Reading gpx data.");
			JAXBContext context = JAXBContext.newInstance(GPX_METADATA_CLASSES);
			Unmarshaller un = context.createUnmarshaller();
			Object o = un.unmarshal(gpxFile);
			type = (GpxType) ((JAXBElement) o).getValue();
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog("Could not read data from file "
					+ gpxFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
			return null;
		}
		
		if (type == null){
			SmartPatrolPlugIn.displayLog("Could not parse file " + gpxFile.getAbsolutePath(), null);
			return null;
		}
		
		List<WptType> waypoints = new ArrayList<WptType>();
		
		monitor.subTask("Parsing track points.");
		for (TrkType track: type.getTrk()){
			String name = track.getName();
			for (TrksegType seg : track.getTrkseg()){
				for (WptType wp : seg.getTrkpt()){
					wp.setName(name);
					waypoints.add(wp);
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
				// am-pm
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy h:mm:ssa");
				wpdt = sdf.parse(wptType.getCmt());
			} catch (ParseException e) {
			}
			if (wpdt == null) {
				try {
					// 24hr
					SimpleDateFormat sdf = new SimpleDateFormat(
							"dd-MMM-yy h:mm:ss");
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
			
		//TODO: projection
		Waypoint waypoint = new Waypoint();
		if (wpdt != null){
			waypoint.setTime( new Time(wpdt.getTime()) );
			waypoint.setImportedDate(wpdt);
		}
		waypoint.setX(wptType.getLon().doubleValue());
		waypoint.setY(wptType.getLat().doubleValue());
		waypoint.setId(id);
		waypoint.setComment(wptType.getCmt());
		return waypoint;
	}
	
	
	
	/**
	 * 
	 * Reads data from a pgx file.  If dataType is WAYPOINT then
	 * reads all Wpts from the gpx file and returns a list of Waypoints.  If TRACK then
	 * it reads all track points, converts them to coordinates and returns a list of coordinates.
	 * 
	 * <p>
	 * If day is provided then only waypoints or trackpoints that occur on that day are imported.
	 * </p>
	 * 
	 * @param gpxFile the gpx file name 
	 * @param day the day to import data for; if null all data is imported
	 * @param monitor a progress monitor
	 * @return a hashmap that contains a key for each ImportType provided.   
	 * 
	 */
	public static Map<ImportType, Object> convertGpx(File gpxFile, Date day, Set<ImportType> dataType, IProgressMonitor monitor){
		
		HashMap<ImportType, Object> data = new HashMap<ImportType, Object>();		
		Date plddt = null;
		if (day != null){
			plddt = SmartUtils.getDatePart(day, false);
		}
		
		if (dataType.contains(ImportType.WAYPOINT)){
			List<WptType> waypoints = getWaypointsGpx(gpxFile, monitor);
			monitor.subTask("Parsing waypoints.");
			ArrayList<Waypoint> newwaypoints = new ArrayList<Waypoint>();
			for (Iterator<WptType> iterator = waypoints.iterator(); iterator.hasNext();) {
				WptType wptType = (WptType) iterator.next();
				Waypoint newwp = convertWpt(wptType);
				if (plddt == null){
					//import all waypoints regardless of date
					newwaypoints.add(newwp);
				} else if (newwp.getImportedDate() != null){
					//only import waypoints whose imported date match the given date
					if (SmartUtils.getDatePart(newwp.getImportedDate(), false).equals(plddt)){
						newwaypoints.add(newwp);
					}
				}
			}
			data.put(ImportType.WAYPOINT, newwaypoints);
		}
		if (dataType.contains(ImportType.TRACK)){
			monitor.subTask("Parsing tracks.");
			List<Coordinate> trackCoords = new ArrayList<Coordinate>();
		
			List<TrkType> tracks = getTracksGpx(gpxFile, monitor);
			for (TrkType trk : tracks){
				List<TrksegType> segments = trk.getTrkseg();
				for (TrksegType seg: segments){
					List<WptType> trkPnt = seg.getTrkpt();
					for (WptType pnt : trkPnt){
						double y = pnt.getLat().doubleValue();
						double x = pnt.getLon().doubleValue();
						Date datetime = findWaypointDate(pnt);
						
						if (plddt == null){
							//include all
							double time = Double.NaN;
							if (datetime != null){
								time = datetime.getTime();
							}
							Coordinate c = new Coordinate(x, y, time);
							trackCoords.add(c);
						}else if (plddt != null && datetime != null){
							//include only waytpoints which match current date
							if (SmartUtils.getDatePart(datetime, false).equals(plddt)){
								Coordinate c = new Coordinate(x, y, datetime.getTime());
								trackCoords.add(c);
							}
						}
						
					}
				}
			}
			data.put(ImportType.TRACK, trackCoords);
		}
		return data;
	}
	
	/**
	 * Converts a set of coordinates to a track.  Coordinates are first sorted
	 * by date/time.
	 * @param coordinates set of coordinates
	 * @return track
	 */
	public static Track convertToTrack(List<Coordinate> coordinates){
			GeometryFactory gf = new GeometryFactory();
			Collections.sort(coordinates, new Comparator<Coordinate>() {
				@Override
				public int compare(Coordinate o1, Coordinate o2) {
					return ((Double) o1.z).compareTo((Double) o2.z);
				}
			});
			LineString track = gf.createLineString(coordinates
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
