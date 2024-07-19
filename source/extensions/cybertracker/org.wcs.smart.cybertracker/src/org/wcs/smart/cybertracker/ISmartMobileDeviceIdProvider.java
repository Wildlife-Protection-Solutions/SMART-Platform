package org.wcs.smart.cybertracker;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

public interface ISmartMobileDeviceIdProvider {

	public static final String EXT_ID = "org.wcs.smart.cybertracker.device";
	public static final String EXT_NAME = "device_id_provider";
	
	public List<String> getDeviceIds(Session session, ConservationArea ca);
	
}
