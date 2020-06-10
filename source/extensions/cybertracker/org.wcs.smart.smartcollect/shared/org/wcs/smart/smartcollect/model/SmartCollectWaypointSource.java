package org.wcs.smart.smartcollect.model;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

public class SmartCollectWaypointSource implements IWaypointSource {

	/**
	 * Location of incident data in the filestore
	 */
	public static final String FILESTORE_LOC = "smartcollect"; //$NON-NLS-1$
	
	/**
	 * Key for waypoint source
	 */
	public static final String KEY = "SMARTCOLLECT";
	
	public SmartCollectWaypointSource() {
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(ISmartCollectLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getDatastoreFileLocation(Object wp, Session session) throws Exception {
		if (wp instanceof SmartCollectWaypoint){
			StringBuilder sb = new StringBuilder();
			sb.append(FILESTORE_LOC);
			sb.append(File.separator);
			sb.append(UuidUtils.uuidToString(((SmartCollectWaypoint)wp).getWaypoint().getUuid()));
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
			throw new Exception("Object type " + wp.getClass().getName() + " not supported for smart collect waypoint source."); //$NON-NLS-1$ //$NON-NLS-2$
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
