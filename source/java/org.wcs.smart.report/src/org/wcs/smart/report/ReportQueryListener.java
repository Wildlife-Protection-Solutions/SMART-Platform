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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.query.event.QueryListenerAdapter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.ReportQuery;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Query save listener that checks to determine if queries
 * are used in reports and warned users about changes before
 * the query is saved. 
 * 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportQueryListener extends QueryListenerAdapter {

	private static final String WARNING_DIALOGTITLE = Messages.ReportQueryListener_Warning_DialogTitle;

	/**
	 * Removes query from query to report link
	 * @see org.wcs.smart.query.IQueryEventListener#beforeDelete(org.wcs.smart.query.model.Query, org.hibernate.Session)
	 */
	@Override
	public boolean beforeDelete(Query query, Session session) {
		if (query.getUuid() == null) return true;
		@SuppressWarnings("unchecked")
		List<ReportQuery> queries = session.createCriteria(ReportQuery.class).add(Restrictions.eq("id.queryUuid", query.getUuid())).list(); //$NON-NLS-1$
		if (queries.size() == 0) {
			return true;
		}else{
			
			StringBuilder sb = new StringBuilder();
			for (ReportQuery rp : queries){
				sb.append("   * "); //$NON-NLS-1$
				sb.append(rp.getReport().getName());
				sb.append(" ["); //$NON-NLS-1$
				sb.append(rp.getReport().getId());
				sb.append("] {"); //$NON-NLS-1$
				sb.append(SmartLabelProvider.getFullLabel(rp.getReport().getOwner()));
				sb.append("}"); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
			}
			if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
					WARNING_DIALOGTITLE, 
					MessageFormat.format(
							Messages.ReportQueryListener_BeforeDelete_QueryUsedWarning1, new Object[]{sb.toString(), query.getName() + " [" + query.getId() + "]"}))){ //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
			return true;
		}				
	}

}
