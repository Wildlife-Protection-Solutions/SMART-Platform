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
package org.wcs.smart.asset.model;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * Link between a waypoint attachment and the asset deployment record. This
 * allows us to link individual attachments to the assets they came from.
 * 
 * @author Emily
 * 
 */
@Entity
@Table(name="smart.asset_waypoint_attachment")
public class AssetWaypointAttachment {
	
	private AssetWaypointAttachmentPk id = new AssetWaypointAttachmentPk();
	
	@EmbeddedId
	public AssetWaypointAttachmentPk getId(){
		return this.id;
	}
	public void setId(AssetWaypointAttachmentPk id){
		this.id = id;
	}
	
	
	@Transient
	public AssetWaypoint getAssetWaypoint(){
		return id.waypoint;
	}
	public void setAssetWaypoint(AssetWaypoint waypoint){
		id.setAssetWaypoint(waypoint);
	}
	
	@Transient
	public WaypointAttachment getWaypointAttachment(){
		return id.attachment;
	}
	public void setWaypointAttachment(WaypointAttachment attachment){
		id.setWaypointAttachment(attachment);
	}
	
		
	@Override
	public boolean equals(Object other){
		if (other instanceof AssetWaypointAttachment){
			return this.id.equals(((AssetWaypointAttachment) other).id);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.id.hashCode();
	}
	
	
	@Embeddable
	public static class AssetWaypointAttachmentPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private AssetWaypoint waypoint;
		private WaypointAttachment attachment;
		
		public AssetWaypointAttachmentPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="asset_waypoint_uuid", referencedColumnName="uuid")
		public AssetWaypoint getAssetWaypoint(){
			return this.waypoint;
		}
		public void setAssetWaypoint(AssetWaypoint waypoint){
			this.waypoint = waypoint;
		}
		
		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name="wp_attachment_uuid", referencedColumnName="uuid")
		public WaypointAttachment getWaypointAttachment(){
			return this.attachment;
		}
		public void setWaypointAttachment(WaypointAttachment attachment){
			this.attachment  = attachment;
		}
		
		@Override
		public boolean equals(Object other){
			if (other instanceof AssetWaypointAttachmentPk){
				AssetWaypointAttachmentPk p = (AssetWaypointAttachmentPk)other;
				return Objects.equals(waypoint, p.waypoint) && Objects.equals(attachment, p.attachment);
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(waypoint, attachment);
		}
	}


}
