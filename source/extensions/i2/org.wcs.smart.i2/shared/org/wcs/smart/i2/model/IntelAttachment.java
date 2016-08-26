package org.wcs.smart.i2.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;

/**
 * Model class of intelligence attachment.
 * 
 */
@Entity
@Table(name="smart.i_attachment")
public class IntelAttachment extends NamedItem{

	private ConservationArea ca;
	private Date dateCreated;
	private Employee createdBy;
	private String description;
	private String file;
	
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

	/**
	 * Set the file.
	 * 
	 * @param file
	 *            file
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * Get the file.
	 * 
	 * @return file
	 */
	public String getFile() {
		return this.file;
	}

}
