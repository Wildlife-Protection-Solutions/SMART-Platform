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
package org.wcs.smart.patrol.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.engine.DerbyObservationEngine;
import org.wcs.smart.patrol.query.model.observation.PatrolQueryColumnCache;
import org.wcs.smart.patrol.query.model.types.PatrolObservationQueryType;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.IPagedQueryResultSet;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

/**
 * A class to represent an observation query.
 * <p>Observation queries query each observation
 * which consists of a category and a 
 * set of attributes.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.observation_query")
public class PatrolObservationQuery extends ObservationQuery{

	private Object LOCK = new Object();

	@Transient
	protected IPagedQueryResultSet getPagedQueryResults(IProgressMonitor progressMonitor, Session session) throws Exception {
		Session lsession = session;
		if (session == null){
			lsession = HibernateManager.openSession();
			lsession.beginTransaction();
		}
		try {
			DerbyObservationEngine engine = new DerbyObservationEngine();
			IPagedQueryResultSet lastResult = engine.executeDerbyQuery(this, lsession, progressMonitor);
			return lastResult;
		} finally {
			if (session == null && lsession.isOpen()){
				lsession.getTransaction().commit();
				lsession.close();
			}
		}
	}
	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	@Override
	public PatrolObservationQuery clone(){
		PatrolObservationQuery q = new PatrolObservationQuery();
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
	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(PatrolObservationQueryType.KEY);
	}
	
	@Override
	protected void initQueryColumns() {
			if (this.queryColumns != null){
				return;
			}
			synchronized (LOCK) {
				if (this.queryColumns != null){
					return;
				}	
			
				QueryColumn[] cols = PatrolQueryColumnCache.getInstance().getObservationQueryColumns();
			
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
}
