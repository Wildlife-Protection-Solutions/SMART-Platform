/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.engine;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Query engine for query data filters
 * 
 * @author Emily
 *
 */
public class QueryClassEngine {

	private PawsQueryClass pc;
	private LocalDate startDate;
	private LocalDate endDate;
	
	private IQueryResult results;
	
	private String temptable;
	private String obcol;
	
	public QueryClassEngine(PawsQueryClass pc, LocalDate startDate, LocalDate endDate) {
		this.pc = pc;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	private void throwException(Query query) throws Exception{
		throw new Exception(MessageFormat.format(Messages.QueryClassEngine_QueryNotSupported, query.getName())); 
	}
	
	public String getTable() { return this.temptable; }
	public String getObsColumn() { return this.obcol; }
	
	public void dispose(Session session) throws SQLException {
		results.dispose(session);
	}
	
	public void process(Session session) throws Exception {
		
		Query query = QueryHibernateManager.getInstance().findQuery(session, pc.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType( pc.getQueryType()) );
		if (!(query instanceof ObservationQuery)) {
			throwException(query);
		}
		
		CustomDateFilter ff = new CustomDateFilter();
		ff.setDates(startDate, endDate);
		query.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, ff));
		
		results = QueryExecutor.INSTANCE.executeQuery(query, session, new NullProgressMonitor());
		if (!(results instanceof IObservationPagedQueryResultSet)) {
			throwException(query);
		}

		temptable = ((IObservationPagedQueryResultSet)results).getResultsTable();
		obcol = ((IObservationPagedQueryResultSet)results).getObservationColumn();
	}
}
