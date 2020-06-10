package org.wcs.smart.smartcollect;

import org.eclipse.core.runtime.IAdapterFactory;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypoint;

public class WaypointAdapterFactory implements IAdapterFactory {
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Waypoint.class) {
			if (adaptableObject instanceof SmartCollectWaypoint){
				return (T)((SmartCollectWaypoint)adaptableObject).getWaypoint();
				
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{Waypoint.class};
	}

}
