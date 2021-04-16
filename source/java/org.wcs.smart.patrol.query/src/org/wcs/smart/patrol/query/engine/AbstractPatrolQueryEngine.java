/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.patrol.query.engine;

import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AbstractPatrolQueryEngine extends AbstractQueryEngine implements IPatrolQueryEngine{
	
	static {
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(PatrolWaypoint.class, "pwp"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Team.class, "smart.team"); //$NON-NLS-1$
		tablePrefix.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tablePrefix.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$

	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(PatrolWaypoint.class, "smart.patrol_waypoint"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Team.class, "smart.team"); //$NON-NLS-1$
		tableNames.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tableNames.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
	}


	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	@Override
	public IFilterProcessor getFilterProcessor(FilterType filterType, 
			String queryDataTable, Query query){
		if (filterType == FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, query);
		}else if (filterType == FilterType.GROUP) {
			return new WaypointGroupFilterProcessor(queryDataTable, this, query);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, query);
		}
	}
	

}
