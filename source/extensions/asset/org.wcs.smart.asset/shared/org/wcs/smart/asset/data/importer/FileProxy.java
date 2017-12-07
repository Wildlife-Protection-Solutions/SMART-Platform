/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.importer;


import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Wrapper around asset file to track details computed
 * about files.
 * @author Emily
 *
 */
public class FileProxy extends ISmartAttachment{

	private Path file;
	
	private Date imageDate;
	private Double x;
	private Double y;
	private Asset asset;
	private AssetStation station;
	private AssetStationLocation location;
	
	private ConservationArea ca;
	
	private List<ActionableWarning> warnings;
	
	//map for caching processing data
	private HashMap<String, Object> dataCache = new HashMap<>();
	//raw observations in the metadata file
	private List<WaypointObservation> rawObservations = new ArrayList<>();
	//cleaned observation (combined where they can be combined etc).
	private List<WaypointObservation> observations = new ArrayList<>();
	//all related files that are computed automatically (this will form a single waypoint)
	private Set<FileProxy> relations = new HashSet<>();
	//user defined relations - files can have either auto
	//defined (relations) or fixedRelations but not both
	private Set<FileProxy> fixedRelations = null;
	//all relations will have the same group - this is number for displaying to the user
	private Integer incidentGroup;
	
	/**
	 * New file proxy
	 * @param file the file
	 * @param ca the conservation area
	 */
	public FileProxy(Path file, ConservationArea ca) {
		super.attachmentFile = file.toFile();
		super.setFilename(file.getFileName().toString());
		this.file = file;
		this.ca = ca;
		warnings = new ArrayList<>();
	}
	
	/**
	 * Gets the incident group 
	 * 
	 * @return
	 */
	public Integer getIncidentGroup() {
		return this.incidentGroup;
	}
	
	/**
	 * Sets the incident group.  If the incident group is already set
	 * for this file, this will not update it and return false.  Otherwise
	 * it will update the group for this proxy and all related proxies, then
	 * return true.  If the proxy is not valid then this will not
	 * set the group and false will be returned.
	 * 
	 * If group is null, the incident group for this proxy will be updated to null,
	 * but relations will not be updated.
	 * 
	 * @param group
	 * @return
	 */
	public boolean setIncidentGroup(Integer group) {
		if (group == null) {
			this.incidentGroup = null;
			return true;
		}
		if (!isValid()) {
			this.incidentGroup = null;
			return false;
		}
		if (this.incidentGroup != null) return false;
		this.incidentGroup = group;
		if (fixedRelations != null) {
			fixedRelations.forEach(c->c.setIncidentGroup(group));
			return true;
		}
		relations.forEach(c->c.setIncidentGroup(group));
		return true;
	}
	
	/**
	 * Configures if this file is part of a user defined group.
	 * 
	 * Set to null to remove this as a fixed relation.  When setting to
	 * null it will remove this item from all fixed relations references.
	 * 
	 * Set to new collection to defined fixed relation, removing any existing
	 * relations.
	 * 
	 * @param relations
	 */
	public void setFixedRelations(Collection<FileProxy> relations) {
		if (relations == null) {
			if (fixedRelations != null) {
				for (FileProxy f : fixedRelations)  f.fixedRelations.remove(this);
			}
			this.fixedRelations = null;
			return;
		}
		
		this.fixedRelations = new HashSet<>();
		fixedRelations.addAll(relations);
		fixedRelations.remove(this);
		for (FileProxy p : this.relations) {
			p.getRelations().remove(this);
			if (p.fixedRelations != null) p.fixedRelations.add(this);
		}
		this.relations.clear();
	}
	
	/**
	 * If this is part of a user defined group.
	 * @return
	 */
	public boolean isFixed() {
		return this.fixedRelations != null;
	}
	
	/**
	 * Add a relation to this proxy.  If this proxy is part of a 
	 * fixed relation this will not add it.  Otherwise it will add both 
	 * parts of the relation.  If the fp to add relation is also fixed it will also 
	 * not get added
	 * 
	 * Relations will no contain itself.
	 * 
	 * @param fp
	 */
	public void addRelation(FileProxy fp) {
		if (this.fixedRelations != null)  return; //this is a fixed relation don't add me
		if (fp.fixedRelations != null) return; //fp is also fixed we should not be configuring relation

		for (FileProxy p : fp.relations) {
			p.relations.add(this);
			this.relations.add(p);
		}
		for (FileProxy p : this.relations) {
			p.relations.add(fp);
			fp.relations.add(p);	
		}

		this.relations.add(fp);
		fp.relations.add(this);
		
		//remove myself from all relations
		if (relations.contains(this)) relations.remove(this);
		for (FileProxy p : relations) {
			if (p.relations.contains(p)) p.relations.remove(p);
		}
	}
	
	/**
	 * Get all related proxies.
	 * 
	 * @return
	 */
	public Set<FileProxy> getRelations() {
		return this.relations;
	}
	
	/**
	 * Get the fixed (use defined) related proxies.  Will be null
	 * if not defined.
	 * @return
	 */
	public Set<FileProxy> getFixedRelations() {
		return this.fixedRelations;
	}
	
	/**
	 * Sets the cleaned up observation associated with the proxy
	 * @param observations
	 */
	public void setObservations(List<WaypointObservation> observations) {
		this.observations = observations;
	}
	
	/**
	 * Get the cleaned up observations associated with 
	 * this proxy.
	 *   
	 * @return
	 */
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	
	/**
	 * 
	 * @return list of warnings generated during processing
	 */
	public List<ActionableWarning> getWarnings(){
		return this.warnings;
	}
	
