package org.wcs.smart.asset.data.importer;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

import com.vividsolutions.jts.geom.Coordinate;

public class FileProxy {

	private Path file;
	
	private Date imageDate;
	private Double x;
	private Double y;
	private AssetDeployment deployment;
	private Asset asset;
	private AssetStation station;
	private AssetStationLocation location;
	
	private ConservationArea ca;
	
	private Exception processingException;
	
	public FileProxy(Path file, ConservationArea ca) {
		this.file = file;
		this.ca = ca;
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
	
	public void setAsset(Asset asset) {
		this.asset = asset;
	}
	
	public void setStationLocation(AssetStationLocation location) {
		this.location = location;
	}
	
	public Asset getAsset() {
		return this.asset;
	}
	public AssetDeployment getDeployment() {
		return this.deployment;
	}
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
		if (getDeployment() == null) return false;
		return true;
	}
	
	public String validMessage() {
		if (processingException != null) return processingException.getMessage();
		if (getAsset() == null) return "No Assest found";
		if (getImageDate() == null) return "No date found";
		if (getStation() == null) return "No station found";
		if (getStationLocation() == null) return "No station location found";
		if (getDeployment() == null) return "No depoyment found";
		return "";
	}
	
	
	public void updateAssetDeployment(Session session) {
		AssetDeployment deploy = findAssetDepolyment(session);
		if (deploy == null) {
			this.deployment = null;
		}else {
			if (!deploy.getAsset().equals(asset)) throw new IllegalStateException("Asset Deployment found does not match asset identified in the file");
			this.deployment = deploy;
			this.location = deploy.getStationLocation();
			this.station = deploy.getStationLocation().getStation();
		}
	}
	
	
	private AssetDeployment findAssetDepolyment(Session session) {
		//an asset is required in all cases
		if (asset == null) return null;
		
		//if we have an asset deployment we are done
		if (deployment != null) return deployment;
		
		//if we have a location; then lets search for a deployment
		//at this location
		if (location != null) {
			return findDeployment(location, session);
		}
		
		//if we have a station; but no location we need to 
		//find or create a location then find or create a deployment
		
		//we need an x&y to find a location
		if (station != null && x != null && y != null) {
			return findDeployment(station, session);
		}else if (station != null) {
			//we have a station but no position
		}
		
		//if all we have is a position we first search for a station
		//then a location
		//then a deployment
		if (x != null && y != null) {
			//find a station
			List<AssetStation> stations = QueryFactory.buildQuery(session, AssetStation.class, 
					new Object[] {"conservationArea", ca}).list();
			
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
				matching.setId("GENERATE ID");
				matching.setLocations(new ArrayList<>());
						
			}
			return findDeployment(matching, session);
		}
		return null;
		
	}
	
	private AssetDeployment findDeployment(AssetStation station, Session session) {
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
			matching.setId("GNEERATE IDTODO:");
			matching.setX(x);
			matching.setX(y);
			station.getLocations().add(matching);
		}
		
		return findDeployment(matching, session);
	}

		
	private AssetDeployment findDeployment(AssetStationLocation location, Session session) {
		List<AssetDeployment> currentDeployments = new ArrayList<>();
		if (location.getUuid() != null) {
			currentDeployments = QueryFactory.buildQuery(session, AssetDeployment.class, 
				new Object[] {"asset", asset},
				new Object[] {"stationLocation", location},
				new Object[] {"endDate", null}).list();
		}
		
		if (currentDeployments.size() > 1) {
			//this is an error 
			throw new IllegalStateException(MessageFormat.format("Multiple deployments active for asset {0}. This state is not valid", asset.getId() ));
		}else if (currentDeployments.size() == 1) {
			return currentDeployments.get(0);
		}else {
			//we need to create a new deployments
			//TODO: we need to configure the start date correctly
			AssetDeployment newDeployment = new AssetDeployment();
			newDeployment.setAsset(asset);
			newDeployment.setAssetWaypoints(new ArrayList<>());
			newDeployment.setAttributeValues(new ArrayList<>());
			newDeployment.setStartDate(new Date());
			newDeployment.setEndDate(null);
			newDeployment.setStationLocation(location);
			return newDeployment;
		}
	}
}
