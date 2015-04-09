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
package org.wcs.smart.intelligence.query.report.data;

import java.sql.Date;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.ISmartQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.data.oda.smart.query.common.EmptyResultSet;
import org.wcs.smart.data.oda.smart.query.common.PagedQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.SimpleQueryResultSetMetadata;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.intelligence.query.RecievedDateFilter;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQueryType;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQueryType;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;

/**
 * Wrapper for loading intelligence queries in report.
 * 
 * @author Emily
 *
 */
public class IntelligenceReportQuery implements ISmartQuery {

	public IntelligenceReportQuery() {
	}

	/**
	 * Here was load the query from the database
	 * and parse it to ensure it is valid.
	 */
	@Override
	public void prepare(SmartQuery smartQuery) throws OdaException {

		// attempt to parse query
		if (smartQuery.getQuery() instanceof SimpleQuery) {
			((SimpleQuery) smartQuery.getQuery()).getFilter();
		} else if (smartQuery.getQuery() instanceof IntelligenceSummaryQuery) {
			
		}
	}

	@Override
	public IResultSet executeQuery(SmartQuery smartQuery, SmartConnection connection) throws OdaException {
		IResultSet resultSet = null;

		//create date filter
		Date startDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			//all others will just return an empty
			return EmptyResultSet.INSTANCE;
		}
		
		CustomDateFilter cd = new CustomDateFilter();
		cd.setDates(startDate, endDate);
		DateFilter dateFilter = new DateFilter(
				RecievedDateFilter.INSTANCE,cd);

		smartQuery.getQuery().setDateFilter(dateFilter);
		
		//the result set
		if (smartQuery.getQuery().getType().getKey().equals(IntelligenceRecordQueryType.KEY)){
			resultSet = new PagedQueryResultSet(smartQuery.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(smartQuery),
					connection);
		} else if (smartQuery.getQuery() instanceof IntelligenceSummaryQuery) {
			
			IntelligenceSummaryQuery sumQuery = (IntelligenceSummaryQuery)smartQuery.getQuery();
			resultSet = new SummaryQueryResultSet(sumQuery,
					(SummaryQueryResultSetMetadata)getMetaData(smartQuery), connection);
		}
		return resultSet;
	}

	@Override
	public IResultSetMetaData getMetaData(SmartQuery smartQuery)
			throws OdaException {
		if (smartQuery.getQuery().getType().getKey().equals(IntelligenceRecordQueryType.KEY)){
			return new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery.getQuery());
		}else if (smartQuery.getQuery().getType().getKey().equals(IntelligenceSummaryQueryType.KEY)){
			return new IntelSummaryQueryResultSetMetadata((IntelligenceSummaryQuery)smartQuery.getQuery());
		}
		throw new OdaException("Unsupported query type."); //$NON-NLS-1$
	}

	
	private class IntelSummaryQueryResultSetMetadata extends SummaryQueryResultSetMetadata{

		public IntelSummaryQueryResultSetMetadata(IntelligenceSummaryQuery query) {
			super();
			results = IntelligenceSummaryQueryType.createResultTemplate();
		}

		protected void parseHeader(SummaryQuery query) {
			results = IntelligenceSummaryQueryType.createResultTemplate();
		}
		
	}
}