	/**
	 * Add a raw observation.  This is parsed directly
	 * from metadata and metadata mappings.
	 * @param wo
	 */
	public void addRawObservation(WaypointObservation wo) {
		this.rawObservations.add(wo);
	}
	
	/**
	 * Gets all raw observations.
	 * @return
	 */
	public List<WaypointObservation> getRawObservations(){
		return this.rawObservations;
	}
	
	/**
	 * Add a warning
	 * @param warning
	 */
	public void addWarning(ActionableWarning warning) {
		this.warnings.add(warning);
	}
	
	/**
	 * For storing user data
	 * @param key
	 * @param value
	 */
	public void putData(String key, Object value) {
		dataCache.put(key, value);
	}
	/**
	 * Gets associated user data
	 * @param key
	 * @return
	 */
	public Object getData(String key) {
		return dataCache.get(key);
	}
	
	/**
	 * Sets the position associated with the file
	 * @param x
	 * @param y
	 */
	public void setPosition(Double x, Double y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Get the x location 
	 * @return
	 */
	public Double getX() {
		return this.x;
	}
	
	/**
	 * Get the y location
	 * @return
	 */
	public Double getY() {
		return this.y;
	}
	
	/**
	 * Get the file name
	 * @return
	 */
	public Path getFile() {
		return this.file;
	}
	
	/**
	 * Get the image date
	 * @param imageDate
	 */
	public void setImageDate(Date imageDate) {
		this.imageDate = imageDate;
	}
	
	/**
	 * Updates the associated asset
	 * 
	 * @param asset
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}
	
	/**
	 * Updates the associated location & station.  If
	 * no (x,y) is defined for the file, this will
	 * update the (x,y) to the location position. 
	 * 
	 * @param location
	 */
	public void setStationLocation(AssetStationLocation location) {
		this.location = location;
		this.station = location.getStation();
		
		if (this.x == null || this.y == null) {
			x = location.getX();
			y = location.getY();
		}
	}
	
	/**
	 * Updates the station and sets the location to null, if
	 * the location is null to begin with.
	 * 
	 * @param location
	 */
	public void setStation(AssetStation station) {
		if (this.location != null) return;
		
		this.location = null;
		this.station = station;
	}
	
	/**
	 * Gets the asset
	 * @return
	 */
	public Asset getAsset() {
		return this.asset;
	}
	
	/**
	 * Get the asset station
	 * @return
	 */
	public AssetStation getStation() {
		return this.station;
	}
	
	/**
	 * Get the asset station location
	 * @return
	 */
	public AssetStationLocation getStationLocation() {
		return this.location;
	}
	
	/**
	 * Get the image date
	 * @return
	 */
	public Date getImageDate() {
		return this.imageDate;
	}
	
	/**
	 * Determines if all required information has been provided, for the file to be loaded.
	 * @return
	 */
	public boolean isValid() {
		if (getAsset() == null) return false;
		if (getImageDate() == null) return false;
		if (this.location == null) return false;
		if (this.station == null) return false;
		if (getX() == null) return false;
		if (getY() == null) return false;
		return true;
	}
	
	/**
	 * If not valid, returns a message identifying the missing data
	 * @return
	 */
	public String validMessage() {
		if (getAsset() == null) return "No Assest found";
		if (getImageDate() == null) return "No date found";
		if (getStation() == null) return "No station found";
		if (getStationLocation() == null) return "No station location found";
		if (getX() == null || getY() == null) return "No position found";
		return "";
	}
	
	/**
	 * Computes the location/station for the proxy item.
	 * 
	 * allFiles is provided to ensure that only a single new station is 
	 * created in the case where multiple files at the same location 
	 * are imported. 
	 * 
	 * @param session 
	 * @param allFiles all other files also processing
	 * 
	 */
	
	public void updateStationLocation(Session session, FileProcessor processor) {
		updateStationLocationInternal(session, processor);
		
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
	
	/*
	 * Updates the station location
	 */
	private void updateStationLocationInternal(Session session, FileProcessor processor) {
		if (station != null && location != null) {
			if (!location.getStation().equals(station)) {
				//update station to correct location value
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
			updateStationLocation(station, session, processor);
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
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			
			//add any other "new" stations to this search as well
			for (FileProxy p : processor.getFileDetails()) {
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
					ex.printStackTrace();
				}
			}
			
			double stnBufferM = AssetHibernateManager.getStationBuffer(session, ca);
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
				//when we save we need to generate valid station ids
				matching.setId(MessageFormat.format("** NEW STATION {0} **", processor.NewObjectCounter.getAndIncrement())); 
				matching.setLocations(new ArrayList<>());
			}
			this.station = matching;
			updateStationLocation(matching, session, processor);
		}
	}
	
	
	private void updateStationLocation(AssetStation station, Session session, FileProcessor processor) {
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
				ex.printStackTrace();
			}
		}
		
		double stnBufferM = AssetHibernateManager.getStationLocationBuffer(session, ca);
		if (bestDistance == null || bestDistance > stnBufferM) {
			//this is not within buffer of current locations so we need to create a new one
			matching = null;
		}
		
		if (matching == null) {
			matching = new AssetStationLocation();
			matching.setStation(station);
			matching.setAttributeValues(new ArrayList<>());
			//when we save we set a valid id
			matching.setId(MessageFormat.format("** NEW LOCATION {0} **", processor.NewObjectCounter.getAndIncrement())); 
			matching.setX(x);
			matching.setY(y);
			station.getLocations().add(matching);
		}
		this.location = matching;
	}

	@Override
	protected String getDatastoreFolderPath(Session session) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	
}
