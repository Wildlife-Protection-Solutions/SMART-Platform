package org.wcs.smart.i2.query;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;

public class PagedResultSetIterator {

	private IPagedQueryResultSet results;
	
	private Session session;
	private ScrollableResults resultSet;
	
	
	public PagedResultSetIterator(IPagedQueryResultSet results, Session session){
		this.results = results;
		this.session = session;
		resultSet = session.createSQLQuery("SELECT * FROM " + results.getQueryDataTable()).scroll();
	}
	
	public boolean hasNext(){
		return resultSet.next();
	}
	
	public IResultItem next(){
		return results.asResultItem(resultSet, session);
	}
	
}
