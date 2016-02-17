/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.columns;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column implementation for patrol queries.
 * 
 * @author Emily
 *
 */
public class PatrolQueryColumnProvider implements IPatrolQueryColumnProvider {
	private static Logger logger = Logger.getLogger(PatrolQueryColumnProvider.class.getName());
	
	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		List<QueryColumn> cols = null;
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(PatrolObservationQuery.KEY)){
				cols = getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(PatrolWaypointQuery.KEY)){
				cols = getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(PatrolGriddedQuery.KEY)){
				cols = getGriddedQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(PatrolQuery.KEY)){
				cols = getPatrolQueryColumns(query, l, session);
			}
			if (cols != null){
				QueryColumnUtils.filterQueryColumns(cols, query);
				return cols.toArray(new QueryColumn[cols.size()]);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex); //$NON-NLS-1$
			return null;
		}
		return null;
	}

	private List<QueryColumn> getPatrolQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		
		if (q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_ID, l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_NAME, l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_START_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_END_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_STATION,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TEAM,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_MANDATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ARMED,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_START_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_END_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE,l));
//		keys.add(new QueryColumn("Patrol Track", "track",ColumnType.STRING){
//			@Override
//			public QueryColumn clone() {
//				return null;
//			}
//
//			@Override
//			public Object getValue(IResultItem arg0) {
//				return null;
//			}});
		
		return keys;
	}
	
	private List<QueryColumn> getObservationQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		ObservationOptions ops = QueryColumnUtils.getOptions(q.getConservationArea(), session);
		if (q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_ID, l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_NAME, l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_START_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_END_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_STATION,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TEAM,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_MANDATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ARMED,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_TIME,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_X,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_Y,l));
		if (QueryColumnUtils.trackDistanceDirection(ops)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION,l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE,l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT,l));
		if (QueryColumnUtils.trackObserver(ops)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER,l));
		}
		
		for (QueryColumn qc : QueryColumnUtils.getDataModelColumns(session, l, AbstractQueryEngine.parseConservationAreaFilter(q))){
			keys.add(qc);
		}
		
		return keys;
	}
	
	private List<QueryColumn> getWaypointQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		ObservationOptions ops = QueryColumnUtils.getOptions(q.getConservationArea(), session);
		if (q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_ID, l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.CA_NAME, l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_START_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_END_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_STATION,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_TEAM,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_OBJETIVE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_MANDATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_ARMED,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_LEADER,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.PATROL_LEG_PILOT,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_TIME,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_X,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_Y,l));
		if (QueryColumnUtils.trackDistanceDirection(ops)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION,l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE,l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT,l));
		
		return keys;
	}
	
	private List<QueryColumn> getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols;
	}

}
