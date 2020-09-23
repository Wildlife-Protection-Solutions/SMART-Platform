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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Model class for asset history record
 * 
 * @author egouge
 */
@Entity
@Table(name="smart.asset_history_record")
public class AssetHistoryRecord extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private Asset asset;
	private LocalDateTime date;
	private String comment;

	/**
	 * Constructor.
	 */
	public AssetHistoryRecord() {
	}


	/**
	 * Get the asset.
	 * 
	 * @return asset
	 */
	@ManyToOne(fetch = FetchType.LAZY)
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
	 * Get the date.
	 * 
	 * @return date
	 */
	@Column(name="date")
	public LocalDateTime getDate() {
		return this.date;
	}
	
	/**
	 * Set the date.
	 * 
	 * @param date
	 */
	public void setDate(LocalDateTime date) {
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
