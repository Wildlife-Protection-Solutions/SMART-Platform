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

import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Model class for asset deployments.   This represents the deployment of the asset to the field. 

 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset_deployment")
public class AssetDeployment extends UuidItem {

	private Asset asset;
	private AssetStation station;

	private Date startDate;
	private Date endDate;
	private List<AssetDeploymentAttributeValue> attributes;
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
	 * @return station
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="station_uuid", referencedColumnName="uuid")
	public AssetStation getStation() {
		return this.station;
	}

	/**
	 * Set the asset station.
	 * 
	 * @param station
	 */
	public void setStation(AssetStation station) {
		this.station = station;
	}


	/**
	 * Get the start_date.
	 * 
	 * @return start_date
	 */
	@Column(name="start_date")
	public Date getStartDate() {
		return this.startDate;
	}
	
	/**
	 * Set the start_date.
	 * 
	 * @param startDate
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}


	/**
	 * Get the end_date.
	 * 
	 * @return end_date
	 */
	@Column(name="end_date")
	public Date getEndDate() {
		return this.endDate;
	}
	
	/**
	 * Set the end_date.
	 * 
	 * @param endDate
	 */
	public void setEndDate(Date endDate) {
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
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.assetDeployment", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<AssetWaypoint> getAssetWaypointSet() {
		return this.waypoints;
	}


	/**
	 * Set the set of the asset_waypoint.
	 * 
	 * @param assetWaypointSet
	 */
	
	public void setAssetWaypointSet(List<AssetWaypoint> waypoints) {
		this.waypoints = waypoints;
	}


}
