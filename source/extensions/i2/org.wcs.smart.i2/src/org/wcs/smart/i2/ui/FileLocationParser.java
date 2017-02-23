/*   
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.gpx.GPSDataImport;
import org.wcs.smart.gpx.xml.WptType;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.dialogs.WptTypeSelectionDialog;
import org.wcs.smart.map.GeometryFactoryProvider;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Parses locations from attachment files.  For images it reads image metadata
 * for gpx files it reads gpx data and gives users options to select locations.
 *  
 * @author Emily
 *
 */
public enum FileLocationParser {

	INSTANCE;
	
	/**
	 * Attempts to parse locations from a files.  Checks both gpx and image formats.
	 * @param f
	 * @return
	 */
	public List<IntelLocation> parseFile(File f){
		List<IntelLocation> locations = new ArrayList<IntelLocation>();
		locations.addAll(parseFromGpx(f, null));
		locations.addAll(parseFromImage(f));
		return locations;
	}
	
	/**
	 * Parses location from the gpx file.  Resulting locations will have the
	 * geometry, comment, id and datetime set.  Filename must end with .gpx.
	 * 
	 * @param gpxFile
	 * @return
	 */
	public List<IntelLocation> parseFromGpx(File gpxFile, IProgressMonitor monitor){
		if (monitor == null) monitor = new NullProgressMonitor();
		if (!gpxFile.getName().toLowerCase().endsWith(".gpx")) return Collections.emptyList();
		List<IntelLocation> locations = new ArrayList<IntelLocation>();
		try{
			List<WptType> waypoints = GPSDataImport.getWaypointsGpx(Collections.singletonList(gpxFile.getAbsolutePath()), monitor);
			
			List<WptType> selectedPoints = new ArrayList<WptType>();
			Display.getDefault().syncExec(()->{
				WptTypeSelectionDialog dialog = new WptTypeSelectionDialog(Display.getDefault().getActiveShell(), waypoints, MessageFormat.format("Import locations from gpx file: {0}", gpxFile.getName()));
				if (dialog.open() == Window.OK){
					selectedPoints.addAll(dialog.getWaypoints());
				}
			});
			for (WptType wp : selectedPoints){
				IntelLocation l = new IntelLocation();
				Point pnt = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getLon().doubleValue(), wp.getLat().doubleValue()));
				Date dt = GPSDataImport.findWaypointDate(wp);
				if (dt == null) dt = new Date();
				l.setGeometry(pnt);
				l.setDateTime(dt);
				l.setId(wp.getName());
				if (wp.getCmt() != null && !"null".equals(wp.getCmt().toLowerCase())){
					l.setComment(wp.getCmt());
				}
				locations.add(l);
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return locations;
	}
	
	
	/**
	 * Parses a list of locations from an image file.  Exceptions are consumed and an empty list returned.  
	 * Resulting locations will have the datetime and geometry set.
	 * 
	 * @param imageFile
	 * @return
	 */
	public List<IntelLocation> parseFromImage(File imageFile){
		List<IntelLocation> locations = new ArrayList<IntelLocation>();
		try{
			Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
			for (Directory directory : metadata.getDirectoriesOfType(GpsDirectory.class)) {
				GeoLocation geoLocation = ((GpsDirectory)directory).getGeoLocation();
				if (geoLocation != null){
					Date dateTime = ((GpsDirectory) directory).getGpsDate();
					Point pnt = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(geoLocation.getLongitude(), geoLocation.getLatitude()));
					
					IntelLocation l = new IntelLocation();
					l.setGeometry(pnt);
					l.setDateTime(dateTime);
					locations.add(l);
					break;
				}
				
			}
		}catch (Exception ex){
			//cannot create thumbnail
			//ex.printStackTrace();
		}
		return locations;
	}
}
