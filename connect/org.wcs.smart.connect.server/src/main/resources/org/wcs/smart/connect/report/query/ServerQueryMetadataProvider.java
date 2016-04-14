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
package org.wcs.smart.connect.report.query;

import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.engine.ISummaryEngine;
import org.wcs.smart.data.oda.smart.impl.GeometryColumn;
import org.wcs.smart.data.oda.smart.impl.QueryMetadataProvider;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.query.common.IMetadataProvider;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Query metadata provider for BIRT SMART queries.
 * 
 * @author Emily
 *
 */
public enum ServerQueryMetadataProvider implements IMetadataProvider {
	INSTANCE; 
	
	@Override
	public IResultSetMetaData createMetadata(Query query, GeometryColumn[] geom, SmartConnection c)
			throws OdaException {
		
		if (query instanceof SummaryQuery){
			SummaryQuery a = (SummaryQuery)query;
			if (a.getDateFilter() == null){
				a.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, Last30DaysDateFilter.INSTANCE));
			}
			try{
				SummaryQueryResult results = new SummaryQueryResult();
				ISummaryEngine engine = (ISummaryEngine)QueryManager.INSTANCE.findQueryEngine(a);
				engine.getHeaderInfo(a, results, c.getCurrentLocale(), c.getSession());
				return new SummaryQueryResultSetMetadata(results);
			}catch (Exception ex){
				throw new OdaException(ex);
			}
		}
		return QueryMetadataProvider.INSTANCE.createMetadata(query, geom, c);
	}

}
