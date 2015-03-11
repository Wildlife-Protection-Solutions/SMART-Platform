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
package org.wcs.smart.entity.query.report.data;

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
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.entity.query.model.type.EntityGridQueryType;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.model.type.EntitySummaryQueryType;
import org.wcs.smart.entity.query.model.type.EntityWaypointQueryType;
import org.wcs.smart.entity.query.report.internal.Messages;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;

public class EntityReportQuery implements ISmartQuery {

	public EntityReportQuery() {
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
		} else if (smartQuery.getQuery() instanceof EntitySummaryQuery) {
			EntitySummaryQuery sumQuery = (EntitySummaryQuery)smartQuery.getQuery();
			
			//date group by problem with reports 
			GroupByPart part = sumQuery.getQueryDefinition().getColumnGroupByPart();
			List<IGroupBy> headers = part.getGroupBys();
			for (IGroupBy h : headers){
				if (h instanceof DateGroupBy){
					throw new OdaException(Messages.EntityReportQuery_SummaryDateGroupByInvalid);
				}
			}
		} else if (smartQuery.getQuery() instanceof EntityGriddedQuery){
			((EntityGriddedQuery)smartQuery.getQuery()).getQueryDefinition();
		}
	}

	@Override
	public IResultSet executeQuery(SmartQuery smartQuery, SmartConnection connection) throws OdaException {
		IResultSet resultSet = null;

		//create date filter
		Date startDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) smartQuery.getParameters().get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			if (smartQuery.getQuery().getType().getKey().equals(EntitySummaryQueryType.KEY)){
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
		
		if (smartQuery.getQuery().getType().getKey().equals(EntityObservationQueryType.KEY) ||
				smartQuery.getQuery().getType().getKey().equals(EntityWaypointQueryType.KEY)){
			((SimpleQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new PagedQueryResultSet(smartQuery.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(smartQuery),
					connection);
		}else  if (smartQuery.getQuery().getType().getKey().equals(EntityGridQueryType.KEY)){
			((GriddedQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet(smartQuery.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(smartQuery),
					connection);
		} else if (smartQuery.getQuery().getType().getKey().equals(EntitySummaryQueryType.KEY)){
			((SummaryQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new SummaryQueryResultSet(
					(SummaryQuery) smartQuery.getQuery(),
					new EntitySummaryQueryResultSetMetadata((SummaryQuery) smartQuery.getQuery()),
					connection);
		}
		
		return resultSet;
	}

	@Override
	public IResultSetMetaData getMetaData(SmartQuery smartQuery)
			throws OdaException {
		if (smartQuery.getQuery().getType().getKey().equals(EntityObservationQueryType.KEY) ||
				smartQuery.getQuery().getType().getKey().equals(EntityWaypointQueryType.KEY)){
			return new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery.getQuery());
		} else if (smartQuery.getQuery().getType().getKey().equals(EntitySummaryQueryType.KEY)) {
			return new EntitySummaryQueryResultSetMetadata((EntitySummaryQuery) smartQuery.getQuery());
		} else if (smartQuery.getQuery().getType().getKey().equals(EntityGridQueryType.KEY)){
			return new SimpleQueryResultSetMetadata( (GriddedQuery) smartQuery.getQuery());
		}
		throw new OdaException(Messages.EntityReportQuery_MetadataError);
	}

}
