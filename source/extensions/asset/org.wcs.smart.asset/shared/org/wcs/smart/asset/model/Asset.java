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
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Model class of asset. These represent the individual asset units (cameras, drones, etc.). 
 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset")
public class Asset extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	public static final int ID_MAX_LENGTH = 128;
	
	/*
	 * The order here matters as it is used for sort order 
	 * 
	 */
	public enum Status{
		ACTIVE,
		INACTIVE,
		RETIRED;
		
		public String getGuiName(Locale l) {
			return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(this, l);
		}
	}
	
	private String id;
	private AssetType assetType;
	private boolean isRetired;
	private Status status;

	private List<AssetAttributeValue> assetAttributes;
	private ConservationArea conservationArea;
	

	/**
	 * Constructor.
	 */
	public Asset() {
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
	 * Get the asset type
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="asset_type_uuid", referencedColumnName="uuid")
	public AssetType getAssetType() {
		return this.assetType;
	}
	
	/**
	 * Sets the asset type
	 * @param assetType
	 */
	public void setAssetType(AssetType assetType) {
		this.assetType = assetType;
	}
	
	/**
	 * Set the unique id.
	 * 
	 * @param id
	 *            id
	 */
	@Column(name = "id")
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get the unique id.
	 * 
	 * @return id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Gets the asset attribute values
	 * 
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.asset", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<AssetAttributeValue> getAttributeValues() {
		return this.assetAttributes;
	}
	
	/**
	 * Set the asset attribute values
	 * @param assetAttributes
	 * 
	 */
	public void setAttributeValues(List<AssetAttributeValue> assetAttributes) {
		this.assetAttributes = assetAttributes;
	}

	
	/**
	 * Get the is_retired.
	 * 
	 * @return is_retired
	 */
	@Column(name="is_retired")
	public Boolean getIsRetired() {
		return this.isRetired;
	}
	
	/**
	 * Set the is_retired.
	 * 
	 * @param isRetired
	 */
	public void setIsRetired(Boolean isRetired) {
		this.isRetired = isRetired;
	}
	
	/**
	 * Get the status value; recomputing from database
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
	 * Compute the status of the asset
	 */
	public void computeStatus(Session session) {
		if (getUuid() == null) {
			status = Status.INACTIVE;
			return;
		}
		if (getIsRetired()) {
			status = Status.RETIRED;
			return;
		}
		
		Date now = new Date();
		String sql = "FROM AssetDeployment WHERE asset = :asset and startDate <= :now and (endDate is null or endDate >= :now2)"; //$NON-NLS-1$
		List<AssetDeployment> ad = session.createQuery(sql, AssetDeployment.class)
				.setParameter("asset",  this) //$NON-NLS-1$
				.setParameter("now",  now) //$NON-NLS-1$
				.setParameter("now2", now) //$NON-NLS-1$
				.list();

		if (ad.isEmpty()) {
			status = Status.INACTIVE;
		}else {
			status = Status.ACTIVE;
		}
	}
	
	/**
	 * Searches for the current active deployment for the sensor
	 * @param session
	 * @return null if there is not active deployment
	 */
	@Transient
	public AssetDeployment findActiveDeployment(Session session) {
		Date now = new Date();
		String sql = "FROM AssetDeployment WHERE asset = :asset and startDate <= :now and (endDate is null or endDate >= :now2)"; //$NON-NLS-1$
		List<AssetDeployment> ad = session.createQuery(sql, AssetDeployment.class)
				.setParameter("asset",  this) //$NON-NLS-1$
				.setParameter("now",  now) //$NON-NLS-1$
				.setParameter("now2", now) //$NON-NLS-1$
				.list();
		if (ad.size() == 0) return null;
		if (ad.size() == 1) return ad.get(0);
		//this likely shouldn't happen but if startdate = enddate = current time it might happen
		return ad.get(0);
		
	}
}
