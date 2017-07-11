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

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.qa.model.IQaDataProvider;
import org.wcs.smart.qa.model.IQaRoutineType;
import org.wcs.smart.qa.patrol.ILabelProvider;
import org.wcs.smart.qa.patrol.ILabelProvider.Key;
import org.wcs.smart.qa.routine.LocationRoutineType;

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
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<?> getData(Session session, ConservationArea ca, Date startDate, Date endDate) {
		List<WaypointLocationData> waypoints = new ArrayList<>();
		
		List<Waypoint> pws = session.createCriteria(Waypoint.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
				.add(Restrictions.eq("sourceId", PatrolWaypointSource.PATROL_WP_SOURCE_ID)) //$NON-NLS-1$
				.add(Restrictions.between("dateTime", startDate, endDate)) //$NON-NLS-1$
				.list();
		for (Waypoint wp : pws){
			waypoints.add(new WaypointLocationData(wp));
		}
		
		return waypoints;
	}

	@Override
	public String getFeatureId(Session session, Object obj, Locale l){
		PatrolWaypoint pw = (PatrolWaypoint) session.createCriteria(PatrolWaypoint.class)
				.add(Restrictions.eq("id.waypoint", ((WaypointLocationData)obj).getWaypoint())) //$NON-NLS-1$
				.uniqueResult();
		if (pw == null){
			return ILabelProvider.getLabel(Key.PatrolWaypointDataProvider_WpNotFound, l);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(pw.getPatrolLegDay().getPatrolLeg().getPatrol().getId());
		sb.append(" - "); //$NON-NLS-1$
		sb.append(ILabelProvider.getLabel(Key.PatrolWaypointDataProvider_WpIdLabel, l));
		sb.append(pw.getWaypoint().getId());
		sb.append(" ("); //$NON-NLS-1$
		sb.append(DateFormat.getDateTimeInstance().format(pw.getWaypoint().getDateTime()));
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
	public Image getImage() {
		return SmartContext.INSTANCE.getClass(ILabelProvider.class).getImage(getClass());
	}
}
