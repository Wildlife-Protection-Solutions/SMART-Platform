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

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.IEmployeeListener;

/**
 * Employee delete listener for reports.  This listener
 * process the cross conservation area reports moving them
 * to a different user is available or deleting them all together
 * it no alternative user exists
 * 
 * @author Emily
 *
 */
public class ReportEmployeeListener implements IEmployeeListener {

	@Override
	public void beforeDelete(Employee e, Session s) {
		checkReport(e, s);
		checkReportFolder(e, s);
	}

	
	private void checkReport(Employee e, Session s){
		if (e.getConservationArea().getIsCcaa()){
			//ccaa queries we delete all associated queries and folders
			StringBuilder sql = new StringBuilder();
			sql.append("DELETE from Report "); //$NON-NLS-1$
			sql.append(" WHERE owner = :owner"); //$NON-NLS-1$
			Query q = s.createQuery(sql.toString());
			q.setParameter("owner", e); //$NON-NLS-1$
			q.executeUpdate();
		}
	}
	
	private void checkReportFolder(Employee e, Session s){
		if (e.getConservationArea().getIsCcaa()){
			//ccaa queries we delete all associated queries and folders
			StringBuilder sql = new StringBuilder();
			sql.append("DELETE from ReportFolder "); //$NON-NLS-1$
			sql.append(" WHERE employee = :owner"); //$NON-NLS-1$
			Query q = s.createQuery(sql.toString());
			q.setParameter("owner", e); //$NON-NLS-1$
			q.executeUpdate();
		}
	}
}
