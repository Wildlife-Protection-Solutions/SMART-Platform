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
package org.wcs.smart.qa.er;

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
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.qa.er.ILabelProvider.Key;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.routine.LocationRoutineType;

/**
 * Data provider for providing patrol track data.
 * 
 * @author Emily
 *
 */
public class ErTrackDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.mission.track"; //$NON-NLS-1$
	
	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.ErTrackDataProvider_Name, l);
	}

	public String getId(){
		return ID;
	}
	
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, LocalDate startDate, LocalDate endDate) {
		List<TrackLocationData> tracks = new ArrayList<>();
		
		Query<Mission> q = session.createQuery("FROM Mission WHERE survey.surveyDesign.conservationArea = :ca AND startDate between :start and :end", Mission.class); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.setParameter("start", startDate); //$NON-NLS-1$
		q.setParameter("end", endDate); //$NON-NLS-1$
		List<Mission> missions = q.list();
		
		for (Mission m: missions){
			for (MissionDay md : m.getMissionDays()){
				if ((md.getDate().isEqual(endDate) || md.getDate().isBefore(endDate)) && 
						(md.getDate().isEqual(startDate)  || md.getDate().isAfter(startDate))){
					for (MissionTrack t : md.getTracks()){
						try{
							if (t != null && t.getLineString() != null){
								tracks.add(new TrackLocationData(t));
							}
						}catch (Exception ex){
							Logger.getLogger(ErTrackDataProvider.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
						}
					}
				}
			}
		}
		
		return tracks;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		MissionTrack track = (MissionTrack) session.get(MissionTrack.class, ((TrackLocationData)obj).getTrack().getUuid());

		if (track == null){
			return ILabelProvider.getLabel(Key.ErTrackDataProvider_TrackNotFound, l);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(track.getId());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(track.getMissionDay().getMission().getId());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(track.getMissionDay().getDate()));
		sb.append(")"); //$NON-NLS-1$
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

	public boolean recheck(Session session, QaError error) {
		MissionTrack t = session.get(MissionTrack.class, error.getSourceId());
		if (t == null) return false;		
		if (error.getStatus() == Status.UNKNOWN) return true;		
		Geometry current = null;
		try {
			current = t.getLineString();
		} catch (Exception e) {
			Logger.getLogger(ErTrackDataProvider.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		if (current != null && !current.equalsExact(error.getGeometryObject())) {
			error.setStatus(QaError.Status.UNKNOWN);
			error.setGeometryObject(current);
		}
		return true;
	}
}
