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
package org.wcs.smart.report.query.data.oda.query;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.ISmartQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.impl.SmartQuery;
import org.wcs.smart.data.oda.smart.query.common.EmptyResultSet;
import org.wcs.smart.data.oda.smart.query.common.MemoryQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.PagedQueryResultSet;
import org.wcs.smart.data.oda.smart.query.common.SimpleQueryResultSetMetadata;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSet;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.report.query.internal.Messages;

/**
 * Implementation class of IQuery for the SMART ODA runtime driver. <br>
 * This wraps around any smart query (ncluding summaries, patrol, waypoint
 * queries).
 */
public class PatrolSmartQuery implements ISmartQuery {


	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p>Here the queryText contains the hex encoded uuid
	 * of the query.  The query is loaded from the database and
	 * parsed to ensure it is valid.
	 * </p>
	 */
	@Override
	public void prepare(SmartQuery query) throws OdaException {
		try{
			// attempt to parse query
			if (query.getQuery() instanceof SimpleQuery) {
				((SimpleQuery) query.getQuery()).getFilter();
			} else if (query.getQuery() instanceof PatrolSummaryQuery) {
				PatrolSummaryQuery sumQuery = (PatrolSummaryQuery)query.getQuery();
				
				//date group by problem with reports 
				GroupByPart part = sumQuery.getQueryDefinition().getColumnGroupByPart();
				List<IGroupBy> headers = part.getGroupBys();
				for (IGroupBy h : headers){
					if (h instanceof DateGroupBy){
						throw new OdaException(Messages.SmartQuery_Warning_SummaryGroupByDates);
					}
				}
			} else if (query.getQuery() instanceof PatrolGriddedQuery){
				((PatrolGriddedQuery)query.getQuery()).getQueryDefinition();
			}
			}catch (Exception ex){
			throw new OdaException(ex);
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	@Override
	public IResultSetMetaData getMetaData(SmartQuery query) throws OdaException {
		if (query.getQuery().getTypeKey().equals(PatrolObservationQuery.KEY) ||
				query.getQuery().getTypeKey().equals(PatrolWaypointQuery.KEY) ||
				query.getQuery().getTypeKey().equals(PatrolQuery.KEY)){
			return new SimpleQueryResultSetMetadata((SimpleQuery) query.getQuery());
		} else if (query.getQuery().getTypeKey().equals(PatrolSummaryQuery.KEY)) {
			return new PatrolSummaryQueryResultSetMetadata((PatrolSummaryQuery) query.getQuery());
		} else if (query.getQuery().getTypeKey().equals(PatrolGriddedQuery.KEY)){
			return new SimpleQueryResultSetMetadata( (PatrolGriddedQuery) query.getQuery());
		}
		throw new OdaException(Messages.SmartQuery_Error_CouldNoLoadMetadata);
	}

	@Override
	public IResultSet executeQuery(SmartQuery query, SmartConnection connection) throws OdaException {
		IResultSet resultSet = null;

		//create date filter
		Date startDate = (Date) query.getParameters().get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) query.getParameters().get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			if (query.getQuery().getTypeKey().equals(PatrolSummaryQuery.KEY)){
				//we choose to run summaries in order to get 
				//all header information
				Calendar cal = Calendar.getInstance();
				cal.set(1900, Calendar.JANUARY, 1);
				startDate = new Date( cal.getTimeInMillis() );
				endDate = new Date(startDate.getTime());
			}else{
				//all others will just return an empty
				return EmptyResultSet.INSTANCE;
			}
			
		}
		
		CustomDateFilter cd = new CustomDateFilter();
		cd.setDates(startDate, endDate);
		DateFilter dateFilter = new DateFilter(
				WaypointDateField.INSTANCE,cd);


		//the result set
		
		if (query.getQuery().getTypeKey().equals(PatrolObservationQuery.KEY) ||
				query.getQuery().getTypeKey().equals(PatrolWaypointQuery.KEY)){
			((SimpleQuery) query.getQuery()).setDateFilter(dateFilter);
			resultSet = new PagedQueryResultSet(query.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(query), 
					connection);
		}else if (query.getQuery().getTypeKey().equals(PatrolQuery.KEY)){
			((SimpleQuery) query.getQuery()).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet(query.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(query),
					connection);
		}else  if (query.getQuery().getTypeKey().equals(PatrolGriddedQuery.KEY)){
			((PatrolGriddedQuery) query.getQuery()).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet(query.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(query),
					connection);
		} else if (query.getQuery().getTypeKey().equals(PatrolSummaryQuery.KEY)){
			((PatrolSummaryQuery) query.getQuery()).setDateFilter(dateFilter);
			resultSet = new SummaryQueryResultSet(
					(PatrolSummaryQuery) query.getQuery(),
					new PatrolSummaryQueryResultSetMetadata((PatrolSummaryQuery) query.getQuery()),
					connection);
		}
		
		return resultSet;
	}

}
