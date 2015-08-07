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
package org.wcs.smart.entity.query.model.columns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.IEntityQueryColumnProvider;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * Query column provider implementation.
 * 
 * @author Emily
 *
 */
public class EntityQueryColumnProvider implements IEntityQueryColumnProvider{

	@Override
	public QueryColumn[] getQueryColumns(Query query) {
		String queryTypeKey = query.getTypeKey();
		if (queryTypeKey.equals(EntityObservationQuery.KEY)){
			QueryColumn[] col = EntityQueryColumnCache.getInstance().getObservationQueryColumns();
			List<QueryColumn> all = new ArrayList<QueryColumn>();
			for (QueryColumn c : col){
				all.add(c);
			}
			try{
				all.addAll(processEntityTypes( ((EntityObservationQuery)query).getFilter() ));
			}catch(Exception ex){
				EntityQueryPlugIn.log(ex.getMessage(), ex);
			}
			return all.toArray(new QueryColumn[all.size()]);
		}
		if (queryTypeKey.equals(EntityWaypointQuery.KEY)){
			return EntityQueryColumnCache.getInstance().getWaypointQueryColumns();
		}
		if (queryTypeKey.equals(EntityGriddedQuery.KEY)){
			return EntityQueryColumnCache.getInstance().getGridColumns();
		}
		return null;
	}

	
	private List<QueryColumn> processEntityTypes(QueryFilter filter){
		//initialize add entity type columns

		final Set<String> entityTypes = new HashSet<String>();
		filter.getFilter().accept(new IFilterVisitor() {			
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof EntityAttributeFilter){
					entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
				}
			}
		});
		final List<QueryColumn> queryColumns = new ArrayList<QueryColumn>();
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
		return queryColumns;
	}
}
