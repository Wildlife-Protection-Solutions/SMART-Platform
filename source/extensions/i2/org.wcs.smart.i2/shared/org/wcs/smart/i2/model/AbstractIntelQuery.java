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

import java.util.Date;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;

/**
 * Abstract class for advanced intelligence queries
 * 
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractIntelQuery extends NamedItem implements IIntelAuditItem {
	
	private static final long serialVersionUID = 1L;
	
	protected ConservationArea ca;
	protected String queryString;
	
	protected Date dateCreated;
	protected Date dateModified;

	protected Employee createdBy;
	protected Employee lastModifiedBy;

	/**
	 * 
	 * @return the key is that represents the query type
	 */
	@Transient
	public abstract String getTypeKey();
	
	@Transient
	public String getIconName() {
		return getTypeKey().toLowerCase(Locale.ROOT) + ".png"; //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @return style string for map based queries; null for all other queries
	 */
	@Transient
	public String getStyle() { return null; }
	
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
	 * Get the date_modified.
	 * 
	 * @return date_modified
	 */
	@Column(name="last_modified_date")
	public Date getDateModified() {
		return this.dateModified;
	}
	
	/**
	 * Set the date_modified.
	 * 
	 * @param dateModified
	 *            date_modified
	 */
	public void setDateModified(Date dateModified) {
		this.dateModified = dateModified;
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
	 * Get the last modified by.
	 * 
	 * @return last_modified_by
	 */
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="last_modified_by", referencedColumnName="uuid")
	public Employee getLastModifiedBy() {
		return this.lastModifiedBy;
	}
	
	/**
	 * Set the created_by.
	 * 
	 * @param createdBy
	 *            created_by
	 */
	public void setLastModifiedBy(Employee lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	@Column(name="query_string")
	public String getQueryString(){
		return this.queryString;
	}
	public void setQueryString(String queryString){
		this.queryString = queryString;
	}
	
}
