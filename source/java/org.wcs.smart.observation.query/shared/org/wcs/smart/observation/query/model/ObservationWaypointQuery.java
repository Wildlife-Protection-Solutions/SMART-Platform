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
package org.wcs.smart.observation.query.model;

import java.io.Reader;
import java.io.StringReader;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.observation.query.model.columns.IObservationQueryColumnProvider;
import org.wcs.smart.observation.query.parser.internal.parser.Parser;
import org.wcs.smart.query.common.model.IQueryColumnProvider;
import org.wcs.smart.query.common.model.WaypointQuery;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.QueryFilter;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * A class to represent an waypoint query.
 * <p>Waypoint queries query all observations at a
 * given waypoint.</p>  Also known as incident queries.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="obs_waypoint_query", schema="smart")
public class ObservationWaypointQuery extends WaypointQuery {

	private static final long serialVersionUID = 1L;
	
	public static final String KEY = "observationwaypoint"; //$NON-NLS-1$
	public static final String DEFAULT_STYLE_KEY = "org.wcs.smart.observation.query.map.waypoint"; //$NON-NLS-1$


	/**
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	@Override
	public ObservationWaypointQuery clone(Employee newOwner){
		ObservationWaypointQuery q = new ObservationWaypointQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		q.setQueryFilter(getQueryFilter());
		q.setVisibleColumns(getVisibleColumns());
		q.setStyle(getStyle());
		return q;
	}
	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}

	@Override
	protected QueryFilter parseQueryFilter() throws Exception {
		if (strQueryFilter == null || strQueryFilter.length() == 0){
			return new QueryFilter(EmptyFilter.INSTANCE);
		}
		if(queryFilter != null){
			return queryFilter;
		}
		try(Reader is = new StringReader(strQueryFilter)){
			Parser parser = new Parser(is);
			QueryFilter myQuery = parser.QueryFilter();
			queryFilter = myQuery;
			return myQuery;
		}
	}
	@Override
	@Transient
	protected Class<? extends IQueryColumnProvider> getColumnProviderClass() {
		return IObservationQueryColumnProvider.class;
	}
}