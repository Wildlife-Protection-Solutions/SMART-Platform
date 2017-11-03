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
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Model class of asset. These represent the individual asset units (cameras, drones, etc.). 
 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset")
public class Asset extends UuidItem {
	
	public static final int ID_MAX_LENGTH = 128;
	
	public enum Status{
		ACTIVE,
		INACTIVE,
		RETIRED;
		
		public String getGuiName(Locale l) {
			switch(this) {
			case ACTIVE:
				return "Active";
			case INACTIVE:
				return "Inactive";
			case RETIRED:
				return "Retired";
			}
			return "";
		}
	}
	
	private String id;
	private AssetType assetType;
	private boolean isRetired;
	private Status status;

	private List<AssetAttributeValue> assetAttributes;
	private ConservationArea conservationArea;

	/** The set of asset_deployment. */
	//private Set<AssetDeployment> assetDeploymentSet;
	

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
	 * Get the status value;  will returned
	 * cached value if applicable. 
	 * 
	 * @return status
	 */
	@Transient
	public Status getStatus() {
		if (this.status == null) {
			computeStatus();
		}
		return this.status;
	}
	
	/**
	 * Get the status value; recomputing from database
	 * 
	 * @return status
	 */
	@Transient
	public Status getStatus(boolean refresh) {
		if (this.status == null || refresh) {
			computeStatus();
		}
		return this.status;
	}
	
	private void computeStatus() {
		if (getUuid() == null) {
			status = Status.INACTIVE;
			return;
		}else if (getIsRetired()) {
			status = Status.RETIRED;
			return;
		}
		try(Session s = HibernateManager.openSession()){
			Long activeDeployments = QueryFactory.buildCountQuery(s, AssetDeployment.class, 
					new Object[] {"asset", this}, //$NON-NLS-1$
					new Object[] {"endDate", null}); //$NON-NLS-1$
			if (activeDeployments == 0) {
				status = Status.INACTIVE;
			}else {
				status = Status.ACTIVE;
			}
		}
	}
}
