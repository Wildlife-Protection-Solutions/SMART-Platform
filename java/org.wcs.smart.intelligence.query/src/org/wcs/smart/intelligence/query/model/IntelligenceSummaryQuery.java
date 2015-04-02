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
package org.wcs.smart.intelligence.query.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.query.engine.SummaryIntelligenceQueryEngine;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;

/**
 * An intelligence summary query has not options. 
 * It simply reports the number of records followed up on
 * or not followed up on.
 * '
 * @author Emily
 *
 */
public class IntelligenceSummaryQuery extends Query {
	
	private DateFilter dateFilter;
	
	
	public IntelligenceSummaryQuery(){
		setConservationAreaFilter(new ConservationAreaFilter(true));
	}
	
	@Override
	public IQueryType getType() {
		return QueryTypeManager.getInstance().findQueryType(IntelligenceSummaryQueryType.KEY);
	}

	@Override
	public boolean isDefinitionEqual(Query other) {
		if (other instanceof IntelligenceSummaryQuery) return true;
		return false;
	}

	/**
	 * Nothing to copy
	 */
	@Override
	public void copyQuery(Query copy) {
	}

	/**
	 * Cannot clone
	 */
	@Override
	public Query clone() {
		return null;
	}

	@Override
	public void setDateFilter(DateFilter filter) {
		this.dateFilter = filter;
	}
	
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}

	
	@Override
	protected Object executeQueryInternal(IProgressMonitor monitor,
			Session session) throws Exception {
		Session lsession = session;
		if (session == null){
			lsession = HibernateManager.openSession();
			lsession.beginTransaction();
		}
		try{
			SummaryIntelligenceQueryEngine engine = new SummaryIntelligenceQueryEngine();
			return engine.executeQuery(this, lsession, monitor);
		}finally{
			if (session == null && lsession.isOpen()){
				lsession.getTransaction().commit();
				lsession.close();
			}
		}
	}

}
