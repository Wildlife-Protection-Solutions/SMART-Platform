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
package org.wcs.smart.patrol.model;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.util.UuidUtils;

/**
 * 
 * Patrol waypoint source.
 * 
 * @author Emily
 *
 */
public class PatrolWaypointSource implements IWaypointSource {

	public static String PATROL_WP_SOURCE_ID = "PATROL"; //$NON-NLS-1$
	
	public PatrolWaypointSource() {
	}

	@Override
	public String getKey() {
		return PATROL_WP_SOURCE_ID;
	}

	@Override
	public String getName() {
		return Messages.PatrolWaypointSource_PatrolWaypointSourceName;
	}

	public String getDatastoreFileLocation(Patrol p){
		StringBuilder sb = new StringBuilder();
		sb.append(Patrol.PATROL_FILESTORE_LOC);
		sb.append(File.separator);
		sb.append(UuidUtils.getDirectoryPath(p.getUuid()));
		sb.append(File.separator);
		return sb.toString();
	}
	
	@Override
	public String getDatastoreFileLocation(Object source) {
		if (source instanceof Waypoint){
			return getDatastoreFileLocation((Waypoint)source);
		}else if (source instanceof Patrol){
			return getDatastoreFileLocation((Patrol)source);
		}
		return null;
	}
	
	public String getDatastoreFileLocation(final Waypoint wp) {
		if (wp.getUuid() == null){
			return null;
		}
		//need to determine the patrol this waypoint is associated
		//with; do in a different thread so we can have our own database
		//connection
		final String[] patrolDir = new String[]{null};
		Job j = new Job("get datastore location"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					List<?> pws = s.createCriteria(PatrolWaypoint.class).add(Restrictions.eq("id.waypoint", wp)).list(); //$NON-NLS-1$
					if (pws.size() > 0){
						patrolDir[0] = UuidUtils.getDirectoryPath(((PatrolWaypoint)pws.get(0)).getPatrolLegDay().getPatrolLeg().getPatrol().getUuid());
					}else{
						SmartPatrolPlugIn.log(Messages.PatrolWaypointSource_WaypointNotFoundError + UuidUtils.uuidToString(wp.getUuid()), null);
					}
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}};
		j.setSystem(true);
		j.schedule();
		
		try {
			j.join();
		} catch (InterruptedException e) {
			SmartPatrolPlugIn.log(Messages.PatrolWaypointSource_WaypointNotFoundError + UuidUtils.uuidToString(wp.getUuid()), null);
			return null;
		}
			
		StringBuilder sb = new StringBuilder();
		sb.append(Patrol.PATROL_FILESTORE_LOC);
		sb.append(File.separator);
		if (patrolDir[0] != null){
			sb.append(patrolDir[0]);
			sb.append(File.separator);
		}
		
		return sb.toString();
	}

}
