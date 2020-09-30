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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.er.ILabelProvider.Key;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType;
import org.wcs.smart.qa.routine.WaypointLocationData;

/**
 * Data provider for patrol waypoints.
 * 
 * @author Emily
 *
 */
public class ErWaypointDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.mission.waypoint"; //$NON-NLS-1$
	
	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.ErWaypointDataProvider_Name, l);
	}

	public String getId(){
		return ID;
	}
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, LocalDate startDate, LocalDate endDate) {
		List<WaypointLocationData> waypoints = new ArrayList<>();
		
		Query<Waypoint> query = session.createQuery("FROM Waypoint WHERE conservationArea = :ca AND sourceId = :source AND dateTime between :start and :end", Waypoint.class); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		query.setParameter("source", SurveyWaypointSource.KEY); //$NON-NLS-1$
		query.setParameter("start", startDate.atStartOfDay()); //$NON-NLS-1$
		query.setParameter("end", endDate.atTime(LocalTime.MAX)); //$NON-NLS-1$
		List<Waypoint> pws = query.list();
		for (Waypoint wp : pws){
			waypoints.add(new WaypointLocationData(wp));
		}
		
		return waypoints;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		SurveyWaypoint pw = QueryFactory.buildQuery(session, SurveyWaypoint.class, "id.waypoint", ((WaypointLocationData)obj).getWaypoint()).uniqueResult(); //$NON-NLS-1$
		if (pw == null){
			return ILabelProvider.getLabel(Key.ErWaypointDataProvider_WpNotFound, l);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(pw.getMissionDay().getMission().getId());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(ILabelProvider.getLabel(Key.ErWaypointDataProvider_WpIdLbl, l));
		sb.append(" "); //$NON-NLS-1$
		sb.append(pw.getWaypoint().getId());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(pw.getWaypoint().getDateTime()));
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
		return ((WaypointLocationData)obj).getWaypoint().getUuid();
	}
}
