package org.wcs.smart.cybertracker.community.model;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

public class CommunityWaypointSource implements IWaypointSource {

	/**
	 * Location of incident data in the filestore
	 */
	public static final String FILESTORE_LOC = "community"; //$NON-NLS-1$
	
	/**
	 * Key for waypoint source
	 */
	public static final String KEY = "COMMUNITY";
	
	public CommunityWaypointSource() {
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(ICommunityLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getDatastoreFileLocation(Object wp, Session session) throws Exception {
		if (wp instanceof CommunityWaypoint){
			StringBuilder sb = new StringBuilder();
			sb.append(FILESTORE_LOC);
			sb.append(File.separator);
			sb.append(UuidUtils.uuidToString(((CommunityWaypoint)wp).getWaypoint().getUuid()));
			sb.append(File.separator);
			return sb.toString();
		}else if (wp instanceof Waypoint) {
			StringBuilder sb = new StringBuilder();
			sb.append(FILESTORE_LOC);
			sb.append(File.separator);
			sb.append(UuidUtils.uuidToString(((Waypoint)wp).getUuid()));
			sb.append(File.separator);
			return sb.toString();
		}else{
			throw new Exception("Object type " + wp.getClass().getName() + " not supported for community waypoint source."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public String getSourceLabel(Object source, Session session, Locale l) {
		Waypoint ap = (Waypoint) source;
		
		StringBuilder sb = new StringBuilder();
		sb.append(getName(l));
		sb.append(": "); //$NON-NLS-1$
		sb.append(ap.getId());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, l).format(ap.getDateTime()));
		sb.append(") "); //$NON-NLS-1$
		return sb.toString();
	}

}
