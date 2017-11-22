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

import org.wcs.smart.observation.model.Waypoint;

/**
 * Link between a waypoint and the asset deployment record.
 * 
 * @author Emily
 * 
 */
@Entity
@Table(name="smart.asset_waypoint")
public class AssetWaypoint {
	
	private AssetWaypointPk id = new AssetWaypointPk();

	
	@EmbeddedId
	public AssetWaypointPk getId(){
		return this.id;
	}
	public void setId(AssetWaypointPk id){
		this.id = id;
	}
	
	
	@Transient
	public AssetDeployment getAssetDeployment(){
		return id.deployment;
	}
	public void setAssetDeployment(AssetDeployment deployement){
		id.setAssetDeployment(deployement);
	}
	
	@Transient
	public Waypoint getWaypoint(){
		return id.wp;
	}
	public void setWaypoint(Waypoint wp){
		id.setWaypoint(wp);
	}
	
		
	@Override
	public boolean equals(Object other){
		if (other instanceof AssetWaypoint){
			return this.id.equals(((AssetWaypoint) other).id);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.id.hashCode();
	}
	
	@Embeddable
	public static class AssetWaypointPk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private AssetDeployment deployment;
		private Waypoint wp;
		
		public AssetWaypointPk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="asset_deployment_uuid", referencedColumnName="uuid")
		public AssetDeployment getAssetDeployment(){
			return this.deployment;
		}
		public void setAssetDeployment(AssetDeployment deployement){
			this.deployment = deployement;
		}
		
		@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
		public Waypoint getWaypoint(){
			return this.wp;
		}
		public void setWaypoint(Waypoint wp){
			this.wp  = wp;
		}
		
		@Override
		public boolean equals(Object other){
			if (other instanceof AssetWaypointPk){
				AssetWaypointPk p = (AssetWaypointPk)other;
				return Objects.equals(deployment, p.deployment) && Objects.equals(wp, p.wp);
			}
			return false;
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(deployment, wp);
		}
	}
}
