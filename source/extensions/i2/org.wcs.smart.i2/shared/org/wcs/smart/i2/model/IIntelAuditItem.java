package org.wcs.smart.i2.model;

import java.util.Date;

import org.wcs.smart.ca.Employee;

public interface IIntelAuditItem {
	
	/**
	 * Get the date_created.
	 * 
	 * @return date_created
	 */
	public Date getDateCreated();
	
	/**
	 * Set the date_created.
	 * 
	 * @param dateCreated
	 *            date_created
	 */
	public void setDateCreated(Date dateCreated);


	/**
	 * Set the date_modified.
	 * 
	 * @param dateModified
	 *            date_modified
	 */
	public void setDateModified(Date dateModified);

	/**
	 * Get the date_modified.
	 * 
	 * @return date_modified
	 */
	public Date getDateModified();

	/**
	 * Get the created_by.
	 * 
	 * @return created_by
	 */
	public Employee getCreatedBy();
	
	/**
	 * Set the created_by.
	 * 
	 * @param createdBy
	 *            created_by
	 */
	
	public void setCreatedBy(Employee createdBy);
	/**
	 * Get the last modified by.
	 * 
	 * @return last_modified_by
	 */
	
	public Employee getLastModifiedBy();
	
	/**
	 * Set the created_by.
	 * 
	 * @param createdBy
	 *            created_by
	 */
	public void setLastModifiedBy(Employee lastModifiedBy);
}
