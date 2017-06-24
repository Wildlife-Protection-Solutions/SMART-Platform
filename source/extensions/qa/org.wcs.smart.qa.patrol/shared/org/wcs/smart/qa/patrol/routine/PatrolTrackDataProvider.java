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
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType;

public class PatrolTrackDataProvider implements IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.patrol.track";
	
	@Override
	public String getName(Locale l) {
		return "Patrol Tracks";
	}

	public String getId(){
		return ID;
	}
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, Date startDate, Date endDate) {
		List<TrackLocationData> tracks = new ArrayList<>();
		
		List<Patrol> patrols = session.createCriteria(Patrol.class)
				.add(Restrictions.eq("conservationArea", ca))
				.add(Restrictions.between("startDate", startDate, endDate))
				.list();
		for (Patrol p : patrols){
			for (PatrolLeg pl : p.getLegs()){
				for (PatrolLegDay pld : pl.getPatrolLegDays()){
					if ((pld.getDate().equals(endDate) || pld.getDate().before(endDate)) && (pld.getDate().equals(startDate)  || pld.getDate().after(startDate))){
						try{
							if (pld.getTrack().getLineString() != null){
								tracks.add(new TrackLocationData(pld.getTrack()));
							}
						}catch (Exception ex){
							ex.printStackTrace();
							//TODO:
						}
					}
				}
			}
		}
		
		return tracks;
	}

	@Override
	public String getFeatureId(Session session, Object obj){
		Track track = (Track) session.get(Track.class, ((TrackLocationData)obj).getTrack().getUuid());

		if (track == null){
			return "Patrol Track not found - data error";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(track.getPatrolLegDay().getPatrolLeg().getPatrol().getId());
		if (track.getPatrolLegDay().getPatrolLeg().getPatrol().getLegs().size() > 1){
			sb.append(" - Leg ");
			sb.append(track.getPatrolLegDay().getPatrolLeg().getId());
		}
		sb.append(" (");
		sb.append(DateFormat.getDateInstance().format(track.getPatrolLegDay().getDate()));
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
		return ((TrackLocationData)obj).getTrack().getUuid();
	}

}
