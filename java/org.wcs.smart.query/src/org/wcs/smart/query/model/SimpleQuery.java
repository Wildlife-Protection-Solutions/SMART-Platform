package org.wcs.smart.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyQueryEngine2;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.parser.internal.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.formulaDnd.DropItem;

public abstract class SimpleQuery extends Query {
	@Transient
	private List<DropItem> items;
	
	private String strQueryFilter;
	private IFilter queryFilter;	//cached copy of the parsed query
	
	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
		
	private List<QueryResultItem> lastResults  = null;
	
	
	/**
	 * Creates a new waypoint query with the default
	 * conservation area filter and no date filter
	 */
	public SimpleQuery(){
		super();
		setName("<No Name Query>");
		caFilter = new ConservationAreaFilter();
		if (SmartDB.getCurrentConservationArea() != null){
			caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		dateFilter = null;
	strQueryFilter = "";
	}
	
	/**
	 * Creates a new waypoint query the given filter and
	 * the default conservation area filter and no date filter.
	 * 
	 * @param pFilter query filter
	 */
	public SimpleQuery(IFilter pFilter){
		this();
		
		this.queryFilter = pFilter;
		this.strQueryFilter = pFilter.asString();
	}
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQueryFilter(String filter){
		this.strQueryFilter = filter;
		this.queryFilter = null;
	}
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_filter")
	public String getQueryFilter(){
		return this.strQueryFilter;
	}
	
	@Transient
	abstract public List<QueryColumn> getQueryColumns();


	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	@Transient
	public IFilter getFilter(){
		if (queryFilter == null){
			try{
				queryFilter = parseQueryFilter();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		return queryFilter;	
	}
	
	
	/**
	 * @return the conservation area filter
	 */
	@Transient
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
	}
	
	
	/**
	 * @return the date filter; or null if date filter not set
	 */
	@Transient
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	/**
	 * Sets the date filter 
	 * @param dateFilter
	 */
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
	}

	
	/**
	 * Runs the query and returns the results.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Transient
	public List<QueryResultItem> getQueryResults(IProgressMonitor progressMonitor) throws Exception{
		
		lastResults = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			lastResults = getQueryResults(session, progressMonitor);
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
		return lastResults;
	}
	
	/**
	 * Returns the results from last time the query was run.  Does not re-run the query.
	 * @return the last run results
	 */
	@Transient
	public List<QueryResultItem> getLastResults(){
		return lastResults;
	}
	
	/** public for testing purposes only */
	@Transient
	public abstract List<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception;

	
	/**
	 * Generates all drop items for the query.
	 * @param session hibernate session
	 * @throws Exception
	 */
	@Transient
	public void generateDropItems(Session session) throws Exception{
		//parses the query into a collection of drop items
		IFilter query = parseQueryFilter();
		if (items != null){
			for (DropItem it : items){
				it.dispose();
			}
			items.clear();
		}else{
			items = new ArrayList<DropItem>();
		}
		DropItem[] filterItems = query.getDropItems(session);
		for (int i = 0; i < filterItems.length; i ++){
			items.add(filterItems[i]);
		}
	}
	
	/**
	 * @return the drop items generated for the query
	 */
	@Transient
	public List<DropItem> getDropItems(){
		return items;
	}
	/**
	 * @param items the drop items associated with the query
	 */
	@Transient
	public void setDropItems(List<DropItem> items){
		this.items = items;
	}
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	private  IFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return IFilter.EMPTY_FILTER;
		}
		InputStream is = new ByteArrayInputStream(strQueryFilter.getBytes());
		Parser parser = new Parser(is);
		IFilter myQuery = parser.QueryFilter();
		is.close();
		return myQuery;
	}
	
}
