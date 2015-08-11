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
package org.wcs.smart.intelligence.model;

import java.io.File;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Link between intelligence and associated attachment
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Table(name="smart.intelligence_attachment")
public class IntelligenceAttachment implements ISmartAttachment {
	
	private UUID uuid;
	private Intelligence intelligence;
	private String filename;
	
	private File copyFromLocation;
    private String fullFile;
	
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="intelligence_uuid", referencedColumnName="uuid")
	public Intelligence getIntelligence(){
		return this.intelligence;
	}
	public void setIntelligence(Intelligence intelligence){
		this.intelligence = intelligence;
		setFullFile();
	}
	
	@Column(name="filename")
	public String getFilename(){
		return this.filename;
	}
	public void setFilename(String filename){
		this.filename = filename;
		setFullFile();
	}
	
	@Transient
	/**
	 * Location of the file to copy.  Temporarily set until
	 * saved.  Will return null if file already in datastore.
	 * @return 
	 */
	public File getCopyFromLocation(){
		return this.copyFromLocation;
	}
	/**
	 * Location to copy files from.  Temporarily set
	 * for newly added attachments until saved. 
	 */
	public void setCopyFromLocation(File newFile){
		this.copyFromLocation= newFile;
	}
	private void setFullFile(){
		if (intelligence != null) {
			this.fullFile = intelligence.getDatastoreLocation() + File.separator + getFilename();
		}
	}
	@Transient
	public File getFullFile(){
		if (this.fullFile == null){
			//try to set it
			setFullFile();
		}
		return new File(this.fullFile);
	}
	
	@Transient
	@Override
	public String getDatastoreFolderPath() throws Exception {
		return intelligence.getDatastoreLocation();
	}
	

}
