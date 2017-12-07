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

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.eclipse.core.runtime.IAdaptable;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Link between a waypoint and the asset waypoint
 * 
 * @author Emily
 * 
 */
@Entity
@Table(name="smart.asset_waypoint")
public class AssetWaypoint extends UuidItem implements IAdaptable{
	
	public enum State{
		DIRTY,
		OK
	}
	
	private AssetDeployment deployment;
	private Waypoint wp;
	private State state;
	
	private Set<AssetWaypointAttachment> attachments;
	
	@Column(name="state")
	@Enumerated(value=EnumType.ORDINAL)
	public State getState() {
		return this.state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="asset_deployment_uuid", referencedColumnName="uuid")
	public AssetDeployment getAssetDeployment(){
		return this.deployment;
	}
	public void setAssetDeployment(AssetDeployment deployement){
		this.deployment = deployement;
	}
	
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.wp;
	}
	public void setWaypoint(Waypoint wp){
		this.wp  = wp;
	}
	
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.assetWaypoint", orphanRemoval = true, cascade={CascadeType.ALL})
	public Set<AssetWaypointAttachment> getAttachments(){
		return this.attachments;
	}
	
	public void setAttachments(Set<AssetWaypointAttachment> attachments) {
		this.attachments = attachments;
	}
		
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(Waypoint.class)) return (T) getWaypoint();
		return null;
	}
}
