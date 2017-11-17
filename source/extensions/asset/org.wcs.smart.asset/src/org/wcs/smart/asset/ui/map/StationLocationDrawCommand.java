package org.wcs.smart.asset.ui.map;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.hibernate.SmartDB;

import com.vividsolutions.jts.geom.Coordinate;

public class StationLocationDrawCommand extends AbstractDrawCommand{


	private static int LOCATION_RADIUS = 3;
	private static int STATION_RADIUS = 2;
	
	private Collection<AssetStation> stationsToDraw = null;
	private Collection<AssetStationLocation> locationsToDraw = null;
	
	private Collection<AssetStationLocation> selectedLocations = new HashSet<>();
	private Collection<AssetStation> selectedStations = new HashSet<>();
	
	private double stationBuffer;
	private double locationBuffer;
	
	private ReferencedEnvelope bounds = null;
	
	public StationLocationDrawCommand() {
		this(0,0);
	}
		
	public StationLocationDrawCommand(double stationBuffer, double locationBuffer) {
		this.stationBuffer = stationBuffer;
		this.locationBuffer = locationBuffer;
	}
	
	public void setBuffers(double stationBuffer, double locationBuffer) {
		this.stationBuffer = stationBuffer;
		this.locationBuffer = locationBuffer;
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
		//TODO;
		return null;
	}
	
	public ReferencedEnvelope getBounds() {
		if (bounds != null) return bounds;
		
		Double xmin = null;
		Double xmax = null;
		Double ymin = null;
		Double ymax = null;
		
		
		if (stationsToDraw != null) {
			for (AssetStation s : stationsToDraw) {
				if (xmin == null || s.getX() < xmin) xmin = s.getX();
				if (ymin == null || s.getY() < ymin) ymin = s.getY();
				if (xmax == null || s.getX() > xmax) xmax = s.getX();
				if (ymax == null || s.getY() > ymax) ymax = s.getY();
			}
		}
		if (locationsToDraw != null) {
			for (AssetStationLocation s : locationsToDraw) {
				if (xmin == null || s.getX() < xmin) xmin = s.getX();
				if (ymin == null || s.getY() < ymin) ymin = s.getY();
				if (xmax == null || s.getX() > xmax) xmax = s.getX();
				if (ymax == null || s.getY() > ymax) ymax = s.getY();
			}
		}
		
		
		if (xmin == null) {
			bounds = new ReferencedEnvelope();
		}else {
			double offset = Math.max(findBufferDistance(xmin, ymin, Math.max(stationBuffer, locationBuffer)), findBufferDistance(xmax, ymax, Math.max(stationBuffer, locationBuffer)));
			bounds = new ReferencedEnvelope(xmin-offset, xmax+offset, ymin-offset, ymax+offset, SmartDB.DATABASE_CRS);
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
	
	private void drawStationLocations(ViewportGraphics graphics, IMap map) {

		if (stationsToDraw != null) {
			for (AssetStation s : stationsToDraw) {
				Coordinate station = new Coordinate(s.getX(),  s.getY());
		
				double radius_ll = findBufferDistance(station.x, station.y, stationBuffer);
		
				Point pnt = map.getViewportModel().worldToPixel(station);
				Point pnt2 = map.getViewportModel().worldToPixel(new Coordinate(station.x + radius_ll, station.y + radius_ll));
			
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
				
			double radius_ll = findBufferDistance(loc.getX(), loc.getY(), locationBuffer);
			Point pnt = map.getViewportModel().worldToPixel(new Coordinate(loc.getX(), loc.getY()));
			Point pnt2 = map.getViewportModel().worldToPixel(new Coordinate(loc.getX() + radius_ll, loc.getY() + radius_ll));
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
