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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Model class for Asset Types.  Asset Types will be used to group assets
 *  based on the details you want to capture about the assets.
 * 
 * @author egouge
 */
@Entity
@Table(name="asset_type", schema="smart")
public class AssetType extends NamedKeyItem{

	private static final long serialVersionUID = 1L;
	
	private ConservationArea conservationArea;
	private byte[] icon;
	private Integer incidentCutoff;

	private List<Asset> assets;
	private List<AssetTypeAttribute> assetAttributes;
	private List<AssetTypeDeploymentAttribute> deploymentAttributes;

	/**
	 * Constructor.
	 */
	public AssetType() {
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
	 * Get the icon.
	 * 
	 * @return icon
	 */
	@Column(name="icon")
	@Lob
	public byte[] getIcon() {
		return this.icon;
	}
	
	/**
	 * Set the icon.
	 * 
	 * @param icon
	 *            icon
	 */
	public void setIcon(byte[] icon) {
		this.icon = icon;
	}
	
	@Transient
	public BufferedImage getIconAsImage() throws Exception{
		if (getIcon() == null) return null;
		try(ByteArrayInputStream in = new ByteArrayInputStream(getIcon())){
			return ImageIO.read(in);
		}
	}
	
	/**
	 * Get the incident_cutoff.
	 * This will be used to group files/images into a single incident.  All data that occurs
	 *  within the time frame will be grouped into a single incident 
	 *  instead of creating multiple incidents.
	 * 
	 * Value is stored in seconds.
	 * 
	 * @return incident_cutoff
	 */
	@Column(name="incident_cutoff")
	public Integer getIncidentCutoff() {
		return this.incidentCutoff;
	}

	/**
	 * Set the incident_cutoff.
	 * 
	 * @param incidentCutoff
	 *            incident_cutoff
	 */
	public void setIncidentCutoff(Integer incidentCutoff) {
		this.incidentCutoff = incidentCutoff;
	}


	/**
	 * Get the set of the asset.
	 * 
	 * @return The set of asset
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="assetType",orphanRemoval=true, cascade= {CascadeType.ALL})
	public List<Asset> getAssets() {
		return this.assets;
	}

	/**
	 * Set the set of the asset.
	 * 
	 * @param assetSet
	 *            The set of asset
	 */
	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}


	/**
	 * Get the set of the asset_type_attribute. These
	 * are the attributes that should be collected for
	 * each asset of this type
	 * 
	 * @return The set of asset_type_attribute
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="id.assetType",orphanRemoval=true, cascade= {CascadeType.ALL})
	@OrderBy("seq_order asc")
	public List<AssetTypeAttribute> getAssetAttributes() {
		return this.assetAttributes;
	}
	
	/**
	 * Set the set of the asset_type_attribute.
	 * 
	 * @param assetTypeAttributeSet
	 *            The set of asset_type_attribute
	 */
	public void setAssetAttributes(List<AssetTypeAttribute> assetAttributes) {
		this.assetAttributes = assetAttributes;
	}



	/**
	 * Get the set of the asset_type_deployment_attribute. These
	 * are the attributes that should be collected for each asset deployment
	 * of an asset of this type.
	 * 
	 * @return The set of asset_type_deployment_attribute
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="id.assetType",orphanRemoval=true, cascade= {CascadeType.ALL})
	@OrderBy("seq_order asc")
	public List<AssetTypeDeploymentAttribute> getAssetDeploymentAttributes() {
		return this.deploymentAttributes;
	}

	/**
	 * Set the set of the asset_type_deployment_attribute.
	 * 
	 * @param assetTypeDeploymentAttributeSet
	 *            The set of asset_type_deployment_attribute
	 */
	public void setAssetDeploymentAttributes(List<AssetTypeDeploymentAttribute> deploymentAttributes) {
		this.deploymentAttributes = deploymentAttributes;
	}


}
