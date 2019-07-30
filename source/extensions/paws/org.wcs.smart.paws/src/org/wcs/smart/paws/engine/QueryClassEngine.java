package org.wcs.smart.paws.engine;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.paws.model.PawsClassification;
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

public class QueryClassEngine {

	private PawsClassification pc;
	private LocalDate startDate;
	private LocalDate endDate;
	
	private IQueryResult results;
	
	private String temptable;
	private String obcol;
	
	public QueryClassEngine(PawsClassification pc, LocalDate startDate, LocalDate endDate) {
		this.pc = pc;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	private void throwException(Query query) throws Exception{
		throw new Exception(MessageFormat.format("Query not supported {0}", query.getName())); 
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

		ff.setDates(java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
		query.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, ff));
		
		results = QueryExecutor.INSTANCE.executeQuery(query, session, new NullProgressMonitor());
		if (!(results instanceof IObservationPagedQueryResultSet)) {
			throwException(query);
		}
		System.out.println( ((IObservationPagedQueryResultSet)results).getItemCount() );
		System.out.println( ((IObservationPagedQueryResultSet)results).getWpCount() );
		
		temptable = ((IObservationPagedQueryResultSet)results).getResultsTable();
		obcol = ((IObservationPagedQueryResultSet)results).getObservationColumn();
	}
}
