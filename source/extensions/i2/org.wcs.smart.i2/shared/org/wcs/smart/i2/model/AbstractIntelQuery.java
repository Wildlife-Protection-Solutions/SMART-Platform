package org.wcs.smart.i2.model;

import java.util.Date;

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

@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractIntelQuery extends NamedItem implements IIntelAuditItem {
	
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
	public abstract String getKeyId();
	
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
