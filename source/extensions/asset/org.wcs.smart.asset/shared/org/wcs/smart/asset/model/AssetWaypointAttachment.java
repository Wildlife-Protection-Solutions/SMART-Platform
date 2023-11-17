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

import org.wcs.smart.observation.model.WaypointAttachment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Link between a waypoint attachment and the asset deployment record. This
 * allows us to link individual attachments to the assets they came from.
 * 
 * @author Emily
 * 
 */
@Entity
@Table(name="asset_waypoint_attachment", schema="smart")
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
		if (this == other) return true;
		if (other == null) return false;
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		AssetWaypointAttachment s = (AssetWaypointAttachment)other;
		return (Objects.equals(getId(), s.getId()));
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
			
			if (this == other) return true;
			if (other == null) return false;
			if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
			AssetWaypointAttachmentPk s = (AssetWaypointAttachmentPk)other;
			return Objects.equals(getAssetWaypoint(), s.getAssetWaypoint()) && 
					Objects.equals(getWaypointAttachment(), s.getWaypointAttachment());
			
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(waypoint, attachment);
		}
	}


}
