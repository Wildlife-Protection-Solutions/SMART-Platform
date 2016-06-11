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
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;

/**
 * Wrapper for loading intelligence queries in report.
 * 
 * @author Emily
 *
 */
public class IntelligenceReportQuery extends AbstractSmartQuery {

	public IntelligenceReportQuery() {
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
			} else if (smartQuery.getQuery() instanceof IntelligenceSummaryQuery) {
			
			}
		}catch(Exception ex){
			throw new OdaException(ex);
		}
	}

	@Override
	public IResultSet executeQuery(AbstractSmartBirtQuery smartQuery, SmartConnection connection) throws OdaException {
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
				ReceivedDateFilter.INSTANCE,cd);

		smartQuery.getQuery().setDateFilter(dateFilter);
		
		return super.executeQueryInternal(smartQuery, connection);
	}

	@Override
	public IResultSetMetaData getMetaData(AbstractSmartBirtQuery smartQuery, SmartConnection connection)
			throws OdaException {
		if (smartQuery.getQuery().getTypeKey().equals(IntelligenceRecordQuery.KEY)){
			return getMetaDataInternal(smartQuery, connection);
		}else if (smartQuery.getQuery().getTypeKey().equals(IntelligenceSummaryQuery.KEY)){
			return new SummaryQueryResultSetMetadata(IntelligenceSummaryQuery.createResultTemplate(Locale.getDefault()));
		}
		throw new OdaException("Unsupported query type."); //$NON-NLS-1$
	}
	
	@Override
	public GeometryColumn[] getGeometryColumns(String queryTypeKey, Locale l) {
		if (queryTypeKey.equals(IntelligenceRecordQuery.KEY)){		
			return new GeometryColumn[]{
					new GeometryColumn(SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(ICoreLabelProvider.GEOMETRY_LABEL, l),
							IntelligenceRecordResultItem.GEOMCOLUMN_KEY)};
		}
		return null;
	}
}
