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
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column provider implementation for entity queries.
 * 
 * @author Emily
 *
 */
public class EntityQueryColumnProvider implements IEntityQueryColumnProvider{

	private static Logger logger = Logger.getLogger(EntityQueryColumnProvider.class.getName());
	
	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		List<QueryColumn> cols = null;
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(EntityObservationQuery.KEY)){
				cols = getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(EntityWaypointQuery.KEY)){
				cols = getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(EntityGriddedQuery.KEY)){
				cols = getGriddedQueryColumns(query, l, session);
			}
			
			if (cols != null){
				QueryColumnUtils.filterQueryColumns(cols, query);
				return cols.toArray(new QueryColumn[cols.size()]);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}

	public List<QueryColumn> getObservationQueryColumns(Query query, Locale l,  Session s) throws SQLException {
		ObservationOptions ops = QueryColumnUtils.getOptions(query.getConservationArea(), s);
		ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID
					|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
				add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
					|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
				add = QueryColumnUtils.trackDistanceDirection(ops);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = QueryColumnUtils.trackObserver(ops);
			}
			if (add) {
				cols.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
		}

		for (QueryColumn q : QueryColumnUtils.getDataModelColumns(s, l, query)){
			cols.add(q);
		}
		
		return cols;
	}
	
	public List<QueryColumn> getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols;
	}
	
	public List<QueryColumn> getWaypointQueryColumns(Query query, Locale l,  Session s) throws SQLException {
		ObservationOptions ops = QueryColumnUtils.getOptions(query.getConservationArea(), s);
		ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID
					|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
				add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
					|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
				add = QueryColumnUtils.trackDistanceDirection(ops);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = QueryColumnUtils.trackObserver(ops);
			}
			if (add) {
				cols.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
		}
		return cols;
	}
}
