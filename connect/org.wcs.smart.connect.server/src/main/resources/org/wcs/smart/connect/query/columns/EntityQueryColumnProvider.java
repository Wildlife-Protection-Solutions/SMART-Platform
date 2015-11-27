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
		try{
			String queryTypeKey = query.getTypeKey();
			if (queryTypeKey.equals(EntityObservationQuery.KEY)){
				return getObservationQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(EntityWaypointQuery.KEY)){
				return getWaypointQueryColumns(query, l, session);
			}else if (queryTypeKey.equals(EntityGriddedQuery.KEY)){
				return getGriddedQueryColumns(query, l, session);
			}
		}catch (SQLException ex){
			logger.log(Level.SEVERE, "Error determining query columns.", ex);
			return null;
		}
		return null;
	}

	public QueryColumn[] getObservationQueryColumns(Query query, Locale l,  Session s) throws SQLException {
		ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID
					|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
				add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
					|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
				add = trackDistanceDirection(query.getConservationArea(), s);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = trackObserver(query.getConservationArea(), s);
			}
			if (add) {
				cols.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
		}

		for (QueryColumn q : DataModelColumnProvider.getDataModelColumns(s, l, query)){
			cols.add(q);
		}

		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public QueryColumn[] getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols.toArray(new QueryColumn[cols.size()]);
	}
	
	public QueryColumn[] getWaypointQueryColumns(Query query, Locale l,  Session s) throws SQLException {
		ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
			FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns.values()[i];
			boolean add = true;
			if (item == FixedQueryColumn.FixedColumns.CA_ID
					|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
				add = query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
					|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
				add = trackDistanceDirection(query.getConservationArea(), s);
			} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
				add = trackObserver(query.getConservationArea(), s);
			}
			if (add) {
				cols.add(new FixedQueryColumn(item, Locale.getDefault()));
			}
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
//
//	
//	private List<QueryColumn> processEntityTypes(QueryFilter filter){
//		//initialize add entity type columns
//
//		final Set<String> entityTypes = new HashSet<String>();
//		filter.getFilter().accept(new IFilterVisitor() {			
//			@Override
//			public void visit(IFilter filter) {
//				if (filter instanceof EntityAttributeFilter){
//					entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
//				}
//			}
//		});
//		final List<QueryColumn> queryColumns = new ArrayList<QueryColumn>();
//		Job j = new Job("query columns"){ //$NON-NLS-1$
//			//done in job so it has it's own session
//			@Override
//			protected IStatus run(IProgressMonitor monitor) {
//				Session session = HibernateManager.openSession();
//				try{
//					for (String entityType : entityTypes){
//						EntityType et = EntityHibernateManager.getEntityType(entityType, session);
//						for (EntityAttribute ea : et.getAttributes()){
//							EntityAttributeQueryColumn newcol = new EntityAttributeQueryColumn("[" + et.getName() + "]" + ea.getName(), et.getKeyId(), ea.getKeyId(), ea.getDmAttribute().getType()); //$NON-NLS-1$ //$NON-NLS-2$
//							queryColumns.add(newcol);
//						}
//					}
//				}catch (Exception ex){
//					EntityQueryPlugIn.log(ex.getMessage(), ex);
//				}finally{
//					session.close();
//				}
//				return Status.OK_STATUS;
//			}};
//		j.setSystem(true);	
//		j.schedule();
//		try {
//			j.join();
//		} catch (InterruptedException e) {
//			EntityQueryPlugIn.log(e.getMessage(), e);
//		}
//		return queryColumns;
//	}
}
