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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.query.IQueryEventListener;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.model.SummaryQuery;
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
public class ReportQueryListener implements IQueryEventListener {

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.IQuerySaveListener#beforeSave(org.wcs.smart.query.model.Query)
	 */
	@Override
	public boolean beforeSave(Query query, Session session) {
		if (query.getUuid() == null) return true;
		try{
			@SuppressWarnings("unchecked")
			List<ReportQuery> queries = session.createCriteria(ReportQuery.class).add(Restrictions.eq("id.queryUuid", query.getUuid())).list();
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
						sb.append("   * " + rp.getReport().getName() + " ["+ rp.getReport().getId() + "] {" + rp.getReport().getOwner().getLabel() + "}");
						sb.append("\n");
					}
					if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						"Warning", "This query is used in the following reports.  By changing the query output columns you my invalidate the report.\n"  + sb.toString() + "  Are you sure you want to continue?")){
						return false;
					}
				}
				return true;
			}
		
		}catch (Exception ex){
			ReportPlugIn.displayLog("Error saving query : " + ex.getMessage(), ex);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.IQueryEventListener#beforeDelete(org.wcs.smart.query.model.Query, org.hibernate.Session)
	 */
	@Override
	public boolean beforeDelete(Query query, Session session) {
		if (query.getUuid() == null) return true;
		@SuppressWarnings("unchecked")
		List<ReportQuery> queries = session.createCriteria(ReportQuery.class).add(Restrictions.eq("id.queryUuid", query.getUuid())).list();
		if (queries.size() == 0) {
			return true;
		}else{
			
			StringBuilder sb = new StringBuilder();
			for (ReportQuery rp : queries){
				sb.append("   * " + rp.getReport().getName() + " ["+ rp.getReport().getId() + "] {" + rp.getReport().getOwner().getLabel() + "}");
				sb.append("\n");
			}
			if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
					"Warning", "This query is referenced by the following reports.  By deleting the queries the reports will no longer run.\n"  + sb.toString() + "\n Are you sure you want to continue?")){
				return false;
			}
			return true;
		}				
	}

}
