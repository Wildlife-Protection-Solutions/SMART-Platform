package org.wcs.smart.cybertracker.community;

import org.eclipse.core.runtime.IAdapterFactory;
import org.wcs.smart.cybertracker.community.model.CommunityWaypoint;
import org.wcs.smart.observation.model.Waypoint;

public class WaypointAdapterFactory implements IAdapterFactory {
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Waypoint.class) {
			if (adaptableObject instanceof CommunityWaypoint){
				return (T)((CommunityWaypoint)adaptableObject).getWaypoint();
				
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{Waypoint.class};
	}

}
