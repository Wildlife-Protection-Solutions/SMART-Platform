package org.wcs.smart.asset.model;

import java.util.Collection;

import org.wcs.smart.asset.model.AssetWaypoint.State;
import org.wcs.smart.observation.model.Waypoint;

public class AssetWaypointMapping {

	private Waypoint waypoint;
	private Collection<AssetWaypoint> assetLinks;
	
	public AssetWaypointMapping(Waypoint waypoint, Collection<AssetWaypoint> assetLinks) {
		this.waypoint = waypoint;
		this.assetLinks = assetLinks;
	}
	
	public Waypoint getWaypoint() {
		return this.waypoint;
	}
	
	public Collection<AssetWaypoint> getAssetLinks(){
		return this.assetLinks;
	}
	
	public boolean hasDirty() {
		for (AssetWaypoint wp : assetLinks) {
			if (wp.getState() == State.DIRTY) return true;
		}
		return false;
	}
}
