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
package org.wcs.smart.i2.model;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Model class of intelligence attachment.
 * 
 */
@Entity
@Table(name="smart.i_attachment")
public class IntelAttachment extends ISmartAttachment{

	public static final String INTELLIGENCE_FS_DIR = "intelligence2";
	
	private ConservationArea ca;
	private Date dateCreated;
	private Employee createdBy;
	private String description;
	
	/**
	 * Constructor.
	 */
	public IntelAttachment() {
	}

	/**
	 * Get the conservation_area.
	 * 
	 * @return conservation_area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	/**
	 * Set the conservation_area.
	 * 
	 * @param ca
	 *            conservation_area
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}

	
	/**
	 * Get the date_created.
	 * 
	 * @return date_created
	 */
	@Column(name="date_created")
	public Date getDateCreated() {
		return this.dateCreated;
	}

	/**
	 * Set the date_created.
	 * 
	 * @param dateCreated
	 *            date_created
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * Get the created_by.
	 * 
	 * @return created_by
	 */
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="created_by", referencedColumnName="uuid")
	public Employee getCreatedBy() {
		return this.createdBy;
	}
	
	/**
	 * Set the created_by.
	 * 
	 * @param createdBy
	 *            created_by
	 */
	public void setCreatedBy(Employee createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * Set the description.
	 * 
	 * @param description
	 *            description
	 */
	@Column(name="description")
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Get the description.
	 * 
	 * @return description
	 */
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getDatastoreFolderPath(Session session) throws Exception {
		return getConservationArea().getFileDataStoreLocation() + 
				File.separator +
				INTELLIGENCE_FS_DIR +
				"/attachments";
	}


}
