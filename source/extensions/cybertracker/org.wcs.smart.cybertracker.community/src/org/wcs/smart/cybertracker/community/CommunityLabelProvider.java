package org.wcs.smart.cybertracker.community;

import java.util.Locale;

import org.wcs.smart.cybertracker.community.model.CommunityWaypointSource;
import org.wcs.smart.cybertracker.community.model.ICommunityLabelProvider;

public class CommunityLabelProvider implements ICommunityLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item.getClass() == CommunityWaypointSource.class) return "Community Incident"; 

		return null;
	}

}
