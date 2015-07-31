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
package org.wcs.smart.query.common.model;

import java.awt.Point;
import java.io.File;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartContext;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.util.UuidUtils;

/**
 * A class to represent a summary query.
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class GriddedQuery extends StyledQuery {
	
	protected String strQuery;			//query string stored in db
	protected String crsDefinition;	//query crs definition stored in db
	
	@Transient
	protected GridQueryDefinition query;	//parsed query string
	@Transient
	protected DateFilter dateFilter;		//temp date filter
	@Transient
	protected List<QueryColumn> queryColumns = null;	//parsed query columns
	@Transient
	protected CoordinateReferenceSystem crs;	//parsed crs definition
	

	/**
	 * Creates a new gridded query with the default
	 * conservation area filter and no date filter
	 */
	protected GriddedQuery(){
		super();
	}

	/**
	 * @return the query filter as string
	 */
	@Column(name = "query_def")
	public String getQuery(){
		return this.strQuery;
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
		this.query = null;
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
	 * 
	 * @return the columns associated with the query
	 */
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
	protected abstract void initQueryColumns();
	
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

	/**
	 * 
	 * @return the prased query definition
	 */
	@Transient
	public GridQueryDefinition getQueryDefinition() throws Exception{
		if (query == null){
			query = parseQuery();
		}
		return query;
	}
	

	/**
	 * @see org.wcs.smart.query.model.Query#isDefinitionEqual(org.wcs.smart.query.model.Query)
	 */
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


	/**
	 * @see org.wcs.smart.query.model.Query#copyQuery(org.wcs.smart.query.model.Query)
	 */
	@Override
	public void copyQuery(Query copy) {
		if (copy instanceof GriddedQuery){
			setQuery(((GriddedQuery) copy).getQuery());
			setCrsDefinition(((GriddedQuery) copy).getCrsDefinition());
			setConservationAreaFilter(copy.getConservationAreaFilter());
		}
	}

	protected abstract GridQueryDefinition parseQuery() throws Exception ;
	
	/**
	 * @return the raster file name for the gridded query
	 */
	@Transient
	public File getRasterFileName() {
		// ensure query dir exists
		File dir = SmartContext.INSTANCE.getTempFilestoreLocation();
		if (!dir.exists()) {
			dir.mkdir();
		}

		// create raster file name
		String fName = null;
		if (getUuid() == null) {
			fName = String.valueOf(System.nanoTime());
		} else {
			// ensure filename is unique for each raster service created
			fName = UuidUtils.uuidToString(getUuid()) + "_" //$NON-NLS-1$
					+ String.valueOf(System.nanoTime());
		}

		StringBuilder pathBuilder = new StringBuilder(50);
		pathBuilder.append(dir.getAbsolutePath()).append(File.separator)
				.append(fName).append(".tiff"); //$NON-NLS-1$

		File lastRasterFile = new File(pathBuilder.toString());
		return lastRasterFile;

	}
}
