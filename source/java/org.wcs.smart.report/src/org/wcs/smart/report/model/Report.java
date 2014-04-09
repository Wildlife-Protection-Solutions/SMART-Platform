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
package org.wcs.smart.report.model;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.report.ReportPlugIn;

/**
 * A report object.
 * 
 * @author egouge
 * @since 1.0.0
 */
@Entity
@Table(name="smart.report")
public class Report extends NamedItem {

	public static final int MAX_NAME_LENGTH = org.wcs.smart.ca.Label.MAX_LENGTH;
	
	private Employee creator;
	private String id;
	private ConservationArea conservationArea;
	private boolean isShared;
	private ReportFolder folder;
	private String filename;
	
	/**
	 * @return the creator
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public Employee getOwner() {
		return creator;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(Employee owner) {
		this.creator = owner;
	}
	
	/**
	 * @return the id
	 */
	@Column(name="id")
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the conservationArea
	 */

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * @param conservationArea the conservationArea to set
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	/**
	 * @return the isShared
	 */
	@Column(name="shared")
	public boolean getShared() {
		return isShared;
	}
	/**
	 * @param isShared the isShared to set
	 */
	public void setShared(boolean isShared) {
		this.isShared = isShared;
	}
	
	
	/**
	 * @return the report folder associated with the report or <code>null</code>
	 * if associated with the root folder
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="folder_uuid", referencedColumnName="uuid")
	public ReportFolder getFolder() {
		return folder;
	}
	/**
	 * @param folder the folder to set
	 */
	public void setFolder(ReportFolder folder) {
		this.folder = folder;
	}
	
	/**
	 * @return the report filename
	 */
	@Column(name="filename")
	public String getFilename(){
		return this.filename;
	}
	/**
	 * Sets the filename corresponding to the BIRT
	 * report file.
	 * @param filename
	 */
	public void setFilename(String filename){
		this.filename = filename;
	}
	
	/**
	 * @return the full report filename including path
	 */
	@Transient
	public File getFullReportFilename(){
		return new File(ReportPlugIn.getReportDirectory(getConservationArea()), getFilename());
	}
	
}
