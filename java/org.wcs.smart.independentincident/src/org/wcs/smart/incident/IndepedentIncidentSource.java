package org.wcs.smart.incident;

import java.io.File;

import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

public class IndepedentIncidentSource implements IWaypointSource {

	/**
	 * Location of patrol data in the filestore
	 */
	public static final String FILESTORE_LOC = "incidents"; //$NON-NLS-1$
	
	/**
	 * Source Key
	 */
	public static final String KEY = "INDINC"; //$NON-NLS-1$
	
	public IndepedentIncidentSource() {
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName() {
		return "Independent Incident";
	}

	@Override
	public String getDatastoreFileLocation(Waypoint wp) {
		StringBuilder sb = new StringBuilder();
		sb.append(FILESTORE_LOC);
		sb.append(File.separator);
		sb.append(SmartUtils.encodeHex(wp.getUuid()));
		sb.append(File.separator);
		return sb.toString();
	}

}
