package org.wcs.smart.asset.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Model class of asset map style.  Provides for ability to save
 * asset over map styles. 
 * 
 * @author egouge
 */
@Table
@Entity(name="smart.asset_map_style")
public class AssetMapStyle extends UuidItem{

	public static final int MAX_NAME_LENGTH = 1024;
	
	private String name;
	private ConservationArea conservationArea;
	private String styleString;
	
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
	 * Get the name
	 * 
	 * @return name
	 */
	@Column(name="name")
	public String getName() {
		return this.name;
	}
	
	/**
	 * Set the name.
	 * 
	 * @param name
	 *            name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the map style as xml string
	 * 
	 * @return style
	 */
	@Column(name="style_string")
	public String getStyleString() {
		return this.styleString;
	}
	
	/**
	 * Set the style string 
	 * 
	 */
	public void setStyleString(String style) {
		this.styleString = style;
	}
	
}
