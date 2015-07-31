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
package org.wcs.smart.report.internal.advisor;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.LabelConstants;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;

/**
 * Delete advisor for determining if an
 * employee can be deleted.  
 * Validates if the employee owns any reports or
 * report folders;
 * 
 * @author Emily
 *
 */
public class EmployeeDeleteAdvisor  implements IDeleteAdvisor {

	/**
	 * @see org.wcs.smart.ca.advisors.IDeleteAdvisor#canDelete(java.lang.Object, org.hibernate.Session)
	 */
	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Employee)){
			return "Object not of type Employee. Can not delete."; //$NON-NLS-1$
		}
		Employee e = (Employee)object;
		if (e.getUuid() == null){
			return null;
		}
		Long cnt = (Long) session.createCriteria(Report.class).add(Restrictions.eq("owner", e)).setProjection(Projections.rowCount()).uniqueResult(); //$NON-NLS-1$
		if (cnt > 0){
			return MessageFormat.format( Messages.EmployeeDeleteAdvisor_Error_OwnsReports, new Object[]{LabelConstants.getFullLabel(e), cnt});
		}
	
		cnt = (Long) session.createCriteria(ReportFolder.class).add(Restrictions.eq("employee", e)).setProjection(Projections.rowCount()).uniqueResult(); //$NON-NLS-1$
		if (cnt > 0){
			return MessageFormat.format( Messages.EmployeeDeleteAdvisor_Error_OwnsFolders, new Object[]{LabelConstants.getFullLabel(e), cnt});
		}
		return null;
	}

}
