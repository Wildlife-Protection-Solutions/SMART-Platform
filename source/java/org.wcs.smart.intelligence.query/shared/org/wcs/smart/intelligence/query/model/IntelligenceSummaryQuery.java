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
package org.wcs.smart.intelligence.query.model;

import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.intelligence.query.IIntelligenceQueryLabelProvider;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;

/**
 * An intelligence summary query has not options. 
 * It simply reports the number of records followed up on
 * or not followed up on.
 * '
 * @author Emily
 *
 */
@Entity
@Table(name="smart.intel_summary_query")
public class IntelligenceSummaryQuery extends Query {
	
	public static final String NUMBER_KEY = "intellcnt"; //$NON-NLS-1$
	public static final String NOT_FOLLOW_KEY = "notfollow"; //$NON-NLS-1$
	public static final String FOLLOW_KEY = "follow"; //$NON-NLS-1$
	
	public static final String KEY = "intelligencesummary"; //$NON-NLS-1$
	
	protected DateFilter dateFilter;
	
	
	public IntelligenceSummaryQuery(){
	}
	
	@Transient
	@Override
	public String getTypeKey() {
		return KEY;
	}

	@Transient
	@Override
	public boolean isDefinitionEqual(Query other) {
		if (other instanceof IntelligenceSummaryQuery) return true;
		return false;
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
	 * Nothing to copy
	 */
	@Override
	public void copyQuery(Query copy) {
		IntelligenceSummaryQuery q = (IntelligenceSummaryQuery)copy;
		setConservationAreaFilter(q.getConservationAreaFilter());
	}

	/**
	 * Cannot clone
	 */
	@Override
	public Query clone(Employee newOwner) {
		IntelligenceSummaryQuery q = new IntelligenceSummaryQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		return q;
	}
	
	/**
	 * Creates the template for the results.  These queries
	 * have on value (Number of Intelligence) grouped into either
	 * Followed Up or Not Followed Up.
	 * 
	 * @return
	 */
	public static SummaryQueryResult createResultTemplate(Locale l){
		SummaryQueryResult results = new SummaryQueryResult();
		
		IIntelligenceQueryLabelProvider lblProvider = SmartContext.INSTANCE.getClass(IIntelligenceQueryLabelProvider.class);
		
		results.addValueHeader(
				new SummaryHeader(
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.NUM_RECORD_SHORTNAME, l),
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.NUM_RECORD_LONGNAME, l),
						IntelligenceSummaryQuery.NUMBER_KEY, true));
		
		results.addRowHeader(
				new SummaryHeader[]{new SummaryHeader(
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.FOLLOWUP_LONGNAME, l),
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.FOLLOWUP_SHORTNAME, l),
						IntelligenceSummaryQuery.FOLLOW_KEY, false), 
				
				new SummaryHeader(
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.NOFOLLOWUP_LONGNAME, l), 
						lblProvider.getLabel(IIntelligenceQueryLabelProvider.NOFOLLOWUP_SHORTNAME, l),
						IntelligenceSummaryQuery.NOT_FOLLOW_KEY, false)}); 
	
		return results;
	}
}
