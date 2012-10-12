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
package org.wcs.smart.query.model;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.engine.DerbyGridEngine;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.filter.DateFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.parser.internal.summary.IValueItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * A class to represent a summary query.
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.gridded_query")
public class GriddedQuery extends Query {

	private GridQueryDefinition query;
	
	private String strQuery;	
	private ConservationAreaFilter caFilter;
	private DateFilter dateFilter;
	
	private List<QueryColumn> queryColumns = null;
	
	private String crsDefinition;
	@Transient
	private CoordinateReferenceSystem crs;
	
	/* transient fields for tracking ui items */
	@Transient
	private List<DropItem> valueDropItems;
	@Transient
	private List<DropItem> filterDropItems;
	
	@Transient
	private List<GridResultItem> lastResults;
	@Transient
	private GridQueryResultMetadata resultMetadata;

	/**
	 * Creates a new gridded query with the default
	 * conservation area filter and no date filter
	 */
	public GriddedQuery(){
		super();
		setName("<No Name Gridded Summary>");
		caFilter = new ConservationAreaFilter();
		if (SmartDB.getCurrentConservationArea() != null){
			caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		}
		
		dateFilter = null;
	}
	
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	public  GridQueryDefinition parseQuery() throws Exception {

		if (strQuery == null || strQuery.length() == 0){
			return null;
		}
		InputStream is = new ByteArrayInputStream(strQuery.getBytes());
		Parser parser = new Parser(is);
		GridQueryDefinition myQuery = parser.GridQuery();
		is.close();
		return myQuery;
	}
	
	/**
	 * @return the conservation area filter
	 */
	
	@Column(name="ca_filter")
	public String getConservationAreaFilterAsFilter(){
		return this.caFilter.asString();
	}
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilterAsFilter(String filter){
		this.caFilter = ConservationAreaFilter.parseFilter(filter);
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
	@Override
	public void setDateFilter(DateFilter dateFilter){
		this.dateFilter = dateFilter;
	}

	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public QueryType getType() {
		return QueryType.GRIDDED;
	}
	
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQuery(String query){
		this.strQuery = query;
	}
	
	/**
	 * Sets the query string.  At this point the
	 * filter is not parsed.
	 * 
	 * @param filter
	 * @return
	 */
	public void setQuery(String queryStr, GridQueryDefinition queryDef){
		this.strQuery = queryStr;
		this.query = queryDef;
	}
	
	/**
	 * 
	 * @return the coordinate reference system definition string
	 */
	@Column(name="crs_definition")
	public String getCrsDefinition(){
		return this.crsDefinition;
	}
	/**
	 * 
	 * @param defintion the coordinate reference system string
	 */
	public void setCrsDefinition(String defintion){
		this.crsDefinition = defintion;
		this.crs = null;
	}
	
	/**
	 * 
	 * @return the decoded coordinate reference system
	 * @throws Exception
	 */
	@Transient
	public CoordinateReferenceSystem getCoordinateReferenceSystem() throws Exception{
		if (this.crs == null && getCrsDefinition() != null){
			this.crs = CRS.parseWKT(getCrsDefinition());
		}
		return this.crs;
	}
	
	
	/**
	 * 
	 * @return the decoded coordinate reference system
	 * @throws Exception
	 */
	@Transient
	public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) throws Exception{
		this.crs = crs;
		setCrsDefinition(this.crs.toWKT());
	}
	
	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_def")
	public String getQuery(){
		return this.strQuery;
	}

	/**
	 * 
	 * @return Results from last query run
	 */
	@Transient
	public List<GridResultItem> getLastResults(){
		return this.lastResults;
	}
	
