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
package org.wcs.smart.query.model.observation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.engine.DerbyQueryEngine2;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.SimpleQuery;

/**
 * A class to represent an observation query.
 * <p>Observation queries query each observation
 * which consists of a category and a 
 * set of attributes.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.waypoint_query")
public class ObservationQuery extends SimpleQuery{
	
	private String visibleTableColumnKeys = null;
	private List<QueryColumn> queryColumns = null;
	
	
	/**
	 * Creates a new observation query with the default
	 * conservation area filter and no date filter
	 */
	public ObservationQuery(){
		super();
	}
	
	
	/**
	 * Returns a list of columns that are visible in the output table.
	 * @return a list of visible column
	 */
	@Column(name = "column_filter")
	public String getVisibleColumns(){
		return this.visibleTableColumnKeys;
	}
	
	/**
	 * Sets the columns that are visible in the output table.
	 * 
	 * @param columns
	 */
	public void setVisibleColumns(String columns){
		this.visibleTableColumnKeys = columns;
	}
	
	
	/**
	 * Updates the visible columns based 
	 * on the isVisible field of the associated
	 * WaypointQueryColumn columns.
	 */
	@Transient
	public void updateVisibleColumns(){
		StringBuilder sb = new StringBuilder();
		boolean all = true;
		for (QueryColumn col : queryColumns){
			if (col.isVisible() ){
				sb.append(col.getKey());
				sb.append(",");
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
		QueryColumn[] cols = QueryColumn.getWaypointQueryColumns();
		
		queryColumns = new ArrayList<QueryColumn>();
		HashSet<String> visible = null;
		if (visibleTableColumnKeys != null){
			String[] bits = visibleTableColumnKeys.split(",");
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
		DerbyQueryEngine2 engine = new DerbyQueryEngine2();
		return engine.executeQuery(this, session, progressMonitor);
	}
	
	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public ObservationQuery clone(){
		ObservationQuery q = new ObservationQuery();
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
		return QueryType.OBSERVATION;
	}
}
