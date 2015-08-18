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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.data.oda.smart.query.common.SummaryQueryResultSetMetadata;
import org.wcs.smart.er.query.engine.DerbySummaryEngine;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.report.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.Last30DaysDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Resultset metadata object for survey summary query
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SurveySummaryQueryResultSetMetadata extends SummaryQueryResultSetMetadata{

	public SurveySummaryQueryResultSetMetadata(SummaryQuery query) {
		super(query);
	}

	@Override
	protected void parseHeader(final SummaryQuery query) {
		Job parseQuery = new Job(Messages.SurveySummaryQueryResultSetMetadata_QueryJobName) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				try{
					//set a default date filter for parsing
					if (query.getDateFilter() == null){
						query.setDateFilter(new DateFilter(WaypointDateField.INSTANCE, 
								Last30DaysDateFilter.INSTANCE));
					}
					DerbySummaryEngine.getHeaderInfo(
							(SurveySummaryQuery)query, 
							results,
							new SurveyDesignFilter( ((SurveySummaryQuery)query).getSurveyDesign() ),
							session);
				}catch (Exception ex){
					throw new RuntimeException(ex);
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
		};
		parseQuery.schedule();
		
		try {
			parseQuery.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}	
	}

}
