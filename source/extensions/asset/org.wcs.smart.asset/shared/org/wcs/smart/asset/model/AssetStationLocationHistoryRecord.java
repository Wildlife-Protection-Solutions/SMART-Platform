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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Model class of Asset Station Location History record.
 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset_station_location_history")
public class AssetStationLocationHistoryRecord extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private AssetStationLocation location;
	private Date date;
	private String comment;

	/**
	 * Constructor.
	 */
	public AssetStationLocationHistoryRecord() {
	}


	/**
	 * Get the asset.
	 * 
	 * @return asset
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="station_location_uuid", referencedColumnName="uuid")
	public AssetStationLocation getStationLocation() {
		return this.location;
	}

	/**
	 * Set the asset.
	 * 
	 * @param asset
	 */

	public void setStationLocation(AssetStationLocation location) {
		this.location = location;
	}


	/**
	 * Get the date.
	 * 
	 * @return date
	 */
	@Column(name="date")
	public Date getDate() {
		return this.date;
	}
	
	/**
	 * Set the date.
	 * 
	 * @param date
	 */
	public void setDate(Date date) {
		this.date = date;
	}


	/**
	 * Get the comment.
	 * 
	 * @return comment
	 */
	@Column(name="comment")
	public String getComment() {
		return this.comment;
	}
	
	/**
	 * Set the comment.
	 * 
	 * @param comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
}
