/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.model;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART Collect waypoint source
 * @author Emily
 *
 */
public class SmartCollectWaypointSource implements IWaypointSource {

	/**
	 * Location of incident data in the filestore
	 */
	public static final String FILESTORE_LOC = "smartcollect"; //$NON-NLS-1$
	
	/**
	 * Key for waypoint source
	 */
	public static final String KEY = "SMARTCOLLECT"; //$NON-NLS-1$
	
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
