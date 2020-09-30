/*
 * Copyright (C) 2012 Wildlife Conservation Society
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

import java.time.LocalDateTime;

import org.wcs.smart.ca.Employee;

/**
 * Interface for intelligence model elements that are audited with
 * date created, date modified, created by and last modified by
 * fields.
 * 
 * @author Emily
 *
 */
public interface IIntelAuditItem {
	
	/**
	 * Get the date_created.
	 * 
	 * @return date_created
	 */
	public LocalDateTime getDateCreated();
	
	/**
	 * Set the date_created.
	 * 
	 * @param dateCreated
	 *            date_created
	 */
	public void setDateCreated(LocalDateTime dateCreated);


	/**
	 * Set the date_modified.
	 * 
	 * @param dateModified
	 *            date_modified
	 */
	public void setDateModified(LocalDateTime dateModified);

	/**
	 * Get the date_modified.
	 * 
	 * @return date_modified
	 */
	public LocalDateTime getDateModified();

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
