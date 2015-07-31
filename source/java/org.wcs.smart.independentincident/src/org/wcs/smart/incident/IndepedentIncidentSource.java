/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.incident;

import java.io.File;

import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Waypoint source class for indepdenent incidents.
 *  
 * @author Emily
 *
 */
public class IndepedentIncidentSource implements IWaypointSource {

	/**
	 * Location of incident data in the filestore
	 */
	public static final String FILESTORE_LOC = "incidents"; //$NON-NLS-1$
	
	/**
	 * Source Key
	 */
	public static final String KEY = "INDINC"; //$NON-NLS-1$
	
	public IndepedentIncidentSource() {
	}

	/**
	 * @see org.wcs.smart.observation.model.IWaypointSource#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * @see org.wcs.smart.observation.model.IWaypointSource#getName()
	 */
	@Override
	public String getName() {
		return Messages.IndepedentIncidentSource_IndIncidentWaypointsourceName;
	}

	/**
	 * @see org.wcs.smart.observation.model.IWaypointSource#getDatastoreFileLocation(org.wcs.smart.observation.model.Waypoint)
	 */
	@Override
	public String getDatastoreFileLocation(Object wp) {
		if (wp instanceof Waypoint){
			StringBuilder sb = new StringBuilder();
			sb.append(FILESTORE_LOC);
			sb.append(File.separator);
			sb.append(UuidUtils.uuidToString(((Waypoint)wp).getUuid()));
			sb.append(File.separator);
			return sb.toString();
		}else{
			throw new IllegalStateException("Object type " + wp.getClass().getName() + " not supported for idependent incident source."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
