/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Simple Connect conservation area info object.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "connect.ca_info")
public class ConservationAreaProxy {

	private UUID caUuid;
	private String label;
	private ConservationAreaInfo.Status status;
	private UUID version;
	private Long revision;
	
	private String description;
	private String designation;
	
	private String organization;
	private String pointOfContact;
	private String location;
	private String owner;
	
	private String caBoundaryJson;
	private String bufferedManagementAreaJson;
	private String managementSectorsJson;
	private String patrolSectorsJson;
	private String administrativAreaJson;
	
	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getPointOfContact() {
		return pointOfContact;
	}

	public void setPointOfContact(String pointOfContact) {
		this.pointOfContact = pointOfContact;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public ConservationAreaProxy(ConservationAreaInfo ca){
		this.caUuid = ca.getUuid();
		this.label = ca.getLabel();
		this.status = ca.getStatus();
		this.version = ca.getVersion();
	}
	
	public void setDescriptionDesignation(String description, String designation){
		this.description = description;
		this.designation = designation;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public String getDesignation(){
		return this.designation;
	}
	
	public void setRevision(Long revision){
		this.revision = revision;
	}
	
	public Long getRevision(){
		return this.revision;
	}
	
	public UUID getUuid() {
		return caUuid;
	}

	public void setUuid(UUID uuid) {
		this.caUuid = uuid;
	}
	
	public UUID getVersion(){
		return this.version;
	}
	public void setVersion(UUID version){
		this.version = version;
	}
	
	public String getLabel(){
		return this.label;
	}
	public void setLabel(String label){
		this.label = label;
	}
	
	public ConservationAreaInfo.Status getStatus(){
		return this.status;
	}
	public void setStatus(ConservationAreaInfo.Status status){
		this.status = status;
	}


	
	//Area Boundary getters/setters
	public String getCaBoundaryJson() {
		return caBoundaryJson;
	}
	
	public void setCaBoundaryJson(String caBoundaryJson) {
		this.caBoundaryJson = caBoundaryJson;
	}

	public String getBufferedManagementAreaJson() {
		return bufferedManagementAreaJson;
	}

	public void setBufferedManagementAreaJson(String bufferedManagementAreaJson) {
		this.bufferedManagementAreaJson = bufferedManagementAreaJson;
	}

	public String getManagementSectorsJson() {
		return managementSectorsJson;
	}

	public void setManagementSectorsJson(String managementSectorsJson) {
		this.managementSectorsJson = managementSectorsJson;
	}

	public String getPatrolSectorsJson() {
		return patrolSectorsJson;
	}

	public void setPatrolSectorsJson(String patrolSectorsJson) {
		this.patrolSectorsJson = patrolSectorsJson;
	}

	public String getAdministrativAreaJson() {
		return administrativAreaJson;
	}

	public void setAdministrativAreaJson(String administrativAreaJson) {
		this.administrativAreaJson = administrativAreaJson;
	}
}
