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

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.query.common.IMetadataProvider;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.er.query.engine.DerbySummaryEngine;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Metadata provider for suvery summary queries.
 * 
 * @author Emily
 *
 */
public class SummaryMetadataProvider implements IMetadataProvider {

	@Override
	public IResultSetMetaData createMetadata(Query query, SmartConnection c) throws OdaException {
		SurveySummaryQuery q = (SurveySummaryQuery)query;
		
		//set a default date filter for parsing
		if (q.getDateFilter() == null){
			q.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, Last30DaysDateFilter.INSTANCE));
		}
			
		SummaryQueryResult results = new SummaryQueryResult();
		try {
			DerbySummaryEngine.getHeaderInfo(
					q, 
					results,
					new SurveyDesignFilter( q.getSurveyDesign() ),
					c.getSession());
		} catch (Exception e) {
			throw new OdaException(e);
		}
		return new SummaryQueryResultSetMetadata(results);
	}

}
