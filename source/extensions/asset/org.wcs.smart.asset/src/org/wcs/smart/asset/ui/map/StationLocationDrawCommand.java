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
package org.wcs.smart.asset.ui.map;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Command for drawing stations and locations on a map.
 * @author Emily
 *
 */
public class StationLocationDrawCommand extends AbstractDrawCommand{


	private static int LOCATION_RADIUS = 3;
	private static int STATION_RADIUS = 2;
	
	private Collection<AssetStation> stationsToDraw = null;
	private Collection<AssetStationLocation> locationsToDraw = null;
	
	private Collection<AssetStationLocation> selectedLocations = new HashSet<>();
	private Collection<AssetStation> selectedStations = new HashSet<>();
	
	private ReferencedEnvelope bounds = null;
	
	public StationLocationDrawCommand() {
	}
		
	public void clearSelections() {
		selectedLocations.clear();
		selectedStations.clear();
	}
	public void setStations(Collection<AssetStation> stations) {
		this.stationsToDraw = stations;
		bounds = null;
	}
	
	public void setLocations(Collection<AssetStationLocation> locations) {
		this.locationsToDraw = locations;
		bounds = null;
	}
	
	public void setStationSelection(Collection<AssetStation> selection) {
		selectedStations.clear();
		selectedStations.addAll(selection);
	}
	
	public void setLocationSelection(Collection<AssetStationLocation> selection) {
		selectedLocations.clear();
		if (selection != null) selectedLocations.addAll(selection);
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws Exception {
		drawStationLocations(graphics, getMap());
	}
	
	@Override
	public Rectangle getValidArea() {
		return null;
	}
	
	public ReferencedEnvelope getBounds() {
		if (bounds != null) return bounds;
		
		ReferencedEnvelope temp = null;
		
		if (stationsToDraw != null) {
			for (AssetStation s : stationsToDraw) {
				
				ReferencedEnvelope re = new ReferencedEnvelope(s.getX(), s.getX(), s.getY(), s.getY(), SmartDB.DATABASE_CRS);
				double offset = findBufferDistance(s.getX(), s.getY(), s.getBuffer());
				re.expandBy(offset);
				
				if (temp == null) {
					temp = re;
				}else {
					temp.expandToInclude(re);
				}
			}
		}
		if (locationsToDraw != null) {
			for (AssetStationLocation s : locationsToDraw) {
				ReferencedEnvelope re = new ReferencedEnvelope(s.getX(), s.getX(), s.getY(), s.getY(), SmartDB.DATABASE_CRS);
				double offset = findBufferDistance(s.getX(), s.getY(), s.getBuffer());
				re.expandBy(offset);
				
				if (temp == null) {
					temp = re;
				}else {
					temp.expandToInclude(re);
				}
			}
		}
		
		
		if (temp == null) {
			bounds = new ReferencedEnvelope();
		}else {
			bounds = temp;
		}
		return bounds;
	}
	
	private Color getStationColor(AssetStation station) {
		if (selectedStations.contains(station)) return Color.YELLOW;
		return Color.RED;
	}
	
	private Color getLocationColor(AssetStationLocation location) {
		if (selectedLocations.contains(location)) return Color.YELLOW;
		return Color.BLUE;
	}
	
	private double findBufferDistance(double x, double y, double bufferM) {
		GeodeticCalculator calc = new  GeodeticCalculator(DefaultGeographicCRS.WGS84);
		calc.setStartingGeographicPoint(x,y);
		calc.setDirection(0.0, bufferM);
		Point2D p2 = calc.getDestinationGeographicPoint();
		calc.setDirection(90.0, bufferM);
		Point2D p3 = calc.getDestinationGeographicPoint();

		double dy = p2.getY() - x;
		double dx = p3.getX() - y;
		double distance = (dy + dx) / 2.0;
		return distance;
	}
	
	private Coordinate reproject(MathTransform transform, Coordinate z) {
		if (transform == null) return z;
		try {
			return JTS.transform(z, null, transform);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return z;
	}
	private void drawStationLocations(ViewportGraphics graphics, IMap map) {
		CoordinateReferenceSystem target = map.getViewportModel().getCRS();
		CoordinateReferenceSystem src = SmartDB.DATABASE_CRS;
		
		MathTransform transform = null;
		if (!CRS.equalsIgnoreMetadata(target, src)) {
			try {
				transform = CRS.findMathTransform(src, target);
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		
		if (stationsToDraw != null) {
			for (AssetStation s : stationsToDraw) {
				Coordinate station = new Coordinate(s.getX(),  s.getY());
				double radius_ll = findBufferDistance(station.x, station.y, s.getBuffer());
				Coordinate bufferpnt = new Coordinate(station.x + radius_ll, station.y + radius_ll);
				
				Point pnt = map.getViewportModel().worldToPixel(reproject(transform, station));
				Point pnt2 = map.getViewportModel().worldToPixel(reproject(transform, bufferpnt));
			
				int r1 = pnt2.x - pnt.x;
				int r2 = pnt2.y - pnt.y;
				int r = Math.max(Math.abs(r1), Math.abs(r2));
				Color drawColor = getStationColor(s);
				if (r > 0) {
					graphics.setColor(new Color(drawColor.getRed(),drawColor.getGreen(),drawColor.getBlue(),40));
					graphics.setBackground(drawColor);
					graphics.fillOval(pnt.x-r, pnt.y - r, r*2, r*2);
					graphics.setColor(Color.RED);
					graphics.drawOval(pnt.x-r, pnt.y - r, r*2, r*2);
				}
				int offset = STATION_RADIUS;
				graphics.setColor(drawColor);
				graphics.setBackground(drawColor);
				graphics.fillOval(pnt.x-offset, pnt.y - offset, offset*2, offset*2);
				graphics.drawOval(pnt.x-offset, pnt.y - offset, offset*2, offset*2);
			}
		}
		
		
		if (locationsToDraw == null) return;
		for (AssetStationLocation loc : locationsToDraw) {
				
			Coordinate location = new Coordinate(loc.getX(),  loc.getY());
			double radius_ll = findBufferDistance(location.x, location.y, loc.getBuffer());
			Coordinate bufferpnt = new Coordinate(location.x + radius_ll, location.y + radius_ll);
			
			Point pnt = map.getViewportModel().worldToPixel(reproject(transform, location));
			Point pnt2 = map.getViewportModel().worldToPixel(reproject(transform, bufferpnt));
			
			int r = Math.max(Math.abs(pnt2.x - pnt.x), Math.abs(pnt2.y - pnt.y));
				
			Color drawColor = getLocationColor(loc);
				
			if (r > 0) {
				graphics.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), 40));
				graphics.setBackground(drawColor);
				graphics.fillOval(pnt.x-r, pnt.y - r, r*2, r*2);
				graphics.setColor(drawColor);
				graphics.drawOval(pnt.x-r, pnt.y - r, r*2, r*2);
			}
			int offset = LOCATION_RADIUS;
			graphics.setColor(drawColor);
			graphics.setBackground(drawColor);
			graphics.fillOval(pnt.x-offset, pnt.y - offset, offset*2, offset*2);
			graphics.drawOval(pnt.x-offset, pnt.y - offset, offset*2, offset*2);
		}
	}
}
