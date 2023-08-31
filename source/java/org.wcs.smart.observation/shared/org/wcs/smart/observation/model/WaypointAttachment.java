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

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Link between a way point and associated
 * attachments
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="wp_attachments", schema="smart")
public class WaypointAttachment extends ISmartAttachment implements ISignatureAttachment {

	private static final long serialVersionUID = 1L;
	
	private Waypoint wp;
	private SignatureType signatureType; 
	
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
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="signature_type_uuid", referencedColumnName="uuid")
	public SignatureType getSignatureType(){
		return this.signatureType;
	}
	public void setSignatureType(SignatureType stype){
		this.signatureType = stype;
	}

	
	/**
	 * @return the full path to where the file should be stored in the 
	 * database.
	 */
	@Transient
	@Override
	public String getDatastoreFolderPath(Session session) throws Exception{
		if (getWaypoint() != null ){
			if (getWaypoint().getSourceId() == null){
				throw new Exception("No attachment information found for waypoint attachment " + UuidUtils.uuidToString(getUuid())); //$NON-NLS-1$
			}else{
				IWaypointSource src = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(getWaypoint().getSourceId());
				
				StringBuilder sb = new StringBuilder();
				sb.append(session.get(ConservationArea.class, getWaypoint().getConservationArea().getUuid()).getFileDataStoreLocation());
				sb.append(File.separator);
				sb.append(src.getDatastoreFileLocation(getWaypoint(), session));
				return sb.toString();
			}
		}
		return null;
	}
	
	@Transient
	public ConservationArea getConservationArea() {
		return this.wp.getConservationArea();
	}
}

