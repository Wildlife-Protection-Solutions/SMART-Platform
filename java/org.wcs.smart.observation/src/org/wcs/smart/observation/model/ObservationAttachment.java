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
 * Observation attachment entities.  These are attachments associated
 * with a given observation (not the waypoint).
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.observation_attachment")
public class ObservationAttachment extends UuidItem implements ISmartAttachment {


	private WaypointObservation observation;
	private String filename;
	
	private File copyFromLocation;
    private String fullFile;
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="obs_uuid", referencedColumnName="uuid")
	public WaypointObservation getObservation(){
		return this.observation;
	}
	
	public void setObservation(WaypointObservation observation){
		this.observation = observation;
		setFullFile();
	}
	
	@Override
	@Column(name="filename")
	public String getFilename() {
		return this.filename;
	}

	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}

	
	@Transient
	@Override
	public File getCopyFromLocation() {
		return copyFromLocation;
	}

	@Override
	public void setCopyFromLocation(File newFile) {
		this.copyFromLocation = newFile;
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
		this.fullFile = getDatastoreFolderPath() + File.separator + getFilename();
	}

	@Transient
	@Override
	public String getDatastoreFolderPath() {
		if (observation != null && observation.getWaypoint() != null){
			if (observation.getWaypoint().getSource() == null){
				ObservationPlugIn.log("No attachment information found for observation attachment " + SmartUtils.encodeHex(getUuid()), null);
			}else{
				return SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + 
						File.separator + 
						observation.getWaypoint().getSource().getDatastoreFileLocation(observation.getWaypoint()) ;
			}
		}
		return null;
	}

}
