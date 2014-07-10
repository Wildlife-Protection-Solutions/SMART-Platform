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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;

/**
 * Parent query class that contains fields
 * shared across all queries.
 * 
 * @author Emily
 * @since 1.0.0
 */

@Entity
public abstract class Query extends NamedItem {
	
	
	private Employee owner = null;
	private ConservationArea conservationArea = null;
	private boolean isShared = false;
	private String id;
	private QueryFolder ownerFolder = null;
	private ConservationAreaFilter caFilter;
		
	//cached results from running the query
	private Object cachedResults;
	
	protected Query(){
		
	}
	
	
	/**
	 * A human-readable conservation area unique identifier for
	 * the query. 
	 * 
	 * @return
	 */
	public String getId(){
		return this.id;
	}
	/**
	 * Sets the query id
	 * @param id
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * @return query owner
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public Employee getOwner() {
		return owner;
	}
	/**
	 * @param owner the query owner
	 */
	public void setOwner(Employee owner) {
		this.owner = owner;
	}
	
	/**
	 * @return the conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * @param owner the query owner
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	/**
	 * @return <code>true</code> if conservation area level query; <code>false</code>
	 * if user only query
	 */
	@Column(name="shared")
	public boolean getIsShared(){
		return this.isShared;
	}
	/**
	 * Sets if the query is shared
	 * @param isShared
	 */
	public void setIsShared(boolean isShared){
		this.isShared = isShared;
	}
	
	/**
	 * @return the query folder associated with the query or <code>null</code>
	 * if associated with the root folder
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="folder_uuid", referencedColumnName="uuid")
	public QueryFolder getFolder(){
		return this.ownerFolder;
	}
	/**
	 * @param folder the query folder
	 */
	public void setFolder(QueryFolder folder){
		this.ownerFolder = folder;
	}
	
	/**
	 * @return the state of the query
	 */
	
	
	/**
	 * @return the conservation area filter
	 */
	@Column(name="ca_filter")
	public String getConservationAreaFilter(){
		if (this.caFilter == null) return null;
		return this.caFilter.asString();
	}
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(String filter){
		this.caFilter = ConservationAreaFilter.parseFilter(filter);
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
	public ConservationAreaFilter getConservationAreaFilterAsFilter(){
		return this.caFilter;
	}
	
	/**
	 * @return the type of query
	 */
	@Transient
	public abstract IQueryType getType();
	
	/**
	 * 
	 * Compares query definitions.
	 * 
	 * @param other
	 * @return <code>true</code> if the query definitions are the same.
	 */
	@Transient
	public abstract boolean isDefinitionEqual(Query other);
	
	/**
	 * 
	 * Copies the query definition from the
	 * provided query into the current query.
	 * 
	 * @param copy the query to copy from 
	 */
	@Transient
	public abstract void copyQuery(Query copy);

	/**
	 * Creates a copy of the current query.
	 * @return new query
	 */
	public abstract Query clone();
	
	/**
	 * Sets the date filter associated with the current
	 * query.
	 * @param filter date filter
	 */
	public abstract void setDateFilter(DateFilter filter);
	
	/**
	 * @param monitor progress monitor
	 * @param session current session; can be null if no active session
	 * 
	 * @return runs the query, caches the results and returns the results
	 */
	public Object executeQuery(IProgressMonitor monitor, Session session) throws Exception{
		cachedResults = executeQueryInternal(monitor, session);
		return cachedResults;
	}
	
	/**
	 * @param monitor progress monitor
	 * 
	 * @return runs the query, caches the results and returns the results
	 */
	public Object executeQuery(IProgressMonitor monitor) throws Exception{
		return executeQuery(monitor, null);
	}
	
	/**
	 * 
	 * @param monitor progress monitor
	 * @param session current session; can be null if no active session
	 * 
	 * @return runs the query and returns the results;
	 */
	protected abstract Object executeQueryInternal(IProgressMonitor monitor, Session session)  throws Exception;
	
	
	/**
	 * If there are results cached it returns
	 * the cached results otherwise it runs
	 * the query, caches the results
	 * and then returns them.
	 * 
	 * @session current session; can be null if no active session
	 * @return 
	 */
	public Object getCachedResults(IProgressMonitor monitor, Session session)  throws Exception{
		if (cachedResults == null){
			cachedResults = executeQuery(monitor, session);
		}
		return cachedResults;
	}
	
	/**
	 * If there are results cached it returns
	 * the cached results otherwise it runs
	 * the query, caches the results
	 * and then returns them.
	 * 
	 * @session current session; can be null if no active session
	 * @return 
	 */
	public Object getCachedResults(IProgressMonitor monitor)  throws Exception{
		return getCachedResults(monitor, null);
	}
	
	/**
	 * Clears the cached results.  This will force
	 * the query to be rerun when getCachedResults() is
	 * called.
	 */
	public void clearCachedResults(){
		this.cachedResults = null;
	}
}
