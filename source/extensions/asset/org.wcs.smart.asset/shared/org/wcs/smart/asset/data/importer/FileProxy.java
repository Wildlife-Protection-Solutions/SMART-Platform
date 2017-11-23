package org.wcs.smart.asset.data.importer;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;

import com.vividsolutions.jts.geom.Coordinate;

public class FileProxy {

	private Path file;
	
	private Date imageDate;
	private Double x;
	private Double y;
	private Asset asset;
	private AssetStation station;
	private AssetStationLocation location;
	
	private ConservationArea ca;
	
	private Exception processingException;
	
	private List<ActionableWarning> warnings;
	
	private HashMap<String, Object> dataCache = new HashMap<>();
	
	private List<WaypointObservation> rawObservations = new ArrayList<>();
	private List<WaypointObservation> observations = new ArrayList<>();
	
	private List<FileProxy> relations = new ArrayList<>();
	
	private Integer waypoint;
	
	public FileProxy(Path file, ConservationArea ca) {
		this.file = file;
		this.ca = ca;
		warnings = new ArrayList<>();
	}
	
	public Integer getWaypoint() {
		return this.waypoint;
	}
	public boolean setWaypoint(Integer newWaypoint) {
		if (newWaypoint == null) {
			this.waypoint = null;
			return true;
		}
		if (!isValid()) {
			this.waypoint = null;
			return false;
		}
		if (this.waypoint != null) return false;
		this.waypoint = newWaypoint;
		relations.forEach(c->c.setWaypoint(newWaypoint));
		return true;
	}
	
	public void addRelation(FileProxy fp) {
		if (!this.relations.contains(fp)) this.relations.add(fp);
		if (!fp.getRelations().contains(this)) fp.getRelations().add(this);
	}
	
	public void removeRelation(FileProxy fp) {
		this.relations.remove(fp);
		fp.getRelations().remove(this);
	}
	
	public List<FileProxy> getRelations() {
		return this.relations;
	}
	
	
	public void setObservations(List<WaypointObservation> observations) {
		this.observations = observations;
	}
	
	public List<ActionableWarning> getWarnings(){
		return this.warnings;
	}
	
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	
	public void addRawObservation(WaypointObservation wo) {
		this.rawObservations.add(wo);
	}
	
	public List<WaypointObservation> getRawObservations(){
		return this.rawObservations;
	}
	
	public void addWarning(ActionableWarning warning) {
		this.warnings.add(warning);
	}
	
	public void putData(String key, Object value) {
		dataCache.put(key, value);
	}
	public Object getData(String key) {
		return dataCache.get(key);
	}
	
	public void setPosition(Double x, Double y) {
		this.x = x;
		this.y = y;
	}
	
	public Double getX() {
		return this.x;
	}
	
	public Double getY() {
		return this.y;
	}
	
	public Path getFile() {
		return this.file;
	}
	
	public Exception getProcessingException() {
		return this.processingException;
	}
	
	public void setProcessingException(Exception ex) {
		this.processingException = ex;
	}
	public void setImageDate(Date imageDate) {
		this.imageDate = imageDate;
	}
	
	/**
	 * Updates the associated asset, clears the existing linked deployment
	 * 
	 * @param asset
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
//		this.deployment = null;
	}
	
	/**
	 * Updates the associated location & station; clears the existing linked deployment
	 * @param location
	 */
	public void setStationLocation(AssetStationLocation location) {
		this.location = location;
		this.station = location.getStation();
		
		if (this.x == null || this.y == null) {
			x = location.getX();
			y = location.getY();
		}
//		this.deployment = null;
	}
	
	/**
	 * Will only update the station if the station location is null
	 * @param location
	 */
	public void setStation(AssetStation station) {
		if (this.location != null) return;
		
		this.location = null;
		this.station = station;
//		this.deployment = null;
	}
	
	public Asset getAsset() {
		return this.asset;
	}
	
//	public AssetDeployment getDeployment() {
//		return this.deployment;
//	}
//	
	public AssetStation getStation() {
		return this.station;
	}
	
	public AssetStationLocation getStationLocation() {
		return this.location;
	}
	
	public Date getImageDate() {
		return this.imageDate;
	}
	
	public boolean isValid() {
		if (processingException != null) return false;
		if (getAsset() == null) return false;
		if (getImageDate() == null) return false;
//		if (getDeployment() == null) return false;
		if (this.location == null) return false;
		if (this.station == null) return false;
		if (getX() == null) return false;
		if (getY() == null) return false;
		return true;
	}
	
