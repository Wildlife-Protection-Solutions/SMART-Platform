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
package org.wcs.smart.ca;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Tracks a conservation area.
 * 
 * @author Emily Gouge, Refractions Research Inc.
 *
 */
@Entity
@Table(name="smart.conservation_area")
public class ConservationArea extends UuidItem {

	public static final UUID MULTIPLE_CA = UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR);

	/**
	 * Maximum conservation area id length
	 */
	public static final int MAX_ID_LENGTH = 8;
	/**
	 * Maximum conservation area name length
	 */
	public static final int MAX_NAME_LENGTH = 256;
	/**
	 * Maximum description length
	 */
	public static final int MAX_DESCRIPTION_LENGTH = 2056;
	/**
	 * Maximum designation length
	 */
	public static final int MAX_DESIGNATION_LENGTH = 1024;
	/**
	 * Maximum organization length
	 */
	public static final int MAX_ORGANIZATION_LENGTH = 256;
	/**
	 * Maximum point of contact length
	 */
	public static final int MAX_POINT_OF_CONTACT_LENGTH = 256;
	/**
	 * Maximum country length
	 */
	public static final int MAX_COUNTRY_LENGTH = 256;
	/**
	 * Maximum owner length
	 */
	public static final int MAX_OWNER_LENGTH = 256;
	
	private String id;
	private String name;
	private String designation;
	private String description;
	private String organization;
	private String pointOfContact;
	private String country;
	private String owner;
	
	private List<Employee> employees;
	private List<Agency> agencies;
	private Set<Language> languages;
	
	
	public ConservationArea(){
		employees = new ArrayList<Employee>();
		languages = new HashSet<Language>();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}

	@Column(name = "pointofcontact")
	public String getPointOfContact() {
		return pointOfContact;
	}
	public void setPointOfContact(String pointOfContact) {
		this.pointOfContact = pointOfContact;
	}

	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public List<Employee> getEmployees() {
		return this.employees;
	}
	public void setEmployees(List<Employee> employees){
		this.employees = employees;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public List<Agency> getAgencies() {
		return this.agencies;
	}
	public void setAgencies(List<Agency> agencies){
		this.agencies = agencies;
	}
	
	@OneToMany(fetch = FetchType.EAGER, cascade={javax.persistence.CascadeType.ALL}, mappedBy="ca", orphanRemoval=true)
	public Set<Language> getLanguages(){
		return this.languages;
	}
	
	public void setLanguages(Set<Language> lang){
		this.languages = lang;
	}
	
	/**
	 * Determines if the current Conservation Area is a special
	 * cross conservation area query or not.
	 * This is currently done by checking the UUID.
	 * 
	 * @return
	 */
	@Transient
	public boolean getIsCcaa(){
		return getUuid().equals(MULTIPLE_CA);
	}
	
	@Transient
	public Language getDefaultLanguage(){
		for(Language lang: this.languages){
			if (lang.isDefault()){
				return lang;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return conservation area label
	 * to use on gui
	 */
	@Transient
	public String getNameLabel(){
		 return getId() + " - " + getName(); //$NON-NLS-1$
	}
	

	/**
	 * @return the filestore location for the given conservation area
	 */
	@Transient
	public String getFileDataStoreLocation(){
		return SmartContext.INSTANCE.getFilestoreLocation() + File.separator + UuidUtils.getDirectoryPath(getUuid());
	}
	
}

