/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType;

/**
 * Data provider for providing patrol track data.
 * 
 * @author Emily
 *
 */
public class PatrolTrackDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.patrol.track"; //$NON-NLS-1$
	
	@Override
	public String getName(Locale l) {
		return "Patrol Track";
	}

	public String getId(){
		return ID;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, Date startDate, Date endDate) {
		List<TrackLocationData> tracks = new ArrayList<>();
		
		List<Patrol> patrols = session.createCriteria(Patrol.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.between("startDate", startDate, endDate)) //$NON-NLS-1$
				.list();
		for (Patrol p : patrols){
			for (PatrolLeg pl : p.getLegs()){
				for (PatrolLegDay pld : pl.getPatrolLegDays()){
					if ((pld.getDate().equals(endDate) || pld.getDate().before(endDate)) && (pld.getDate().equals(startDate)  || pld.getDate().after(startDate))){
						Track t = pld.getTrack();
						try{
							if (t != null && t.getLineString() != null){
								tracks.add(new TrackLocationData(t));
							}
						}catch (Exception ex){
							QaPlugIn.log(ex.getMessage(), ex);
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
