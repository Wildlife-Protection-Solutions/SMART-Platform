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
package org.wcs.smart.connect.api.model;

import java.util.UUID;

/**
 * Simple Connect conservation area info object.
 * 
 * @author Emily
 *
 */
public class ConservationAreaProxy {

	public enum Status{
		UPLOADING,
		DATA,
		NODATA,
		CCAA
	}
	
	private UUID caUuid;
	private UUID version;
	private Long revision;
	private String label;
	private Status status;
	private String description;
	private String designation;
	
	public ConservationAreaProxy(){
	}

	public String getDescription(){
		return this.description;
	}
	
	public String getDesignation(){
		return this.designation;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	public void setDesignation(String designation){
		this.designation = designation;
	}
	
	public void setRevision(Long revision){
		this.revision = revision;
	}
	
	public Long getRevision(){
		return this.revision;
	}
	
	/**
	 * 
	 * @return the uuid for the list element
	 */
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
	
	public Status getStatus(){
		return this.status;
	}
	public void setStatus(Status status){
		this.status = status;
	}
}
