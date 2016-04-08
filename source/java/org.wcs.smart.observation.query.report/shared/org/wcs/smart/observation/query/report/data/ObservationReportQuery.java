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
package org.wcs.smart.observation.query.report.data;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.SmartContext;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartQuery;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.query.common.EmptyResultSet;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Observation queries report extension
 * 
 * @author Emily
 *
 */
public class ObservationReportQuery extends AbstractSmartQuery {

	public ObservationReportQuery() {
	}

	/**
	 * Here was load the query from the database
	 * and parse it to ensure it is valid.
	 */
	@Override
	public void prepare(AbstractSmartBirtQuery smartQuery, SmartConnection connection) throws OdaException {
		try{
			// attempt to parse query
			if (smartQuery.getQuery() instanceof SimpleQuery) {
				((SimpleQuery) smartQuery.getQuery()).getFilter();
			} else if (smartQuery.getQuery() instanceof ObservationSummaryQuery) {
				ObservationSummaryQuery sumQuery = (ObservationSummaryQuery)smartQuery.getQuery();
			
				//date group by problem with reports 
				GroupByPart part = sumQuery.getQueryDefinition().getColumnGroupByPart();
				List<IGroupBy> headers = part.getGroupBys();
				for (IGroupBy h : headers){
					if (h instanceof DateGroupBy){
						throw new OdaException(SmartContext.INSTANCE
								.getClass(IQueryDateLabelProvider.class)
								.getLabel(IQueryDateLabelProvider.SUMMARY_DATE_GROUPBY_ERR, 
										connection.getCurrentLocale()));
					}
				}
			} else if (smartQuery.getQuery() instanceof ObservationGriddedQuery){
				((ObservationGriddedQuery)smartQuery.getQuery()).getQueryDefinition();
			}
		}catch (Exception ex){
			throw new OdaException(ex);
		}
	}

	@Override
	public IResultSet executeQuery(AbstractSmartBirtQuery smartQuery, SmartConnection connection) throws OdaException {
		//create date filter
		Date startDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			if (smartQuery.getQuery().getTypeKey().equals(ObservationSummaryQuery.KEY)){
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
		smartQuery.getQuery().setDateFilter(dateFilter);

		//the result set
		return super.executeQueryInternal(smartQuery, connection);	
	}

	@Override
	public IResultSetMetaData getMetaData(AbstractSmartBirtQuery smartQuery, SmartConnection connection)
			throws OdaException {
		return getMetaDataInternal(smartQuery, connection);
	}

	@Override
	public String[] getGeometryColumnNames(Query query){
		if (query.getTypeKey().equals(ObsObservationQuery.KEY) || query.getTypeKey().equals(ObservationWaypointQuery.KEY)){
			return new String[]{ObservationQueryResultItem.GEOMETRY_COLUMN_NAME};
		}
		return null;
	};
}
