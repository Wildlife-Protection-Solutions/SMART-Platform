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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;

/**
 * A report folder object
 * @author egouge
 * @since 1.0.0
 */
@Entity
@Table(name="smart.report_folder")
public class ReportFolder extends NamedItem {

	private ReportFolder parentFolder;
	private Employee employee;
	private ConservationArea conservationArea;
	
	private List<ReportFolder> childrenFolders;
	
	private ReportFolder deletedParent;
	
	
	/**
	 * @return the parent folder or <code>null</code> if root folder
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="parent_uuid", referencedColumnName="uuid")
	public ReportFolder getParentFolder() {
		return parentFolder;
	}
	/**
	 * @param parentFolder the parent folder or <code>null</code> if root folder
	 */
	public void setParentFolder(ReportFolder parentFolder) {
		this.parentFolder = parentFolder;
	}
	
	/**
	 * @return the folder owner; or <code>null</code> if a conservation area folder
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="employee_uuid", referencedColumnName="uuid")
	public Employee getEmployee() {
		return employee;
	}
	/**
	 * The folder owner; or <code>null</code> if a conservation area folder
	 * @param employee
	 */
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
	
	/**
	 * The conservation area this folder is associated with
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * The conservation area this folder is associated with
	 * @param conservationArea
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	/**
	 * @return the children folders
	 */
	@LazyCollection(LazyCollectionOption.FALSE)  //to fix hiberante bug with using fetchType=EAGER in @onetomany annoation
	@OneToMany(mappedBy="parentFolder", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<ReportFolder> getChildren(){
		return this.childrenFolders;
	}
	/**
	 * Sets the children folder
	 * @param children
	 */
	public void setChildren(List<ReportFolder> children){
		this.childrenFolders = children;
	}
	
	/**
	 * @return the deletedParent
	 */
	@Transient
	public ReportFolder getDeletedParent() {
		return deletedParent;
	}
	/**
	 * When the report is deleted in order for it to 
	 * be deleted correct the parent value must be set to null.
	 * Before setting the parent value to null that value
	 * is copied to this variable so other actions
	 * can determine what the old parent was.
	 * 
	 * @param deletedParent the deletedParent to set
	 */
	@Transient
	public void setDeletedParent(ReportFolder deletedParent) {
		this.deletedParent = deletedParent;
	}
}
