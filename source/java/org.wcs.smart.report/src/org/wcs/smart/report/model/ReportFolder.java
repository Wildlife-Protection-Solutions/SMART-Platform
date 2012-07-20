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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.HasLabel;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;

/**
 * A report folder object
 * @author egouge
 * @since 1.0.0
 */
@Entity
@Table(name="smart.report_folder")
public class ReportFolder extends HasLabel implements IAdaptable {

	private ReportFolder parentFolder;
	private Employee employee;
	private ConservationArea conservationArea;
	
	private List<ReportFolder> childrenFolders;
	private Set<Label> names;	//names
	private String name;
	
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
	 * 
	 * @return the names associated with the list element in the
	 * language the platform is running in.
	 */
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="uuid", insertable=false, updatable=false)	
	@Basic(fetch = FetchType.LAZY)
	public String getName() {
		return name;
	}
	
	/**
	 * Do not use to set the name.
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Updates the name of the associated language
	 * @param name
	 */
	@Transient
	public void updateName(String name, Language language){
		boolean found = false;
		for (Label l : names){
			if (l.getLanguage().equals(language)){
				l.setValue(name);
				found = true;
			}
		}
		if (!found){
			Label lbl = new Label();
			lbl.setElement(this);
			lbl.setLanguage(language);
			lbl.setValue(name);
			names.add(lbl);
		}
	}
	
	/**
	 * 
	 * @return the names associated with the list element
	 */
	@OneToMany(targetEntity = Label.class, fetch = FetchType.LAZY, mappedBy="id.element", cascade={CascadeType.ALL}, orphanRemoval=true)
	public Set<Label> getNames() {
		if (names == null){
			names = new HashSet<Label>();
		}
		return names;
	}

	/**
	 * Sets all names
	 * @param names
	 */
	public void setNames(Set<Label> names) {
		this.names = names;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	@Transient
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
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
