package org.wcs.smart.qa.patrol.routine;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType;

public class PatrolWaypointDataProvider implements IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.patrol.waypoint";
	
	@Override
	public String getName(Locale l) {
		return "Patrol Waypoints";
	}

	public String getId(){
		return ID;
	}
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, Date startDate, Date endDate) {
		List<WaypointLocationData> waypoints = new ArrayList<>();
		
		List<Waypoint> pws = session.createCriteria(Waypoint.class)
				.add(Restrictions.eq("conservationArea", ca))
				.add(Restrictions.eq("sourceId", PatrolWaypointSource.PATROL_WP_SOURCE_ID))
				.add(Restrictions.between("dateTime", startDate, endDate))
				.list();
		for (Waypoint wp : pws){
			waypoints.add(new WaypointLocationData(wp));
		}
		
		return waypoints;
	}

	@Override
	public String getFeatureId(Session session, Object obj){
		PatrolWaypoint pw = (PatrolWaypoint) session.createCriteria(PatrolWaypoint.class)
				.add(Restrictions.eq("id.waypoint", ((WaypointLocationData)obj).getWaypoint()))
				.uniqueResult();
		if (pw == null){
			return "Patrol Waypoint not found - data error";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(pw.getPatrolLegDay().getPatrolLeg().getPatrol().getId());
		sb.append(" - Waypoint ID ");
		sb.append(pw.getWaypoint().getId());
		sb.append(" (");
		sb.append(DateFormat.getDateTimeInstance().format(pw.getWaypoint().getDateTime()));
		sb.append(")");
		return sb.toString();
	}
	@Override
	public boolean supportsRoutine(IQaRoutineType type) {
		if (type.getId().equals(LocationRoutineType.ID)) return true;
		return false;
	}

	@Override
	public UUID getFeatureSource(Session session, Object obj) {
		return ((WaypointLocationData)obj).getWaypoint().getUuid();
	}

}
