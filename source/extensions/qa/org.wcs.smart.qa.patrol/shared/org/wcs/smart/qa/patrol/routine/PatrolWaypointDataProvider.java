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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;
import org.wcs.smart.qa.routine.LocationRoutineType;
import org.wcs.smart.qa.routine.WaypointLocationData;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Data provider for patrol waypoints.
 * 
 * @author Emily
 *
 */
public class PatrolWaypointDataProvider extends IQaDataProvider {

	public static final String ID = "org.wcs.smart.qa.dataprovider.patrol.waypoint"; //$NON-NLS-1$
		
	@Override
	public String getName(Locale l) {
		return ILabelProvider.getLabel(Key.PatrolWaypointDataProvider_Name, l);
	}

	public String getId(){
		return ID;
	}
	
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, LocalDate startDate, LocalDate endDate) {
		List<WaypointLocationData> waypoints = new ArrayList<>();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Waypoint> c = cb.createQuery(Waypoint.class);
		Root<Waypoint> from = c.from(Waypoint.class);
		c.where(cb.and(
				cb.equal(from.get("conservationArea"), ca), //$NON-NLS-1$
				cb.equal(from.get("sourceId"), PatrolWaypointSource.PATROL_WP_SOURCE_ID), //$NON-NLS-1$
				cb.between(from.get("dateTime"), cb.literal(startDate.atStartOfDay()), cb.literal(endDate.atTime(LocalTime.MAX))) //$NON-NLS-1$
				));
		List<Waypoint> pws = session.createQuery(c).getResultList();
		
		for (Waypoint wp : pws){
			waypoints.add(new WaypointLocationData(wp));
		}
		
		return waypoints;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		PatrolWaypoint pw = QueryFactory.buildQuery(session, PatrolWaypoint.class, "id.waypoint", ((WaypointLocationData)obj).getWaypoint()).uniqueResult(); //$NON-NLS-1$
		
		if (pw == null){
			return ILabelProvider.getLabel(Key.PatrolWaypointDataProvider_WpNotFound, l);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(pw.getPatrolLegDay().getPatrolLeg().getPatrol().getId());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(ILabelProvider.getLabel(Key.PatrolWaypointDataProvider_WpIdLabel, l));
		sb.append(pw.getWaypoint().getId());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(pw.getWaypoint().getDateTime()));
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
		return ((WaypointLocationData)obj).getWaypoint().getUuid();
	}

	
	@Override
	public boolean recheck(Session session, QaError error) {
		Waypoint wp = session.get(Waypoint.class, error.getSourceId());
		if (wp == null) return false;
		//already checked and determine modified so we don't need to re-check
		if (error.getStatus() == Status.UNKNOWN) return true;
		
		if (!(error.getGeometryObject() instanceof Point)) return true;
		
		//check geometry object
		Point pnt = (Point)error.getGeometryObject();
		if (wp.getRawX() == pnt.getX() && wp.getRawY() == pnt.getY()) return true;

		//different geometries
		error.setStatus(QaError.Status.UNKNOWN);
		error.setGeometryObject(GeometryFactoryProvider.getFactory().createPoint(new Coordinate(wp.getRawX(), wp.getRawY())));
		return true;
	}
}
