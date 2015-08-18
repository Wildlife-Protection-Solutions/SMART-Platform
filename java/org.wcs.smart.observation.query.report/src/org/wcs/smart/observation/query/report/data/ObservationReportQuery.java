package org.wcs.smart.observation.query.report.data;

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
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.report.internal.Messages;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.DateGroupBy;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;

public class ObservationReportQuery implements ISmartQuery {

	public ObservationReportQuery() {
	}

	/**
	 * Here was load the query from the database
	 * and parse it to ensure it is valid.
	 */
	@Override
	public void prepare(SmartQuery smartQuery) throws OdaException {
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
						throw new OdaException(Messages.ObservationReportQuery_SummaryDateGroupByInvalid);
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
	public IResultSet executeQuery(SmartQuery smartQuery, SmartConnection connection) throws OdaException {
		IResultSet resultSet = null;

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


		//the result set
		
		if (smartQuery.getQuery().getTypeKey().equals(ObsObservationQuery.KEY) ||
				smartQuery.getQuery().getTypeKey().equals(ObservationWaypointQuery.KEY)){
			((SimpleQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new PagedQueryResultSet(smartQuery.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(smartQuery),
					connection);
		}else  if (smartQuery.getQuery().getTypeKey().equals(ObservationGriddedQuery.KEY)){
			((GriddedQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new MemoryQueryResultSet(smartQuery.getQuery(), 
					(SimpleQueryResultSetMetadata)getMetaData(smartQuery),
					connection);
		} else if (smartQuery.getQuery().getTypeKey().equals(ObservationSummaryQuery.KEY)){
			((SummaryQuery) smartQuery.getQuery()).setDateFilter(dateFilter);
			resultSet = new SummaryQueryResultSet(
					(SummaryQuery) smartQuery.getQuery(),
					new ObservationSummaryQueryResultSetMetadata((SummaryQuery) smartQuery.getQuery()),
					connection);
		}
		
		return resultSet;
	}

	@Override
	public IResultSetMetaData getMetaData(SmartQuery smartQuery)
			throws OdaException {
		if (smartQuery.getQuery().getTypeKey().equals(ObsObservationQuery.KEY) ||
				smartQuery.getQuery().getTypeKey().equals(ObservationWaypointQuery.KEY)){
			return new SimpleQueryResultSetMetadata((SimpleQuery) smartQuery.getQuery());
		} else if (smartQuery.getQuery().getTypeKey().equals(ObservationSummaryQuery.KEY)) {
			return new ObservationSummaryQueryResultSetMetadata((ObservationSummaryQuery) smartQuery.getQuery());
		} else if (smartQuery.getQuery().getTypeKey().equals(ObservationGriddedQuery.KEY)){
			return new SimpleQueryResultSetMetadata( (ObservationGriddedQuery) smartQuery.getQuery());
		}
		throw new OdaException(Messages.ObservationReportQuery_MetadataError);
	}

}
