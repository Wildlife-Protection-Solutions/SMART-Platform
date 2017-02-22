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
package org.wcs.smart.gpx;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.gpx.xml.GpxType;
import org.wcs.smart.gpx.xml.TrkType;
import org.wcs.smart.gpx.xml.TrksegType;
import org.wcs.smart.gpx.xml.WptType;
import org.wcs.smart.internal.Messages;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Class of utilties that support
 * the importing of waypoints and tracks from 
 * either gpx files or gps devices using GPS Babel
 * 
 * @author Emily
 * @since 1.0.0
 */
public class GPSDataImport {

	/*
	 * gpx metadata link
	 */
	public static final String GPX_METADATA_CLASSES = "org.wcs.smart.gpx.xml"; //$NON-NLS-1$
	
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
	public static File importFromDevice(final String deviceType, final Set<ImportType> dataType) throws Exception {
		File f = GPSBabel.getData(deviceType, dataType);
		return f;
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
				SmartPlugIn.displayLog(MessageFormat.format(
						Messages.GPSDataImport_WaypointError_CouldNotReadFile,
						new Object[]{gpxFile.getAbsolutePath()}) + "\n" + ex.getMessage(), ex); //$NON-NLS-1$
				continue;
			}
		
			if (type == null){
				SmartPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_WaypointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
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
				SmartPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotRead, new Object[]{gpxFile.getAbsolutePath()}) + ex.getLocalizedMessage(), ex);
				continue;
			}

			if (type == null) {
				SmartPlugIn.displayLog(MessageFormat.format(Messages.GPSDataImport_TrackPointError_CouldNotParse, new Object[]{gpxFile.getAbsolutePath()}), null);
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
	 * Finds the waypoint date/time from a given wpttype object
	 * 
	 * @param wptType
	 * @return date associated with waypoint or null if date cannot be determined
	 */
	public static Date findWaypointDate(WptType wptType) {
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
