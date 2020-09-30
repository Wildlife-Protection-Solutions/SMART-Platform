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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.gpx.xml.TrkType;
import org.wcs.smart.gpx.xml.TrksegType;
import org.wcs.smart.gpx.xml.WptType;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SharedUtils;

/**
 * Class of utilties that support
 * the importing of waypoints and tracks from a variety 
 * of sources 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ObservationGPSDataImport extends GPSDataImport{

	
	/**
	 * Connects to GPSBabel to get gps data from device
	 * 
	 * @param deviceType the type of gps data
	 * @param day the day to import data for; if null all data is imported
	 *
	 * @return a map of type of data imported 
	 * @throws Exception 
	 */
	public static Map<ImportType, List<Waypoint>> importGpsData(final String deviceType, 
			final LocalDate day, final Set<ImportType> dataType, IProgressMonitor monitor) throws Exception {
		
		final HashMap<ImportType, List<Waypoint>> data = new HashMap<ImportType, List<Waypoint>>();

		monitor.setTaskName(Messages.GPSDataImport_Progress_ImportingFromGPS);
		Path f = importFromDevice(deviceType, dataType);
		try {
			monitor.setTaskName(Messages.GPSDataImport_Progress_ReadingData);
			Map<ImportType, List<Waypoint>> vals = convertGpx(Collections.singletonList(f.toAbsolutePath().toString()), day, dataType, monitor);
			for (ImportType type : dataType) {
				data.put(type, vals.get(type));
			}
		} finally {
			try {
				Files.delete(f);
			} catch (Exception ex) {
				ObservationPlugIn.log("Error deleting patrol data file.", ex); //$NON-NLS-1$
			}
		}

		return data;
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
	 * Converts a gpx waypoint to a smart waypoint.
	 * @param wptType
	 * @return
	 */
	private static Waypoint convertWpt(WptType wptType){
		LocalDateTime wpdt = findWaypointDate(wptType);
		int id = -1;
		try{
			id = Integer.parseInt(wptType.getName());
		}catch (Exception ex){}

		Waypoint waypoint = new Waypoint();
		if (wpdt != null){
			waypoint.setDateTime(wpdt);
		}
		waypoint.setRawX(wptType.getLon().doubleValue());
		waypoint.setRawY(wptType.getLat().doubleValue());
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
	public static Map<ImportType, List<Waypoint>> convertGpx(List<String> gpxFiles, LocalDate day, Set<ImportType> dataType, IProgressMonitor monitor) throws Exception{
		
		HashMap<ImportType, List<Waypoint>> data = new HashMap<ImportType, List<Waypoint>>();		
		LocalDate plddt = day;
		
		for (String file : gpxFiles) {
			Path gpxFile = Paths.get(file);

			if (dataType.contains(ImportType.WAYPOINT)) {
				List<WptType> waypoints = getWaypointsGpx(Collections.singletonList(gpxFile.toAbsolutePath().toString()), monitor);
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
						if (newwp.getDateTime() != null && newwp.getDateTime().toLocalDate().isEqual(plddt)) {
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
							LocalDateTime datetime = findWaypointDate(pnt);

							Waypoint c = new Waypoint();
							try{
								c.setId( Integer.parseInt(pnt.getName()) );
							}catch (Exception ex){}
							c.setRawX(x);
							c.setRawY(y);
							c.setDateTime(datetime);
							c.setComment((trk.getName() == null ? "" : trk.getName()) + (pnt.getName() == null ? "" :  " - " + pnt.getName())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							c.setSourceId(trk.getName());
							if (plddt == null) {
								// include all
								trackCoords.add(c);
							} else if (plddt != null && datetime != null) {
								// include only waytpoints which match current
								// date
								if (datetime.toLocalDate().equals(plddt)) {
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
	public static LineString convertToLineString(List<Waypoint> coordinates){
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
			Long time = SharedUtils.toLongTime(w.getDateTime());
			Coordinate c = new Coordinate(w.getRawX(), w.getRawY(), time);
			cs.add(c);
		}

		LineString track = GeometryFactoryProvider.getFactory().createLineString(cs
				.toArray(new Coordinate[coordinates.size()]));
		return track;
	}
	
}
