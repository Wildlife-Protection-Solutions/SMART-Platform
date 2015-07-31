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
package org.wcs.smart.data.oda.smart.query.common;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.model.ReportQuery;


/**
 * Query save listener that checks to determine if queries
 * are used in reports and warned users about changes before
 * the query is saved. 
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class PatrolReportQueryListener extends QueryListenerAdapter {

	private static final String WARNING_DIALOGTITLE = Messages.ReportQueryListener_Warning_DialogTitle;

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.IQuerySaveListener#beforeSave(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean beforeSave(Query query, Session session) {
		if (query.getUuid() == null) return true;
		try{
			@SuppressWarnings("unchecked")
			List<ReportQuery> queries = session.createCriteria(ReportQuery.class).add(Restrictions.eq("id.queryUuid", query.getUuid())).list(); //$NON-NLS-1$
			if (queries.size() == 0) {
				return true;
			}else{
				Query savedQuery = (Query) session.load(  Hibernate.getClass(query), query.getUuid());
				boolean confirmSave = true;
				if (savedQuery != null && savedQuery instanceof SimpleQuery){
					//simple queries only cause problems with visible columns change
					confirmSave = false;
					String savedVisible = ((SimpleQuery)savedQuery).getVisibleColumns();
					String origVisible = ((SimpleQuery)query).getVisibleColumns();
					
					if (!( (savedVisible == null && origVisible == null) || (savedVisible != null && savedVisible.equals(origVisible)))){					
						confirmSave = true;
					}
				}else if (savedQuery != null && savedQuery instanceof SummaryQuery){
					confirmSave = false;
					if (!((SummaryQuery)savedQuery).getQuery().equals( ((SummaryQuery)query).getQuery() )){
						confirmSave = true;
					}
				}else if (savedQuery instanceof GriddedQuery){
					confirmSave = false;
				}
				if (confirmSave){
					//	if the column definition 
					StringBuilder sb = new StringBuilder();
					for (ReportQuery rp : queries){
						sb.append("   * "); //$NON-NLS-1$
						sb.append(rp.getReport().getName());
						sb.append(" ["); //$NON-NLS-1$
						sb.append( rp.getReport().getId());
						sb.append("] {" ); //$NON-NLS-1$
						sb.append(LabelConstants.getFullLabel( rp.getReport().getOwner()));
						sb.append("}"); //$NON-NLS-1$
						sb.append("\n"); //$NON-NLS-1$
					}
					if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						WARNING_DIALOGTITLE, 
						MessageFormat.format(Messages.ReportQueryListener_BeforeSave_QueryUsedWarning, new Object[]{query.getName(), sb.toString()}))){
						return false;
					}
				}
				
				return true;
			}
		
		}catch (Exception ex){
			ReportPlugIn.displayLog(Messages.ReportQueryListener_QuerySaveError + ex.getLocalizedMessage(), ex);
			return false;
		}
	}

	

}
