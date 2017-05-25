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
package org.wcs.smart.patrol.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.IQueryColumnProvider;

/**
 * A class to represent a summary query.
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@Table(name="smart.gridded_query")
public class PatrolGriddedQuery extends GriddedQuery {
	
	public static final String KEY = "patrolgrid"; //$NON-NLS-1$
	
	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected PatrolGridQueryDefinition parseQuery() throws Exception {

		if (strQuery == null || strQuery.length() == 0){
			return null;
		}
		try(InputStream is = new ByteArrayInputStream(strQuery.getBytes())){
			Parser parser = new Parser(is);
			PatrolGridQueryDefinition myQuery = parser.GridQuery();
			return myQuery;
		}
	}

	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	@Override
	public PatrolGriddedQuery clone(Employee newEmployee){
		PatrolGriddedQuery q = new PatrolGriddedQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newEmployee);
		q.setQuery(getQuery());
		q.setCrsDefinition(getCrsDefinition());
		q.setStyle(getStyle());
		return q;
	}
	
	@Override
	@Transient
	protected Class<? extends IQueryColumnProvider> getColumnProviderClass() {
		return IPatrolQueryColumnProvider.class;
	}
}
