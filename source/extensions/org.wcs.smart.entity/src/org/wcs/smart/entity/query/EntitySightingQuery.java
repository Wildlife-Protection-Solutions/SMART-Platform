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
package org.wcs.smart.entity.query;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
/**
 * Entity type sighting query.
 * 
 * @author Emily
 *
 */
public class EntitySightingQuery {

	private DateFilter dateFilter;
	private EntityType entityType;
	private EntityFilter entityFilter;
	private ConservationAreaFilter caFilter;
	
	private SightingPagedResults cachedResults;
	private List<QueryColumn> columns;
	
	/**
	 * Creates a new entity query, creating a 
	 * deafult conservation area filter
	 * 
	 * @param entityType entity type
	 * @param dateFilter date filter
	 * @param entityFilter entity filter
	 */
	public EntitySightingQuery(EntityType entityType, 
			DateFilter dateFilter, 
			EntityFilter entityFilter){
		
		this.entityType = entityType;
		this.dateFilter = dateFilter;
		this.entityFilter = entityFilter;
		
		this.caFilter = new ConservationAreaFilter(true);
	}
	
	/**
	 * 
	 * @return date filter
	 */
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	
	/**
	 * 
	 * @return conservation area
	 */
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	
	/**
	 * 
	 * @return the entity filter
	 */
	public EntityFilter getEntityFilter(){
		return this.entityFilter;
	}
	
	/**
	 * 
	 * @return the entity type
	 */
	public EntityType getEntityType(){
		return this.entityType;
	}
	
	/**
	 * 
	 * @return the query columns
	 */
	public List<QueryColumn> getQueryColumns(){
		return this.columns;
	}
	
	/**
	 * Sets the query columns
	 * @param cols
	 */
	public void setQueryColumns(List<QueryColumn> cols){
		this.columns = cols;
	}
	
	
	/**
	 * 
	 * @return runs the query, caches the results and returns the results
	 */
	public Object executeQuery(IProgressMonitor monitor) throws Exception{
		cachedResults = (SightingPagedResults) executeQueryInternal(monitor);
		return cachedResults;
	}
	
	/**
	 * 
	 * @return runs the query and returns the results;
	 */
	private Object executeQueryInternal(IProgressMonitor monitor)  throws Exception{
		DerbyEntitySightingEngine queryEngine = new DerbyEntitySightingEngine();
		Session session = HibernateManager.openSession();
		try{
			return queryEngine.executeDerbyQuery(this, session, monitor);
		}finally{
			session.close();
		}
		
	}
	
	
	/**
	 * If there are results cached it returns
	 * the cached results otherwise it runs
	 * the query, caches the results
	 * and then returns them.
	 * @return 
	 */
	public Object getCachedResults(IProgressMonitor monitor)  throws Exception{
		if (cachedResults == null){
			cachedResults = (SightingPagedResults) executeQuery(monitor);
		}
		return cachedResults;
	}
}
