package org.wcs.smart.smartcollect;

import java.util.Locale;

import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;

public class SmartCollectLabelProvider implements ISmartCollectLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		
		if (item.getClass() == SmartCollectWaypointSource.class) return "SMART Collect Incident"; 

		return null;
	}

}
