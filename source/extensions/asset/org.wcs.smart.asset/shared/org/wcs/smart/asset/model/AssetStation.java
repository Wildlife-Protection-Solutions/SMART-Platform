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

package org.wcs.smart.asset.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset.Status;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Model class for asset station. An asset station represents a location
 * where an asset was deployed to.  Multiple assets of different types may 
 * be deployed to a single station at different times or at the same time.

 * 
 * @author egouge
 */
@Entity
@Table(name="asset_station", schema="smart")
public class AssetStation extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	public static final int MAX_LENGTH = 128;
			
	private ConservationArea conservationArea;
	private String id;
	private Double x;
	private Double y;
	private Double buffer;
	
	private List<AssetStationAttributeValue> attributes;
	private List<AssetStationLocation> locations;
	
	/*
	 * Status is computed on demand - it is based on the
	 * number of active asset deployments at the
	 * station
	 */
	@Transient
	private Status status;

	/**
	 * Constructor.
	 */
	public AssetStation() {
		
	}

	
	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.conservationArea = ca;
	}


	/**
	 * Get the id.  This id should be unique across the Conservation Area.
	 * 
	 * @return id
	 */
	@Column(name="id")
	public String getId() {
		return this.id;
	}
	
	/**
	 * Set the id.  This id should be unique across the Conservation Area
	 * 
	 * @param id
	 */
	
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the buffer.  
	 * Value to be stored in meters.
	 * The maximum distance between an station location and the station 
	 * position for the asset deployment to be associated with that station
	 * 
	 * @return buffer
	 */
	@Column(name="buffer")
	public Double getBuffer() {
		return this.buffer;
	}
	
	/**
	 * Set the buffer.  This id should be unique across the Conservation Area
	 * 
	 * @param buffer
	 */
	
	public void setBuffer(Double buffer) {
		this.buffer = buffer;
	}
	
	/**
	 * Get the longitude position value
	 * 
	 * @return x
	 */
	@Column(name="x")
	public Double getX() {
		return this.x;
	}

	/**
	 * Set the longitude position value.
	 * 
	 * @param x
	 */
	public void setX(Double x) {
		this.x = x;
	}

	/**
	 * Get the latitude position value
	 * 
	 * @return y
	 */
	@Column(name="y")
	public Double getY() {
		return this.y;
	}
	
	/**
	 * Set the latitude position value
	 * 
	 * @param y
	 */
	public void setY(Double y) {
		this.y = y;
	}

	/**
	 * Get the set of the asset_station_attribute_value.
	 * 
	 * @return The set of asset_station_attribute_value
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.station", orphanRemoval=true, cascade= {CascadeType.ALL})
	public List<AssetStationAttributeValue> getAttributeValues() {
		return this.attributes;
	}


	/**
	 * Set the set of the asset_station_attribute_value.
	 * 
	 * @param attributes
	 */
	public void setAttributeValues(List<AssetStationAttributeValue> attributes) {
		this.attributes = attributes;
	}


	/**
	 * Get the set of the locations associated with the station.
	 * 
	 * @return 
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="station", orphanRemoval=true, cascade= {CascadeType.REMOVE})
	public List<AssetStationLocation> getLocations() {
		return this.locations;
	}


	/**
	 * Set the set of the locations associated with the station
	 * 
	 * @param locations
	 */
	public void setLocations(List<AssetStationLocation> locations) {
		this.locations = locations;
	}
	
	
	/**
	 * Get the status value;  will returned
	 * cached value if applicable. 
	 * 
	 * @return status
	 */
	@Transient
	public Status getCachedStatus() {
		if (this.status == null) {
			throw new IllegalStateException("Status not yet computed.  You must call computeStatus(session) before you can retreive the status."); //$NON-NLS-1$
		}
		return this.status;
	}
	
	
	/**
	 * Computes the item status
	 * @param session
	 */
	public void computeStatus(Session session) {
		if (getUuid() == null) {
			status = Status.INACTIVE;
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		String query = "SELECT count(*) FROM AssetDeployment d join d.stationLocation c WHERE c.station = :station and d.startDate <= :now1 and (d.endDate is null or d.endDate >= :now2)"; //$NON-NLS-1$

		Long activeDeployments = session.createQuery(query, Long.class)
				.setParameter("now1",  now) //$NON-NLS-1$
				.setParameter("now2",  now) //$NON-NLS-1$
				.setParameter("station",  this) //$NON-NLS-1$
				.uniqueResult();
		if (activeDeployments == 0) {
			status = Status.INACTIVE;
		}else {
			status = Status.ACTIVE;
		}
	}
	
	/**
	 * Searches for all the current active deployment for the asset
	 * station location
	 * @param session
	 * @return empty list of there are no deployments
	 */
	@Transient
	public List<AssetDeployment> getActiveDeployments(Session session) {
		LocalDateTime now = LocalDateTime.now();
		
		String query = "SELECT d FROM AssetDeployment d join d.stationLocation c WHERE c.station = :station and d.startDate <= :now and (d.endDate is null or d.endDate >= :now2)"; //$NON-NLS-1$
		List<AssetDeployment> ad = session.createQuery(query, AssetDeployment.class)
				.setParameter("station",  this) //$NON-NLS-1$
				.setParameter("now",  now) //$NON-NLS-1$
				.setParameter("now2", now) //$NON-NLS-1$
				.list();
		return ad;
		
	}
}
