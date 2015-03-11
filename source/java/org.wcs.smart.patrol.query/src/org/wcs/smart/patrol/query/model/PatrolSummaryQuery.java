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

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.engine.DerbySummaryEngine;
import org.wcs.smart.patrol.query.model.types.PatrolSummaryQueryType;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.summary_query")
public class PatrolSummaryQuery extends SummaryQuery {

	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected SumQueryDefinition parseQuery() throws Exception {
		
		if (getQuery() == null || getQuery().length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(getQuery().getBytes());
		Parser parser = new Parser(is);
		SumQueryDefinition myQuery = parser.SumQuery();
		is.close();
		return myQuery;
	}


	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(PatrolSummaryQueryType.KEY);
	}
	
	@Transient
	public SummaryQueryResult executeQueryInternal(IProgressMonitor monitor, Session session) throws Exception{
		Session lsession = session;
		if (session == null){
			lsession = HibernateManager.openSession();
			lsession.beginTransaction();
		}
		try{
			DerbySummaryEngine engine = new DerbySummaryEngine();
			SummaryQueryResult lastResults = engine.executeQuery(this, lsession, monitor);
			return lastResults;
		}finally{
			if (session == null && lsession.isOpen()){
				lsession.getTransaction().commit();
				lsession.close();
			}
		}
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public PatrolSummaryQuery clone(){
		PatrolSummaryQuery q = PatrolQueryFactory.createSummaryQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQuery(getQuery());
		return q;
	}
}
