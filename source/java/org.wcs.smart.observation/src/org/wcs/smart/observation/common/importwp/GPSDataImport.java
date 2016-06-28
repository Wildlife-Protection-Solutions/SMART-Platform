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
package org.wcs.smart.observation.common.importwp;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.common.gpx.GpxType;
import org.wcs.smart.observation.common.gpx.TrkType;
import org.wcs.smart.observation.common.gpx.TrksegType;
import org.wcs.smart.observation.common.gpx.WptType;
import org.wcs.smart.observation.common.importwp.gpsbabel.GPSBabel;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SharedUtils;

import com.vividsolutions.jts.geom.Coordinate;
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
	private static final String GPX_METADATA_CLASSES = "org.wcs.smart.observation.common.gpx"; //$NON-NLS-1$
	
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
				ObservationPlugIn.log("Error deleting patrol data file.", ex); //$NON-NLS-1$
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
					Messages.GPSDataImport_TrackError_CouldNotReadFile, new Object[]{gpxFile.getAbsolutePath()}) + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		if (type == null){
			throw new Exception(MessageFormat.format(Messages.GPSDataImport_TrackError_CouldNotParseFile, new Object[]{ gpxFile.getAbsolutePath()}), null);
		}
		
		monitor.subTask(Messages.GPSDataImport_Progress_ParsingTracks);
		List<TrkType> tracks = type.getTrk();
		return tracks;
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
				ObservationPlugIn.displayLog(MessageFormat.format(
						Messages.GPSDataImport_WaypointError_CouldNotReadFile,
						new Object[]{gpxFile.getAbsolutePath()}) + "\n" + ex.getMessage(), ex); //$NON-NLS-1$
				continue;
			}
		
			if (type == null){
				ObservationPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_WaypointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
				continue;
			}
			monitor.subTask(Messages.GPSDataImport_Progress_ParsingWaypoints);
			waypoints.addAll(type.getWpt());
		}
		
		
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
				ObservationPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotRead, new Object[]{gpxFile.getAbsolutePath()}) + ex.getLocalizedMessage(), ex);
				continue;
			}

			if (type == null) {
				ObservationPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
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
			plddt = SharedUtils.getDatePart(day, false);
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
						if (newwp.getDateTime() != null && SharedUtils.getDatePart(newwp.getDateTime(), false).equals(plddt)) {
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
							c.setSourceId(trk.getName());
							if (plddt == null) {
								// include all
								trackCoords.add(c);
							} else if (plddt != null && datetime != null) {
								// include only waytpoints which match current
								// date
								if (SharedUtils.getDatePart(datetime, false)
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
	public static LineString convertToLineString(List<Waypoint> coordinates, TimeZone timeZone){
		if (coordinates.size() < 2) {
			return null;
		}
		
		Collections.sort(coordinates, new Comparator<Waypoint>() {
			@Override
			public int compare(Waypoint o1, Waypoint o2) {
				if (o1.getDateTime() == null || o2.getDateTime() == null){
					throw new RuntimeException(Messages.GPSDataImport_DateTimeRequired);
				}
				return o1.getDateTime().compareTo(o2.getDateTime());
			}
		});

		List<Coordinate> cs = new ArrayList<Coordinate>();
		for (Waypoint w : coordinates) {
			Calendar c1 = Calendar.getInstance();
			c1.setTimeInMillis(w.getDateTime().getTime());
			Calendar c2 = Calendar.getInstance();
			c2.setTimeZone(timeZone);
			c2.setTimeInMillis(0);
			c2.set(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH),
					c1.get(Calendar.DATE), c1.get(Calendar.HOUR_OF_DAY),
					c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));

			Coordinate c = new Coordinate(w.getX(), w.getY(), c2.getTime()
					.getTime());
			cs.add(c);
		}

		LineString track = GeometryFactoryProvider.getFactory().createLineString(cs
				.toArray(new Coordinate[coordinates.size()]));
		return track;
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
