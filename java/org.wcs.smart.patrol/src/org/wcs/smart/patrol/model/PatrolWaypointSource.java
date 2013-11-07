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
import org.wcs.smart.util.SmartUtils;

public class PatrolWaypointSource implements IWaypointSource {

	public static String PATROL_WP_SOURCE_ID = "PATROL";
	
	public PatrolWaypointSource() {
	}

	@Override
	public String getKey() {
		return PATROL_WP_SOURCE_ID;
	}

	@Override
	public String getName() {
		return "Patrol";
	}

	@Override
	public String getDatastoreFileLocation(final Waypoint wp) {

		//need to determine the patrol this waypoint is associated
		//with; do in a different thread so we can have our own database
		//connection
		final String[] patrolDir = new String[]{null};
		Job j = new Job("get datastore location"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				try{
					List<?> pws = s.createCriteria(PatrolWaypoint.class).add(Restrictions.eq("id.waypoint", wp)).list();
					if (pws.size() > 0){
						patrolDir[0] = SmartUtils.getDirectoryPath(((PatrolWaypoint)pws.get(0)).getPatrolLegDay().getPatrolLeg().getPatrol().getUuid());
					}else{
						SmartPatrolPlugIn.log("Patrol waypoint could not be found for waypoint " + SmartUtils.encodeHex(wp.getUuid()), null);
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
			SmartPatrolPlugIn.log("Patrol waypoint could not be found for waypoint " + SmartUtils.encodeHex(wp.getUuid()), null);
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
