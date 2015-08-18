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
package org.wcs.smart.report;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.AbstractQueryPropertyProvider;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.ReportQuery;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Query property that will display a list of reports
 * associated with the query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryReportPropertyProvider extends AbstractQueryPropertyProvider {

	public QueryReportPropertyProvider() {
	}


	/**
	 * Determines which reports are used by the given query.
	 * 
	 * @see org.wcs.smart.query.AbstractQueryPropertyProvider#getValue(org.wcs.smart.query.model.Query)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String getValue(Query query) {
		if (query.getUuid() == null) return ""; //$NON-NLS-1$
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			List<ReportQuery> reports = s.createCriteria(ReportQuery.class).add(Restrictions.eq("id.queryUuid", query.getUuid())).list(); //$NON-NLS-1$
			if (reports.size() == 0){
				return Messages.QueryReportPropertyProvider_NoReportsLabel;
			}else{
				StringBuilder sb = new StringBuilder();
				for (ReportQuery rq : reports){
					sb.append("* "); //$NON-NLS-1$
					sb.append(rq.getReport().getName());
					sb.append(" ["); //$NON-NLS-1$
					sb.append(rq.getReport().getId());
					sb.append("] {"); //$NON-NLS-1$
					sb.append(Messages.QueryReportPropertyProvider_OwnerLabel); 
					sb.append(": "); //$NON-NLS-1$
					sb.append(SmartLabelProvider.getFullLabel(rq.getReport().getOwner()));
					sb.append( "}");  //$NON-NLS-1$
					
					sb.append("\n"); //$NON-NLS-1$
				}
				if (sb.length() > 1){
					sb.delete(sb.length() - 1, sb.length());
				}
				return sb.toString();
			}
		}catch (Exception ex){
			ReportPlugIn.log("Error loading query properties", ex); //$NON-NLS-1$
			return Messages.QueryReportPropertyProvider_ErrorLabel;
		}finally{
			if (s.getTransaction().isActive()){
				s.getTransaction().commit();
			}
			s.close();
		}
	}


	/**
	 * @return <code>true</code>
	 * @see org.wcs.smart.query.IQueryPropertyProvider#isValid(org.wcs.smart.query.model.Query.QueryType)
	 */
	@Override
	public boolean isValid(IQueryType query) {
		return true;
	}

}