	/**
	 * Executes the query and returns the results
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	@Transient
	public List<GridResultItem> getQueryResults(IProgressMonitor monitor) throws Exception{
		lastResults = Collections.emptyList();
		resultMetadata = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			
			DerbyGridEngine engine = new DerbyGridEngine();
			lastResults = engine.executeQuery(this, session, monitor);
			resultMetadata = GridQueryResultMetadata.computeMetadata(lastResults);
			return lastResults;
		}finally{
			if (session.isOpen()){
				session.getTransaction().commit();
				session.close();
			}
		}
	}
	
	/**
	 * 
	 * @return the metadata about the last set of results computed
	 * or null if not yet computed
	 */
	@Transient
	public GridQueryResultMetadata getResultMetadata(){
		return this.resultMetadata;
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public GriddedQuery clone(){
		GriddedQuery q = new GriddedQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(caFilter);
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQuery(getQuery());
		return q;
	}
	
	

	/**
	 * @return value drop items
	 */
	@Transient
	public List<DropItem>getValueDropItems(){
		return this.valueDropItems;
	}
	
	/**
	 * @param items value drop items
	 */
	@Transient
	public void setValueDropItems(List<DropItem> items){
		this.valueDropItems = items;
	}
	
	/**
	 * @return filter drop items
	 */
	@Transient
	public List<DropItem>getFilterDropItems(){
		return this.filterDropItems;
	}
	
	/**
	 * @param items filter drop items
	 */
	@Transient
	public void setFilterDropItems(List<DropItem> items){
		this.filterDropItems = items;
	}
	
	/**
	 * Generates ui drop items for the given query
	 * @param session
	 * @throws Exception
	 */
	@Transient
	public void generateDropItems(Session session) throws Exception{

		clearDropItemList(valueDropItems);
		clearDropItemList(filterDropItems);
		if (valueDropItems == null){
			valueDropItems = new ArrayList<DropItem>();
		}
		if (filterDropItems == null){
			filterDropItems = new ArrayList<DropItem>();
		}

		
		//---- generate drop items for filter items ----		
		IFilter query = getQueryDefinition().getQueryFilter();
		if (query != null){
			DropItem[] filterItems = query.getDropItems(session);
			for (int i = 0; i < filterItems.length; i ++){
				filterDropItems.add(filterItems[i]);
			}
		}
		
		//---- generate drop items for value items ----
		List<DropItem> valueItems = new ArrayList<DropItem>();
		IValueItem part = getQueryDefinition().getValuePart();

		valueItems.add(part.asDropItem(session));
		valueDropItems.addAll(valueItems);
		
			
	}
	
	@Transient
	private void clearDropItemList(List<DropItem> list){
		if (list != null){
			for (DropItem it : list){
				it.dispose();
			}
			list.clear();
		}
	}
	
	/**
	 * Validates the query.  Assumes the query
	 * definition is formed from a valid string.
	 * <p>
	 * This validates the items in the query.

	 */
	public String validate(){
		//TODO: validation on the query string and/or definition
		//return the error if there is on, null if it's ok.
		if (getQueryDefinition() != null && getQueryDefinition().getGridSize() <= 0){
			return "Invalid Grid Size";
		}
		return null;
	}

	@Transient
	public List<QueryColumn> getQueryColumns() {
		if (this.queryColumns == null){
			initQueryColumns();
		}
		return this.queryColumns;
	}

	/**
	 * Loads the query columns
	 */
	private void initQueryColumns(){
		QueryColumn[] cols = QueryColumn.getGridColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		for (int i = 0; i < cols.length; i ++){
			queryColumns.add(cols[i]);
		}
	}
	
	/**
	 * Gets the grid size.
	 * 
	 * @return the grid size as an int
	 */
	@Transient
	public double getGridSize(){
		if (query == null){
			try{
				query = parseQuery();
				if (query == null){
					return 0.01;
				}
			}catch (Exception ex){
				//cannot parse query
				return 0.01;
			}
		}
		return query.getGridSize();
	}
	
	/**
	 * The grid origin - hard coded for now to 0,0
	 * of whatever crs the data is returned in.
	 * @return
	 */
	@Transient
	public Point getGridOrigin(){
		return new Point(0,0);
	}

	@Transient
	public GridQueryDefinition getQueryDefinition(){
		if (query == null){
			try{
				query = parseQuery();
			} catch (Exception ex) {
				QueryPlugIn.displayLog("Could not parse query.", ex);
			}
		}
		return query;
	}
	
	/**
	 * 
	 * @return the query filter in the filter format.  Will
	 * attempt to parse the query if it has not been parsed
	 */
	@Transient
	public IFilter getFilter(){
		IFilter filter = getQueryDefinition().getQueryFilter();
		if (filter == null){
			return IFilter.EMPTY_FILTER;
		}
		return filter;
	}

	@Override
	public boolean isDefinitionEqual(Query other) {
		if (! (other instanceof GriddedQuery)){
			return false;
		}
		GriddedQuery oquery = (GriddedQuery)other;
		if (oquery.getGridOrigin().equals(getGridOrigin()) && 
			oquery.getQuery().equals(getQuery())){
			return true;
		}
		return false;
	}


	@Override
	public void copyFrom(Query copy) {
		if (copy instanceof GriddedQuery){
			setQuery(((GriddedQuery) copy).getQuery());
		}
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(ConservationAreaFilter filter){
		this.caFilter = filter;
	}
	
	/**
	 * @return the conservation area filter
	 */
	@Transient
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	
	private File lastRasterFile = null;
	@Transient
	public File getLastRasterFile(){
		return this.lastRasterFile;
	}
	
	/**
	 * @return the raster file name for the gridded query
	 */
	@Transient
	public File getRasterFileName() {
		// ensure query dir exists
		File dir = QueryPlugIn.getDefault().getQueryTempDirectory();
		if (!dir.exists()) {
			dir.mkdir();
		}

		// create raster file name
		String fName = null;
		if (getUuid() == null) {
			fName = String.valueOf(System.nanoTime());
		} else {
			// ensure filename is unique for each raster service created
			fName = SmartUtils.encodeHex(getUuid()) + "_"
					+ String.valueOf(System.nanoTime());
		}

		StringBuilder pathBuilder = new StringBuilder(50);
		pathBuilder.append(dir.getAbsolutePath()).append(File.separator)
				.append(fName).append(".tiff");
		
		lastRasterFile = new File(pathBuilder.toString());
		return lastRasterFile;

	}

}
