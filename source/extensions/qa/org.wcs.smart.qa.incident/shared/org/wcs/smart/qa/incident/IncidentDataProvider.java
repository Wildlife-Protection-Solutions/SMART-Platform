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
package org.wcs.smart.qa.incident;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType;

/**
 * QA Data provider for independent incidents
 * 
 * @author Emily
 *
 */
public class IncidentDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.incident.waypoint"; //$NON-NLS-1$
	
	@Override
	public String getName(Locale l) {
		return "Independent Incident";
	}

	public String getId(){
		return ID;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, Date startDate, Date endDate) {
		List<IncidentLocationData> waypoints = new ArrayList<>();
		
		Query query = session.createQuery("FROM Waypoint WHERE conservationArea = :ca and sourceId = :source and dateTime between :start AND :end"); //$NON-NLS-1$
		query.setParameter("ca", ca); //$NON-NLS-1$
		query.setParameter("source",  IndepedentIncidentSource.KEY); //$NON-NLS-1$
		query.setParameter("start",  startDate); //$NON-NLS-1$
		query.setParameter("end", endDate); //$NON-NLS-1$
		List<Waypoint> pws = query.list();
		for (Waypoint wp : pws){
			waypoints.add(new IncidentLocationData(wp));
		}
		
		return waypoints;
	}

	@Override
	public String getFeatureId(Session session, Object obj){
		Waypoint waypoint = (Waypoint) session.get(Waypoint.class, ((IncidentLocationData)obj).getWaypoint().getUuid());
		if (waypoint == null){
			return "Indepdence Incident not found - data error";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Waypoint ID ");
		sb.append(waypoint.getId());
		sb.append(" (");
		sb.append(DateFormat.getDateTimeInstance().format(waypoint.getDateTime()));
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
		return ((IncidentLocationData)obj).getWaypoint().getUuid();
	}

}
