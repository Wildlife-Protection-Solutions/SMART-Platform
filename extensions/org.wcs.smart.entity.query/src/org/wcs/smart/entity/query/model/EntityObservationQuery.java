package org.wcs.smart.entity.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.engine.DerbyEntityObservationEngine;
import org.wcs.smart.entity.query.model.columns.EntityAttributeQueryColumn;
import org.wcs.smart.entity.query.model.columns.EntityQueryColumnCache;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
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
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;
@Entity
@Table(name="smart.entity_observation_query")
public class EntityObservationQuery extends ObservationQuery {
	
	@Override
	protected void initQueryColumns() {
		if (this.queryColumns != null){
			return;
		}
		
		synchronized (this) {
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
				
			}
			
			//initialize add entity type columns
			QueryFilter filter = getFilter();
			final Set<String> entityTypes = new HashSet<String>();
			filter.getFilter().accept(new IFilterVisitor() {			
				@Override
				public void visit(IFilter filter) {
					if (filter instanceof EntityAttributeFilter){
						entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
					}
				}
			});
		
			Job j = new Job("query columns"){ //$NON-NLS-1$
				//done in job so it has it's own session
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session session = HibernateManager.openSession();
					try{
						for (String entityType : entityTypes){
							EntityType et = EntityHibernateManager.getEntityType(entityType, session);
							for (EntityAttribute ea : et.getAttributes()){
								EntityAttributeQueryColumn newcol = new EntityAttributeQueryColumn("[" + et.getName() + "]" + ea.getName(), et.getKeyId(), ea.getKeyId(), ea.getDmAttribute().getType()); //$NON-NLS-1$ //$NON-NLS-2$
								queryColumns.add(newcol);
							}
						}
					}catch (Exception ex){
						EntityQueryPlugIn.log(ex.getMessage(), ex);
					}finally{
						session.close();
					}
					return Status.OK_STATUS;
				}};
			j.setSystem(true);	
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				EntityQueryPlugIn.log(e.getMessage(), e);
			}
			
			
			for (QueryColumn c : queryColumns){
				if (visible == null){
					c.setVisible(true);
				}else if (visible.contains(c.getKey())){
					c.setVisible(true);
				}else{
					c.setVisible(false);
				}
			}
			
		}
		
		

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
		try(InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes())){
			Parser parser = new Parser(is);
			QueryFilter myQuery = parser.QueryFilter();
			queryFilter = myQuery;
			return myQuery;
		}
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
		q.setStyle(getStyle());
		return q;
	}

}
