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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;


/**
 * Entity type sighting query.
 * <p>
 * EntitySightingQuery objects extend Query objects but are 
 * not hibernate objects and are not persisted to the database.
 * This was done to be able to re-use some of the query
 * export logic.
 * </p>
 * 
 * @author Emily
 *
 */
public class EntitySightingQuery extends Query{

	private DateFilter dateFilter;
	private EntityType entityType;
	private EntityFilter entityFilter;
	
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
		setName(MessageFormat.format(Messages.EntitySightingQuery_QueryName, new Object[]{entityType.getName()}));
		
		setConservationAreaFilter(new ConservationAreaFilter(true));
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
	 * @param monitor
	 * @param session can be null
	 * 
	 * @return runs the query and returns the results;
	 */
	protected Object executeQueryInternal(IProgressMonitor monitor, Session session)  throws Exception{
		DerbyEntitySightingEngine queryEngine = new DerbyEntitySightingEngine();
		Session lSession = session;
		if (lSession == null){
			lSession = HibernateManager.openSession();
		}
		try{
			return queryEngine.executeDerbyQuery(this, lSession, monitor);
		}finally{
			if (session == null){
				lSession.close();
			}
		}
		
	}

	@Override
	public IQueryType getType() {
		return EntitySightingQueryType.INSTANCE;
	}

	@Override
	public boolean isDefinitionEqual(Query other) {
		if (!(other instanceof EntitySightingQuery)){
			return false;
		}
		return entityFilter.asString().equals(((EntitySightingQuery)other).getEntityFilter().asString());
	}

	@Override
	public void copyQuery(Query copy) {
		EntitySightingQuery newquery = (EntitySightingQuery) copy;
		newquery.setDateFilter(getDateFilter());
		newquery.setConservationAreaFilter(getConservationAreaFilter());
		newquery.setQueryColumns(getQueryColumns());
		newquery.entityFilter = getEntityFilter();
	}

	/**
	 * Not supported.
	 * @return null
	 */
	@Override
	public Query clone() {
		throw new RuntimeException("Cannot clone entity sighting queries"); //$NON-NLS-1$
	}

	@Override
	public void setDateFilter(DateFilter filter) {
		this.dateFilter = filter;
	}
}
