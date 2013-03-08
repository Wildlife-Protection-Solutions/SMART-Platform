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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.engine.DerbyPatrolEngine;
import org.wcs.smart.query.model.observation.QueryColumn;
import org.wcs.smart.query.model.observation.QueryColumnCache;

/**
 * A representation of a patrol query.
 * @author egouge
 * @since 1.0.0
 */
@Entity
@Table(name="smart.patrol_query")
public class PatrolQuery extends SimpleQuery {

	private List<QueryColumn> queryColumns = null;

	
	/**
	 * Creates a new patrol query with the default
	 * conservation area filter and no date filter
	 */
	protected PatrolQuery(){
		super();
	}
	
	/**
	 * Updates the visible columns based 
	 * on the isVisible field of the associated
	 * QueryColumn columns.
	 */
	@Transient
	@Override
	public void updateVisibleColumns(){
		StringBuilder sb = new StringBuilder();
		boolean all = true;
		for (QueryColumn col : queryColumns){
			if (col.isVisible() ){
				sb.append(col.getKey());
				sb.append(","); //$NON-NLS-1$
			}else{
				all = false;
			}
		}
		if (!all){
			if (sb.length() > 0){
				sb.deleteCharAt(sb.length() - 1);
			}
			setVisibleColumns(sb.toString());
		}	
	}
	
	/**
	 * May call the database, so if performance important
	 * need to call inside job
	 * @return list of output columns available to the query.
	 */
	@Transient
	public List<QueryColumn> getQueryColumns(){
		if (this.queryColumns == null){
			initQueryColumns();
		}
		return this.queryColumns;
	}
	
	/**
	 * Loads the query columns
	 */
	private void initQueryColumns(){
		QueryColumn[] cols = QueryColumnCache.getInstance().getPatrolQueryColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		HashSet<String> visible = null;
		if (visibleTableColumnKeys != null){
			String[] bits = visibleTableColumnKeys.split(","); //$NON-NLS-1$
			visible = new HashSet<String>();
			for (int i = 0; i < bits.length; i ++){
				visible.add(bits[i]);
			}
		}
		for (int i = 0; i < cols.length; i ++){
			queryColumns.add(cols[i]);
			if (visible == null){
				cols[i].setVisible(true);
			}else if (visible.contains(cols[i].getKey())){
				cols[i].setVisible(true);
			}else{
				cols[i].setVisible(false);
			}
		}
	}
	
	/** public for testing purposes only */
	@Transient
	public List<QueryResultItem> getQueryResults(Session session, IProgressMonitor progressMonitor) throws Exception{
		DerbyPatrolEngine engine = new DerbyPatrolEngine();
		return engine.executeQuery(this, session, progressMonitor);
	}
	
	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public PatrolQuery clone(){
		PatrolQuery q = new PatrolQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		return q;
	}



	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public QueryType getType() {
		return QueryType.PATROL;
	}
}

