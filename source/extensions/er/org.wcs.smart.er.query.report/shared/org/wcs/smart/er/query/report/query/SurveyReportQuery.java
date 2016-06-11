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
package org.wcs.smart.er.query.report.query;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartBirtQuery;
import org.wcs.smart.data.oda.smart.impl.AbstractSmartQuery;
import org.wcs.smart.data.oda.smart.impl.GeometryColumn;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.SmartParameterMetaData;
import org.wcs.smart.data.oda.smart.query.common.EmptyResultSet;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
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
 * Implementation class of IQuery for the SMART ODA runtime driver. <br>
 * This wraps around any survey queries (including summaries, mission, and observation
 * queries).
 */
public class SurveyReportQuery extends AbstractSmartQuery {


	/**
	 * @see
	 * org.eclipse.datatools.connectivity.oda.IQuery#prepare(java.lang.String)
	 * <p>Here the queryText contains the hex encoded uuid
	 * of the query.  The query is loaded from the database and
	 * parsed to ensure it is valid.
	 * </p>
	 */
	@Override
	public void prepare(AbstractSmartBirtQuery query, SmartConnection connection) throws OdaException {
		try{
			// attempt to parse query
			if (query.getQuery() instanceof SimpleQuery) {
				((SimpleQuery) query.getQuery()).getFilter();
			} else if (query.getQuery() instanceof SurveySummaryQuery) {
				SurveySummaryQuery sumQuery = (SurveySummaryQuery)query.getQuery();
			
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
			} else if (query.getQuery() instanceof SurveyGriddedQuery){
				((SurveyGriddedQuery)query.getQuery()).getQueryDefinition();
			}
		}catch (Exception ex){
			throw new OdaException(ex);
		}
	}

	/**
	 * @see org.eclipse.datatools.connectivity.oda.IQuery#getMetaData()
	 */
	@Override
	public IResultSetMetaData getMetaData(AbstractSmartBirtQuery query, SmartConnection connection) throws OdaException {
		return getMetaDataInternal(query, connection);
	}

	@Override
	public IResultSet executeQuery(AbstractSmartBirtQuery query, SmartConnection connection) throws OdaException {
		//create date filter
		Date startDate = (Date) query.getParameters().get(SmartParameterMetaData.Parameter.STARTDATE);
		Date endDate = (Date) query.getParameters().get(SmartParameterMetaData.Parameter.ENDDATE);
		
		if (startDate == null || endDate == null){
			if (query.getQuery().getTypeKey().equals(SurveySummaryQuery.KEY)){
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
		DateFilter dateFilter = new DateFilter(WaypointDateField.INSTANCE,cd);

		query.getQuery().setDateFilter(dateFilter);
		//the result set
		return super.executeQueryInternal(query, connection);
	}

	@Override
	public GeometryColumn[] getGeometryColumns(String queryTypeKey, Locale l) {
		if (queryTypeKey.equals(SurveyObservationQuery.KEY) ||
				queryTypeKey.equals(SurveyWaypointQuery.KEY)){		
			return new GeometryColumn[]{
					new GeometryColumn(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.GEOMETRY_LABEL, l),SurveyQueryResultItem.WAYPOINT_GEOMCOLUMN_KEY)};
		}else if (queryTypeKey.equals(MissionQuery.KEY)){
			return new GeometryColumn[]{
					new GeometryColumn(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.GEOMETRY_LABEL, l),SurveyQueryResultItem.TRACK_GEOMCOLUMN_KEY)};
		}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
			return new GeometryColumn[]{
					new GeometryColumn(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.GEOMETRY_LABEL, l),MissionTrackResultItem.TRACK_GEOMCOLUMN_KEY)};
		}
		return null;
	}
	
	
}
