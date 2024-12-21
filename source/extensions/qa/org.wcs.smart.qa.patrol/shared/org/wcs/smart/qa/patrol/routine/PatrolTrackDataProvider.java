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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;
import org.wcs.smart.qa.routine.LocationRoutineType;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

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
		return ILabelProvider.getLabel(Key.PatrolTrackDataProvider_Name, l);
	}

	public String getId(){
		return ID;
	}
	
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, LocalDate startDate, LocalDate endDate) {
		List<TrackLocationData> tracks = new ArrayList<>();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Patrol> c = cb.createQuery(Patrol.class);
		Root<Patrol> from = c.from(Patrol.class);
		c.where(cb.and(
				cb.equal(from.get("conservationArea"), ca), //$NON-NLS-1$
				cb.between(from.get("startDate"), cb.literal(startDate), cb.literal(endDate)) //$NON-NLS-1$
				));
		
		List<Patrol> patrols = session.createQuery(c).getResultList();
		for (Patrol p : patrols){
			for (PatrolLeg pl : p.getLegs()){
				for (PatrolLegDay pld : pl.getPatrolLegDays()){
					if ((pld.getDate().isEqual(endDate) || pld.getDate().isBefore(endDate)) && 
							(pld.getDate().isEqual(startDate)  || pld.getDate().isAfter(startDate))){
						Track t = pld.getTrack();
						try{
							if (t != null && t.getGeometry() != null){
								tracks.add(new TrackLocationData(t));
							}
						}catch (Exception ex){
							Logger.getLogger(PatrolTrackDataProvider.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
						}
					}
				}
			}
		}
		
		return tracks;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		Track track = (Track) session.get(Track.class, ((TrackLocationData)obj).getTrack().getUuid());

		if (track == null){
			return ILabelProvider.getLabel(Key.PatrolTrackDataProvider_TrackNotFound, l);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(track.getPatrolLegDay().getPatrolLeg().getPatrol().getId());
		if (track.getPatrolLegDay().getPatrolLeg().getPatrol().getLegs().size() > 1){
			sb.append(" - "); //$NON-NLS-1$
			sb.append(ILabelProvider.getLabel(Key.PatrolTrackDataProvider_LegLabel, l));
			sb.append(track.getPatrolLegDay().getPatrolLeg().getId());
		}
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(track.getPatrolLegDay().getDate()));
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}
	
	@Override
	public boolean supportsRoutine(IQaRoutineType type) {
		if (type.getId().equals(LocationRoutineType.ID)) return true;
		if (type.getId().equals(PatrolSpeedRoutineType.ID)) return true;
		return false;
	}

	@Override
	public UUID getFeatureSource(Session session, Object obj) {
		return ((TrackLocationData)obj).getTrack().getUuid();
	}

	public boolean recheck(Session session, QaError error) {
		Track t = session.get(Track.class, error.getSourceId());
		if (t == null) return false;
		if (error.getStatus() == Status.UNKNOWN) return true;
		Geometry current = null;
		try {
			current = t.getGeometry();
		} catch (ParseException e) {
			Logger.getLogger(PatrolTrackDataProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		if (current != null && !current.equalsExact(error.getGeometryObject())) {
			error.setStatus(QaError.Status.UNKNOWN);
			error.setGeometryObject(current);
		}
		return true;
	}
}
