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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.entity.query.model.columns.EntityAttributeQueryColumn;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.entity.query.parser.internal.EntityTypeFilter;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

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
			logger.log(Level.SEVERE, "Error determining query columns.", ex); //$NON-NLS-1$
			return null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<QueryColumn> getObservationQueryColumns(Query query, Locale l,  Session s) throws SQLException {
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
				cols.add(new FixedQueryColumn(item, l));
			}
		}

		ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
		for (QueryColumn q : QueryColumnUtils.getDataModelColumns(s, l, caFilter)){
			cols.add(q);
		}
		
		//add entity attributes for any entity types included in filter
		try{
			Set<String> entityTypes = new HashSet<String>();
		
			((SimpleQuery)query).getFilter().getFilter().accept(new IFilterVisitor() {			
				@Override
				public void visit(IFilter filter) {
					if (filter instanceof EntityAttributeFilter){
						entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
					}else if (filter instanceof EntityTypeFilter){
						entityTypes.add(((EntityTypeFilter) filter).getEntityTypeKey());
					}
				}
			});
			
			if (entityTypes.size() > 0){
				String hql = "SELECT a.keyId as att_key, b.keyId as entity_key, c.type FROM EntityAttribute a join a.entityType b join a.dmAttribute c " //$NON-NLS-1$
						+ "WHERE b.conservationArea.uuid IN (:cauuids) and b.keyId in (:entitytypes)"; //$NON-NLS-1$
				org.hibernate.Query hq = s.createQuery(hql);
				hq.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
				hq.setParameterList("entitytypes", entityTypes); //$NON-NLS-1$
				
				List<Object[]> attributes = hq.list();
				List<EntityAttributeQueryColumn> entityAttributeColumns = new ArrayList<EntityAttributeQueryColumn>();
				for (Object[] att : attributes){
					String attributeKey = (String) att[0];
					String entityType = (String) att[1];
					AttributeType type = (AttributeType) att[2];
					
					//find attribute name for entity attribute; this is complicated 
					//as it may be defined or it may be the attribute
					//we also need to support cross-ca 
					EntityAttribute ea = (EntityAttribute) s.createCriteria(EntityAttribute.class)
							.add(Restrictions.eq("keyId", attributeKey)) //$NON-NLS-1$
							.createCriteria("entityType", "et") //$NON-NLS-1$ //$NON-NLS-2$
							.add(Restrictions.eq("et.keyId", entityType)) //$NON-NLS-1$
							.uniqueResult();
					entityAttributeColumns.add(new EntityAttributeQueryColumn("[" + ea.getEntityType().getName() + "]" + ea.getName(), entityType, attributeKey, type)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				QueryColumnUtils.sortByName(entityAttributeColumns, l);
				
				cols.addAll(entityAttributeColumns);
			}
			
		}catch (Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
		}
		
		
		return cols;
	}
	
	private  List<QueryColumn> getGriddedQueryColumns(Query q, Locale l, Session session) throws SQLException{
		List<QueryColumn> cols = new ArrayList<QueryColumn>();
		for (GridQueryColumn.GridColumns t : GridQueryColumn.GridColumns.values()){
			cols.add(new GridQueryColumn(t,l));
		}
		return cols;
	}
	
	private  List<QueryColumn> getWaypointQueryColumns(Query query, Locale l,  Session s) throws SQLException {
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
				add = false;
			}
			if (add) {
				cols.add(new FixedQueryColumn(item, l));
			}
		}
		return cols;
	}
}
