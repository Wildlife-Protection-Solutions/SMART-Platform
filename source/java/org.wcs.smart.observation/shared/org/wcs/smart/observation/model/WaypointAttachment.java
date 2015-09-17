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
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.UuidUtils;

/**
 * Link between a way point and associated
 * attachments
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_attachments")
public class WaypointAttachment extends ISmartAttachment {

	private Waypoint wp;

	public WaypointAttachment(){
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.wp;
	}
	public void setWaypoint(Waypoint wp){
		this.wp = wp;
		super.attachmentFile = null;
	}

	
//	@Transient
//	private String datastoreFolderPath = null;
//	/**
//	 * Sets directory location where the attachment should be stored.  This
//	 * does not have to be set.  If not set then getDatastoreFolderPath will
//	 * lookup the path from the waypoint and source.  
//	 * <p>This function is provided to deal with issues when saving waypoints.  In
//	 * some cases the waypoints are saved before the encompassing object (ex. patrol waypoint)
//	 * so the datastore folder path cannot be computed using standard process.  It
//	 * can be set manually here to allow objects to be saved.</p>
//	 * 
//	 * @param path this is the path not including the CAUUID 
//	 */
//	public void setDatastoreFolderExtension(String path, ConservationArea ca){
//		StringBuilder sb = new StringBuilder();
//		sb.append(ca);
//		sb.append(File.separator);
//		sb.append(path);
//		
//		this.datastoreFolderPath = sb.toString();
//	}
	
	/**
	 * @return the full path to where the file should be stored in the 
	 * database.
	 */
	@Transient
	@Override
	public String getDatastoreFolderPath(Session session) throws Exception{
//		if (datastoreFolderPath != null){
//			return datastoreFolderPath;
//		}
		if (getWaypoint() != null ){
			if (getWaypoint().getSourceId() == null){
				throw new Exception("No attachment information found for waypoint attachment " + UuidUtils.uuidToString(getUuid())); //$NON-NLS-1$
			}else{
				IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(getWaypoint().getSourceId());
				
				StringBuilder sb = new StringBuilder();
				sb.append(getWaypoint().getConservationArea().getFileDataStoreLocation());
				sb.append(File.separator);
				sb.append(src.getDatastoreFileLocation(getWaypoint(), session));
				return sb.toString();
			}
		}
		return null;
	}
}

