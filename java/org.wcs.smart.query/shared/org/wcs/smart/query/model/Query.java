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

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.query.common.engine.IQueryResult;
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
	
	private String caFilter;

	private IQueryResult cachedResults;
	
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
		return this.caFilter;
	}
	/**
	 * @param filter a conservation area filter
	 */
	public void setConservationAreaFilter(String filter){
		this.caFilter = filter;		
	}

	/**
	 * @return the type of query
	 */
	@Transient
	public abstract String getTypeKey();
	
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
	public abstract Query clone(Employee newOwner);
	
	/**
	 * Sets the date filter associated with the current
	 * query.
	 * @param filter date filter
	 */
	public abstract void setDateFilter(DateFilter filter);
	
	
	@Transient
	public IQueryResult getCachedResults(){
		return this.cachedResults;
	}
	
	public void setCachedResults(IQueryResult results){
		this.cachedResults = results;
	}
	
}
