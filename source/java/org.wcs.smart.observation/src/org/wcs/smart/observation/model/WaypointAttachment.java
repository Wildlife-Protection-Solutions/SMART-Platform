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
package org.wcs.smart.observation.model;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.util.SmartUtils;

/**
 * Link between a way point and associated
 * attachments
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_attachments")
public class WaypointAttachment extends UuidItem implements ISmartAttachment {

	private Waypoint wp;
	private String filename;
	
	private File copyFromLocation;
    private String fullFile = null;
	
	public WaypointAttachment(){
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.wp;
	}
	public void setWaypoint(Waypoint wp){
		this.wp = wp;
	}
	
	@Column(name="filename")
	public String getFilename(){
		return this.filename;
	}
	public void setFilename(String filename){
		this.filename = filename;
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
	
	@Transient
	public File getFullFile(){
		if (this.fullFile == null){
			//try to set it
			setFullFile();
		}
		return new File(this.fullFile);
	}
	
	private void setFullFile(){
		if (getWaypoint() != null){
			this.fullFile = getDatastoreFolderPath() + File.separator + getFilename();
		}
	}
	
	@Transient
	@Override
	public String getDatastoreFolderPath() {
		if (getWaypoint() != null ){
			if (getWaypoint().getSource() == null){
				ObservationPlugIn.log("No attachment information found for waypoint attachment " + SmartUtils.encodeHex(getUuid()), null);
			}else{
				return SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator + getWaypoint().getSource().getDatastoreFileLocation(getWaypoint());
			}
		}
		return null;
	}
}

