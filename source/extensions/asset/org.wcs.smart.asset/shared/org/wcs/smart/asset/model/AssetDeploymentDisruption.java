/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;

/**
 * Represents a disruption in an asset deployment.  
 * 
 * For example, if mud splashes up on the lens and the camera is incapable of 
 * taking images properly in the middle of deployment 
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@Entity
@Table(name="smart.asset_deployment_disruption")
public class AssetDeploymentDisruption extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	private Date startDate;
	private Date endDate;
	private String comment;
	
	private AssetDeployment deployment;
	
	
	/**
	 * Get the asset station
	 * 
	 * @return location
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="asset_deployment_uuid", referencedColumnName="uuid")
	public AssetDeployment getAssetDeployment() {
		return this.deployment;
	}

	/**
	 * Set the asset station.
	 * 
	 * @param location
	 */
	public void setAssetDeployment(AssetDeployment deployment) {
		this.deployment = deployment;
	}

	/**
	 * Gets the comment associated with the disruption
	 * @return
	 */
	@Column(name="comment")
	public String getComment() {
		return this.comment;
	}
	
	/**
	 * Set the comment associated with the disruption
	 * @param comment
	 */
	public void setComment(String comment) {
		this.comment = comment;
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
	 * Computes the time in field of this asset deployment. This is computed
	 * as the number of seconds from the start date to either the current time
	 * or the end time (whichever is older).  So future dates are not
	 * included in the active time.
	 * @return
	 */
	@Transient
	public double getActiveTimeInSeconds() {
		Date now = new Date();
		if (getStartDate().after(now)) return 0;
		long start = getStartDate().getTime();
		long end = now.getTime();
		if (getEndDate() != null && getEndDate().before(now)) {
			end = getEndDate().getTime();
		}
		return (end-start) / 1000.0;
	}
}