	public String validMessage() {
		if (processingException != null) return processingException.getMessage();
		if (getAsset() == null) return "No Assest found";
		if (getImageDate() == null) return "No date found";
		if (getStation() == null) return "No station found";
		if (getStationLocation() == null) return "No station location found";
//		if (getDeployment() == null) return "No depoyment found";
		if (getX() == null || getY() == null) return "No position found";
		return "";
	}
	
	
	public void updateStationLocation(Session session, Collection<FileProxy> allFiles) {
		updateStationLocationInternal(session, allFiles);
		
		//ensure lazily loaded required fields
		if (location != null) {
			location.getId();
			if (this.x == null || this.y == null) {
				x = location.getX();
				y = location.getY();
			}
		}
		if (station != null) station.getId();
	}
	
	
	private void updateStationLocationInternal(Session session, Collection<FileProxy> allFiles) {
		if (station != null && location != null) {
			if (!location.getStation().equals(station)) {
				//TODO: we have a problem
				station = location.getStation();
			}
			return; //nothing to do
		}
		if (location != null && station == null) {
			this.station = location.getStation();
			return;
		}

		//we have a station & x,y to find location 
		if (station != null && x != null && y != null) {
			updateStationLocation(station, session);
			return;
		}else if (station != null) {
			//we have a station but no position
			return;
		}
		
		//if all we have is a position we first search for a station
		//then a location
		//then a deployment
		if (x != null && y != null) {
			//find a station
			List<AssetStation> stations = QueryFactory.buildQuery(session, AssetStation.class, 
					new Object[] {"conservationArea", ca}).list();
			
			//add any other "new" stations to this search as well
			for (FileProxy p : allFiles) {
				if (!p.equals(this) && p.getStation() != null && !stations.contains(p.getStation())) {
					stations.add(p.getStation());
				}
			}
			AssetStation matching = null;
			Coordinate imagePosition = new Coordinate(x,y);
			Double bestDistance = null;
			for (AssetStation s : stations) {
				try {
					double distance = JTS.orthodromicDistance(imagePosition, new Coordinate(s.getX(), s.getY()), SmartDB.DATABASE_CRS);
					if (bestDistance == null || distance < bestDistance) {
						bestDistance = distance;
						matching = s;
					}
				}catch (Exception ex) {
					//TODO:
					ex.printStackTrace();
				}
			}
			
			double stnBufferM = AssetHibernateManager.getStationBuffer(session, ca);
			//TODO: distance needs to be checked in meters
			if (bestDistance == null || bestDistance > stnBufferM) {
				//this is not within buffer of current locations so we need to create a new one
				matching = null;
			}
			if (matching == null) {
				matching = new AssetStation();
				matching.setAttributeValues(new ArrayList<>());
				matching.setX(x);
				matching.setY(y);
				matching.setConservationArea(ca);
				matching.setId("** NEW STATION **"); //when we save we need to generate valid station ids
				matching.setLocations(new ArrayList<>());
			}
			this.station = matching;
			updateStationLocation(matching, session);
		}
	}
	
	
	private void updateStationLocation(AssetStation station, Session session) {
		AssetStationLocation matching = null;
		Coordinate imagePosition = new Coordinate(x,y);
		Double bestDistance = null;
		for (AssetStationLocation location : station.getLocations()) {
			try {
				double distance = JTS.orthodromicDistance(imagePosition, new Coordinate(location.getX(), location.getY()), SmartDB.DATABASE_CRS);
				if (bestDistance == null || distance < bestDistance) {
					bestDistance = distance;
					matching = location;
				}
			}catch (Exception ex) {
				//TODO:
				ex.printStackTrace();
			}
		}
		
		double stnBufferM = AssetHibernateManager.getStationLocationBuffer(session, ca);
		//TODO: distance needs to be checked in meters
		if (bestDistance == null || bestDistance > stnBufferM) {
			//this is not within buffer of current locations so we need to create a new one
			matching = null;
		}
		
		if (matching == null) {
			matching = new AssetStationLocation();
			matching.setStation(station);
			matching.setAttributeValues(new ArrayList<>());
			matching.setId("** NEW LOCATION **"); //TODO: when we save we need to generate valid id
			matching.setX(x);
			matching.setY(y);
			station.getLocations().add(matching);
		}
		this.location = matching;
	}

}
