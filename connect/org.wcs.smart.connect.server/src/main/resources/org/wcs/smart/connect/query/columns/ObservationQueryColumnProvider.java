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
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.model.columns.IObservationQueryColumnProvider;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Column provider implementation for all data queries.
 * 
 * @author Emily
 *
 */
public class ObservationQueryColumnProvider implements IObservationQueryColumnProvider {
	
	private static Logger logger = Logger.getLogger(PatrolQueryColumnProvider.class.getName());
	
	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(ObsObservationQuery.KEY)){
				return getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(ObservationWaypointQuery.KEY)){
				return getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(ObservationGriddedQuery.KEY)){
				return getGriddedQueryColumns(query, l, session);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}

	public QueryColumn[] getObservationQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID || 
					item == FixedQueryColumn.FixedColumns.CA_NAME){
				add = q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
				item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
				add = trackDistanceDirection(q.getConservationArea(), session);
			}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = trackObserver(q.getConservationArea(), session);
			}
			if (add){
				keys.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
		}
		
		for (QueryColumn cq : DataModelColumnProvider.getDataModelColumns(session, l, q)){
			keys.add(cq);
		}
		
		return keys.toArray(new QueryColumn[keys.size()]);
	}
	
	public QueryColumn[] getWaypointQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> keys = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID || 
					item == FixedQueryColumn.FixedColumns.CA_NAME){
				add = q.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION ||  
				item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE){
				add = trackDistanceDirection(q.getConservationArea(), session);
			}else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = trackObserver(q.getConservationArea(), session);
			}
			if (add){
				keys.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
		}
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
	
	public boolean trackObserver(ConservationArea ca, Session s){
		ObservationOptions op = getOptions(ca, s);
		if (op == null) return true;
		return op.getTrackObserver();
	}
	
	public ObservationOptions getOptions(ConservationArea ca, Session s){
		return (ObservationOptions) s.get(ObservationOptions.class, ca.getUuid());
	}


}
