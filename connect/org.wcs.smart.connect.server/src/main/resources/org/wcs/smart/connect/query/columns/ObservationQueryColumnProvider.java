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
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn.FixedColumns;
import org.wcs.smart.observation.query.model.columns.IObservationQueryColumnProvider;
import org.wcs.smart.observation.query.model.columns.ObservationAttributeQueryColumn;
import org.wcs.smart.observation.query.model.columns.ObservationCategoryQueryColumn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;

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

	@SuppressWarnings("unchecked")
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
		
		int ccnt = QueryManager.INSTANCE.getCategoryDepth(session, q.getConservationArea().getUuid());
		for (int i = 0; i < ccnt; i ++){
			keys.add(new ObservationCategoryQueryColumn("Category " + i, i));
		}
		
		//attributes
		List<Attribute> atts = session.createCriteria(Attribute.class)
				.add(Restrictions.eq("conservationArea", q.getConservationArea()))
				.list();
		List<QueryColumn> attributes = new ArrayList<QueryColumn>();
		for (Attribute a : atts){
			attributes.add(new ObservationAttributeQueryColumn(a.getName(), a.getKeyId(), a.getType()));
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
	
//	@Override
//	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public QueryColumn[] getQueryColumns(Query query) {
//		String queryTypeKey = query.getTypeKey();
//		if (queryTypeKey.equals(ObsObservationQuery.KEY)){
//			return ObservationQueryColumnCache.getInstance().getObservationQueryColumns();
//		}else if (queryTypeKey.equals(ObservationWaypointQuery.KEY)){
//			return ObservationQueryColumnCache.getInstance().getWaypointQueryColumns();
//		}else if (queryTypeKey.equals(ObservationGriddedQuery.KEY)){
//			return ObservationQueryColumnCache.getInstance().getGridColumns();
//		}
//		return null;
//	}

}
