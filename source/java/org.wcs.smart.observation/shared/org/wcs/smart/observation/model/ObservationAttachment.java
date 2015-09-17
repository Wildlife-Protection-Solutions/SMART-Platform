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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.UuidUtils;

/**
 * Observation attachment entities.  These are attachments associated
 * with a given observation (not the waypoint).
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.observation_attachment")
public class ObservationAttachment extends ISmartAttachment {

	private WaypointObservation observation;

    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="obs_uuid", referencedColumnName="uuid")
	public WaypointObservation getObservation(){
		return this.observation;
	}
	
	public void setObservation(WaypointObservation observation) throws Exception{
		this.observation = observation;
		super.attachmentFile = null;
	}

	
	@Transient
	private String datastoreFolderPath = null;
	/**
	 * Sets directory location where the attachment should be stored.  This
	 * does not have to be set.  If not set then getDatastoreFolderPath will
	 * lookup the path from the waypoint and source.  
	 * <p>This function is provided to deal with issues when saving waypoints.  In
	 * some cases the waypoints are saved before the encompassing object
	 * (ex. patrol waypoint)
	 * so the datastore folder path cannot be computed using standard process.  It
	 * can be set manually here to allow objects to be saved.</p>
	 * 
	 * @param path this is the path not including the CAUUID 
	 * @param conservation area 
	 */
	public void setDatastoreFolderExtension(String path, ConservationArea ca){
		StringBuilder sb = new StringBuilder();
		sb.append(ca.getFileDataStoreLocation());
		sb.append(File.separator);
		sb.append(path);
		
		this.datastoreFolderPath = sb.toString();
	}
	
	@Transient
	@Override
	public String getDatastoreFolderPath(Session session) throws Exception {
		if (datastoreFolderPath != null){
			return datastoreFolderPath;
		}
		if (observation != null && observation.getWaypoint() != null){
			if (observation.getWaypoint().getSourceId() == null){
				throw new Exception("No attachment information found for observation attachment " + UuidUtils.uuidToString(getUuid())); //$NON-NLS-1$
			}else{
				IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(observation.getWaypoint().getSourceId());

				return getObservation().getWaypoint().getConservationArea().getFileDataStoreLocation() + 
						File.separator + 
						src.getDatastoreFileLocation(observation.getWaypoint(), session) ;
			}
		}
		return null;
	}

}
