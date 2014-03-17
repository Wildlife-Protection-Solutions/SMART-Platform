package org.wcs.smart.entity.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.entity.query.engine.DerbyEntityObservationEngine;
import org.wcs.smart.entity.query.model.columns.EntityQueryColumnCache;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.parser.internal.parser.Parser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;
@Entity
@Table(name="smart.entity_observation_query")
public class EntityObservationQuery extends ObservationQuery {
	
	@Override
	protected void initQueryColumns() {
		if (this.queryColumns != null){
			return;
		}
		QueryColumn[] cols = EntityQueryColumnCache.getInstance().getObservationQueryColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		
		HashSet<String> visible = null;
		if (visibleColumns != null){
			String[] bits = visibleColumns.split(","); //$NON-NLS-1$
			visible = new HashSet<String>();
			for (int i = 0; i < bits.length; i ++){
				visible.add(bits[i]);
			}
		}
		
		for (int i = 0; i < cols.length; i ++){
			queryColumns.add(cols[i]);
			if (visible == null){
				cols[i].setVisible(true);
			}else if (visible.contains(cols[i].getKey())){
				cols[i].setVisible(true);
			}else{
				cols[i].setVisible(false);
			}
		}
		
//		//add entity type columns
//		try{
//			List<EntityType> types = getEntityTypeList();
//		
//			for (EntityType e : types){
//				//TODO: dela with this error
//				for (EntityAttribute ea : e.getAttributes()){
//			
//					ColumnType ctype = ColumnType.STRING;
//					if (ea.getDmAttribute().getType() == AttributeType.NUMERIC ){
//						ctype = ColumnType.NUMBER;
//					}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN){
//						ctype = ColumnType.BOOLEAN;
//					}else {
//						ctype = ColumnType.STRING;
//					}
//			
//					AttributeQueryColumn qc = new EntityAttributeQueryColumn(
//					MessageFormat.format("{0}: {1}", new Object[]{e.getName(), ea.getName()}), 
//					"entity:" + ea.getKeyId(), ctype);
//			
//					queryColumns.add(qc);
//					if (visible == null){
//						qc.setVisible(true);
//					}else if (visible.contains(qc.getKey())){
//						qc.setVisible(true);
//					}else{
//						qc.setVisible(false);
//					}
//				}
//			}
//		} catch (Exception e1) {
//			EntityQueryPlugIn.displayLog("Error loading query columns." + e1.getMessage(), e1);
//		}
	}

	
	@Override
	protected IPagedQueryResultSet getPagedQueryResults(
			IProgressMonitor progressMonitor, Session session) throws Exception {
		Session lSession = session;
		if (lSession == null){
			lSession = HibernateManager.openSession();
			lSession.beginTransaction();
		}
		try {
			DerbyEntityObservationEngine engine = new DerbyEntityObservationEngine();
			IPagedQueryResultSet lastResult = engine.executeDerbyQuery(this, lSession, progressMonitor);
			return lastResult;
		} finally {
			if (session == null && lSession.isOpen()){
				lSession.getTransaction().commit();
				lSession.close();
			}
		}
	}

	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		QueryFilter myQuery = parser.QueryFilter();
		is.close();
		queryFilter = myQuery;
		return myQuery;
	}

	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(EntityObservationQueryType.KEY);
	}

	@Override
	public Query clone() {
		EntityObservationQuery q = new EntityObservationQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		return q;
	}

}
