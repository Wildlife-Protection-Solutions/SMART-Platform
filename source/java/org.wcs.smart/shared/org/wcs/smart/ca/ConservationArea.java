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
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.wcs.smart.SmartContext;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 
 * Tracks a conservation area.
 * 
 * @author Emily Gouge, Refractions Research Inc.
 *
 */
@Entity
@Table(name="conservation_area", schema="smart")
public class ConservationArea extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	public static final UUID MULTIPLE_CA = UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR);

	/**
	 * Folder for storing Conservation Area Icon
	 */
	public static final String ICON_FOLDER_NAME = "ca_icon"; //$NON-NLS-1$
	
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
	
	@OneToMany(fetch = FetchType.EAGER, cascade={CascadeType.ALL}, mappedBy="ca", orphanRemoval=true)
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
	
	/**
	 * Sets the logo associated with the Conservation Area; this will overwrite the existing
	 * logo so should only be called from within the database transaction
	 * that updates the Conservation Area
	 * 
	 * Ensure the Conservation Area has a valid UUID before calling this function.  
	 * To clear the icon call with null value for newFile.
	 * 
	 * @param newFile
	 * @throws IOException
	 */
	@Transient
	public void setLogo(Path newFile) throws IOException {
		if (getUuid() == null) throw new IllegalStateException("Cannot set the Conservation Area icon until the unique identifier has been provided."); //$NON-NLS-1$
		Path iconFolder = Paths.get(getFileDataStoreLocation(), ICON_FOLDER_NAME);
		
		Path currentFile = getLogo();
		if (currentFile == null && newFile == null) return;
		if (currentFile != null && currentFile.equals(newFile)) return;
		
		//delete all files in the icon folder (there should old be one)
		if (Files.exists(iconFolder)) {
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(iconFolder)){
				for (Path p : stream) {
					if (!Files.isDirectory(p))Files.delete(p);
				}
			}
		}else {
			Files.createDirectories(iconFolder);
		}
		
		if (newFile != null) {
			Files.copy(newFile, Paths.get(getFileDataStoreLocation(), ICON_FOLDER_NAME, newFile.getFileName().toString()));
		}
	}
	
	/**
	 * Gets the icon associated with the conservation area or null if none specified 
	 * @return
	 * @throws IOException
	 */
	@Transient
	public Path getLogo() throws IOException {
		if (getUuid() == null) return null;
		Path iconFolder = Paths.get(getFileDataStoreLocation(), ICON_FOLDER_NAME);
		if (!Files.exists(iconFolder)) return null;
		
		//there should only ever be one icon here; lets return it if found
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(iconFolder)){
			for (Path p : stream) {
				return p;
			}
		}
		return null;
	}
	
}

