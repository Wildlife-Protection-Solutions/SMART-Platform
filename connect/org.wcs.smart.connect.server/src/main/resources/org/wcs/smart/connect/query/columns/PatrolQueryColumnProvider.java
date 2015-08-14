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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.patrol.query.model.observation.FixedQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.patrol.query.model.observation.PatrolCategoryQueryColumn;
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
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(PatrolObservationQuery.KEY)){
				return getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(PatrolWaypointQuery.KEY)){
				return getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(PatrolGriddedQuery.KEY)){
				return getGriddedQueryColumns(query, l, session);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public QueryColumn[] getObservationQueryColumns(Query q, Locale l, Session session) throws SQLException{
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
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_TIME,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_X,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_Y,l));
		if (trackDistanceDirection(q.getConservationArea(), session)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION,l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE,l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER,l));
		
		int ccnt = QueryManager.INSTANCE.getCategoryDepth(session, q.getConservationArea().getUuid());
		for (int i = 0; i < ccnt; i ++){
			keys.add(new PatrolCategoryQueryColumn("Category " + i, i));
		}
		
		//attributes
		List<Attribute> atts = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", q.getConservationArea()))
				.list();
		List<QueryColumn> attributes = new ArrayList<QueryColumn>();
		for (Attribute a : atts){
			attributes.add(new PatrolAttributeQueryColumn(a.getName(), a.getKeyId(), a.getType()));
		}
		Collections.sort(attributes, new Comparator<QueryColumn>() {

			@Override
			public int compare(QueryColumn o1, QueryColumn o2) {
				return Collator.getInstance(l).compare(o1.getName(), o2.getName());
			}
		});
		keys.addAll(attributes);
		
		return keys.toArray(new QueryColumn[keys.size()]);
	}
	
	public QueryColumn[] getWaypointQueryColumns(Query q, Locale l, Session session) throws SQLException{
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
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.TRANSPORT_TYPE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_ID,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DATE,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_TIME,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_X,l));
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_Y,l));
		if (trackDistanceDirection(q.getConservationArea(), session)){
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION,l));
			keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE,l));
		}
		keys.add(new FixedQueryColumn(FixedQueryColumn.FixedColumns.WAYPOINT_COMMENT,l));
		
		return keys.toArray(new QueryColumn[keys.size()]);
	}
	
	public QueryColumn[] getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
	
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public boolean trackDistanceDirection(ConservationArea ca, Session s){
		ObservationOptions op = getOptions(ca, s);
		if (op == null) return true;
		return op.getTrackDistanceDirection();
	}
	
	public ObservationOptions getOptions(ConservationArea ca, Session s){
		return (ObservationOptions) s.get(ObservationOptions.class, ca.getUuid());
		
	}
}
