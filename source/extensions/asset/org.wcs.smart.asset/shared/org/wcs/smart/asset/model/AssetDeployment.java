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
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Model class for asset deployments.   This represents the deployment of the asset to the field. 

 * 
 * @author egouge
 */
@Entity
@Table(name="asset_deployment", schema="smart")
public class AssetDeployment extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	private Asset asset;
	private AssetStationLocation location;

	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private List<AssetDeploymentAttributeValue> attributes;
	private List<AssetDeploymentDisruption> disruptions;

	private List<AssetWaypoint> waypoints;
	
	private byte[] track;


	/**
	 * Constructor.
	 */
	public AssetDeployment() {
	}

	/**
	 * Get the asset.
	 * 
	 * @return asset
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="asset_uuid", referencedColumnName="uuid")
	public Asset getAsset() {
		return this.asset;
	}
	
	/**
	 * Set the asset.
	 * 
	 * @param asset
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	/**
	 * Get the asset station
	 * 
	 * @return location
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="station_location_uuid", referencedColumnName="uuid")
	public AssetStationLocation getStationLocation() {
		return this.location;
	}

	/**
	 * Set the asset station.
	 * 
	 * @param location
	 */
	public void setStationLocation(AssetStationLocation location) {
		this.location = location;
	}


	/**
	 * Get the start_date.
	 * 
	 * @return start_date
	 */
	@Column(name="start_date")
	public LocalDateTime getStartDate() {
		return this.startDate;
	}
	
	/**
	 * Set the start_date.
	 * 
	 * @param startDate
	 */
	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}


	/**
	 * Get the end_date.
	 * 
	 * @return end_date
	 */
	@Column(name="end_date")
	public LocalDateTime getEndDate() {
		return this.endDate;
	}
	
	/**
	 * Set the end_date.
	 * 
	 * @param endDate
	 */
	public void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}


	/**
	 * Get the track.
	 * 
	 * @return track
	 */
	@Column(name="track")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	public byte[] getTrack() {
		return this.track;
	}


	/**
	 * Set the track.
	 * 
	 * @param track
	 */
	public void setTrack(byte[] track) {
		this.track = track;
	}


	/**
	 * Get the set of disruptions associated with this deployment
	 * 
	 * @return The set of asset_deployment_attribute_value
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="assetDeployment", 
			orphanRemoval = true, cascade={CascadeType.ALL})
	public List<AssetDeploymentDisruption> getDisruptions() {
		return this.disruptions;
	}
	
	/**
	 * Set the set of the asset disruptions collection.
	 * 
	 * @param disruptions
	 */
	public void setDisruptions(List<AssetDeploymentDisruption> disruptions) {
		this.disruptions = disruptions;
	}
	
	/**
	 * Get the set of the asset_deployment_attribute_value.
	 * 
	 * @return The set of asset_deployment_attribute_value
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.assetDeployment", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<AssetDeploymentAttributeValue> getAttributeValues() {
		return this.attributes;
	}
	
	/**
	 * Set the set of the asset_deployment_attribute_value.
	 * 
	 * @param assetDeploymentAttributeValueSet
	 *            The set of asset_deployment_attribute_value
	 */
	public void setAttributeValues(List<AssetDeploymentAttributeValue> attributeValues) {
		this.attributes = attributeValues;
	}

	
	/**
	 * Get the set of the waypoints associated with this deployment
	 * 
	 * @return The set of asset_waypoint
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="assetDeployment", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<AssetWaypoint> getAssetWaypoints() {
		return this.waypoints;
	}


	/**
	 * Set the set of the asset_waypoint.
	 * 
	 * @param assetWaypointSet
	 */
	
	public void setAssetWaypoints(List<AssetWaypoint> waypoints) {
		this.waypoints = waypoints;
	}

	/**
	 * Computes the time in field of this asset deployment minus the disruption times.
	 * This is computed as the number of seconds from the start date to either the current time
	 * or the end time (whichever is older) minus all disruptions piror to current time.
	 * Future dates/disruption are not included in the active time.
	 * 
	 * @return
	 */
	@Transient
	public double getActiveTimeOutInSeconds() {
		double out = getTimeOutInSeconds();
		for (AssetDeploymentDisruption d : getDisruptions()) {
			out = out - d.getActiveTimeInSeconds();
		}
		return out;
	}
	
	/**
	 * Computes the time in field of this asset deployment. This is computed
	 * as the number of seconds from the start date to either the current time
	 * or the end time (whichever is older).  So future dates are not
	 * included in the active time.
	 * @return
	 */
	@Transient
	public double getTimeOutInSeconds() {
		
		LocalDateTime now = LocalDateTime.now();
		if (getStartDate().isAfter(now)) return 0;
		
		LocalDateTime start = getStartDate();
		LocalDateTime end = now;
		
		if (getEndDate() != null && getEndDate().isBefore(now)) {
			end = getEndDate();
		}
		
		return ChronoUnit.MILLIS.between(start, end) / 1000.0;
	}

	/**
	 * If the end date is in the future this returns the number
	 * of seconds to the end date otherwise it returns 0;
	 * @return
	 */
	@Transient
	public double getTimeToEndDate() {
		if (getEndDate() == null) return 0;
		
		LocalDateTime now = LocalDateTime.now();
		if (getEndDate().isBefore(now)) return 0;
		
		return ChronoUnit.MILLIS.between(getEndDate(), now) / 1000.0;
	}
}
