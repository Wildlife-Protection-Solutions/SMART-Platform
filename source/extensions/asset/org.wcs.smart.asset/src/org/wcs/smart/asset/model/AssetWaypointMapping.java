/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import java.util.Collection;

import org.wcs.smart.asset.model.AssetWaypoint.State;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Utility class for mapping a waypoint to all associated asset waypoints.
 * A single waypoint my be comprised of attachments that are associated with
 * different assets
 * 
 * @author Emily
 *
 */
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
