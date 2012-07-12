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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.util.SmartUtils;

/**
 * 
 * Tracks a conservation area.
 * 
 * @author Emily Gouge, Refractions Research Inc.
 *
 */
@Entity
@Table(name="smart.conservation_area")
public class ConservationArea {

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
	
	private byte[] uuid;
	private String id;
	private String name;
	private String designation;
	private String description;
	
	private String srs;
	private List<Employee> employees;
	private Set<Language> languages;
	
	public ConservationArea(){
		employees = new ArrayList<Employee>();
		languages = new HashSet<Language>();
	}
	
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public byte[] getUuid(){
		return this.uuid;
	}
	public void setUuid(byte[] uuid){
		this.uuid = uuid;
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
	
	@Column(name="default_srs")
	public String getSrs() {
		return srs;
	}
	public void setSrs(String srs) {
		this.srs = srs;
	}
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public List<Employee> getEmployees() {
		return this.employees;
	}
	public void setEmployees(List<Employee> employees){
		this.employees = employees;
	}

	@OneToMany(fetch = FetchType.EAGER, cascade={javax.persistence.CascadeType.ALL}, mappedBy="ca", orphanRemoval=true)
	public Set<Language> getLanguages(){
		return this.languages;
	}
	
	public void setLanguages(Set<Language> lang){
		this.languages = lang;
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
	
	

	@Transient
	public String getFileDataStoreLocation(){
		String filestore = SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY);
		filestore = filestore + File.separator + SmartUtils.getDirectoryPath(uuid);
		return filestore;
	}
	
	@Transient
	public String getRootFileDataStoreLocation(){
		String filestore = SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY);
		return filestore;
	}

	/**
	 * Two conservation areas are the same if they
	 * share the same uuid.
	 */
	@Override
	public boolean equals(Object other){
		if (other == null || !(other instanceof ConservationArea)){
			return false;
		}
		return Arrays.equals(uuid, ((ConservationArea)other).uuid);
	}
	
	/**
	 * Generates a hashcode based on the uuid
	 */
	@Override
	public int hashCode(){
		return Arrays.hashCode(uuid);
	}
}

