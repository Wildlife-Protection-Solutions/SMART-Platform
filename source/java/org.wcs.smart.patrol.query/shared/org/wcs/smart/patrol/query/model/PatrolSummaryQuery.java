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

import java.io.Reader;
import java.io.StringReader;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.summary.CombinedValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.summary_query")
public class PatrolSummaryQuery extends SummaryQuery {

	private static final long serialVersionUID = 1L;
	
	public static final String KEY = "patrolsummary"; //$NON-NLS-1$
	
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected SumQueryDefinition parseQuery() throws Exception {
		
		if (getQuery() == null || getQuery().length() == 0){
			return null;
		}
		try(Reader is = new StringReader(getQuery())){
			Parser parser = new Parser(is);
			SumQueryDefinition myQuery = parser.SumQuery();
			
			//update value options for no data option
			for (IValueItem item : myQuery.getValuePart().getValueItems()) {
				if (!(item instanceof CombinedValueItem)) continue;
				
				CombinedValueItem cItem = (CombinedValueItem) item;
				IValueItem part1 = cItem.getPart1();
				IValueItem part2 = cItem.getPart2();
				
				if (part1 instanceof PatrolValueItem && part2 instanceof PatrolValueItem) {
					PatrolValueItem p1 = (PatrolValueItem)part1;
					PatrolValueItem p2 = (PatrolValueItem)part2;
					if (p1.getPatrolValueOption().hasNoDataOption() && p2.getPatrolValueOption().hasNoDataOption()) {
						p2.setIncludeNoData(p1.includeNoData());
					}
				}				
			}
			
			return myQuery;
		}
	}


	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Transient
	public String getTypeKey() {
		return KEY;
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public PatrolSummaryQuery clone(Employee newOwner){
		PatrolSummaryQuery q = new PatrolSummaryQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		q.setQuery(getQuery());
		return q;
	}
}
