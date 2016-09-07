/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.handlers;

import java.text.MessageFormat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Checks employees for ownership/modification of various intelligence components 
 * 
 * @author Emily
 *
 */
public class DeleteEmployeeAdvisor implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Employee)){
			return "Object not an Employee object.  Cannot delete.";
		}

		Employee em = (Employee)object;
		
		StringBuilder sb = new StringBuilder();
		
		Criteria query = session.createCriteria(IntelAttachment.class);
		query.add(Restrictions.eq("createdBy", em)); //$NON-NLS-1$
		query.setProjection(Projections.rowCount());
		long cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			sb.append(MessageFormat.format("The employee {0} is associated with {1,number,integer} intelligence attachments. ", SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		query = session.createCriteria(IntelEntity.class);
		query.add(Restrictions.or(Restrictions.eq("createdBy", em), Restrictions.eq("lastModifiedBy", em))); //$NON-NLS-1$
		query.setProjection(Projections.rowCount());
		cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			sb.append(MessageFormat.format("The employee {0} is associated with {1,number,integer} intelligence entities.", SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		query = session.createCriteria(IntelRecord.class);
		query.add(Restrictions.or(Restrictions.eq("createdBy", em), Restrictions.eq("lastModifiedBy", em))); //$NON-NLS-1$ //$NON-NLS-2$
		query.setProjection(Projections.rowCount());
		cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			sb.append(MessageFormat.format("The employee {0} is associated with {1,number,integer} intelligence records.", SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		query = session.createCriteria(IntelWorkingSet.class);
		query.add(Restrictions.or(Restrictions.eq("createdBy", em), Restrictions.eq("lastModifiedBy", em))); //$NON-NLS-1$ //$NON-NLS-2$
		query.setProjection(Projections.rowCount());
		cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			sb.append(MessageFormat.format("The employee {0} is associated with {1,number,integer} intelligence working sets.", SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		query = session.createCriteria(IntelRecordQuery.class);
		query.add(Restrictions.or(Restrictions.eq("createdBy", em), Restrictions.eq("lastModifiedBy", em))); //$NON-NLS-1$ //$NON-NLS-2$
		query.setProjection(Projections.rowCount());
		cnt = (Long)query.uniqueResult();
		if (cnt != 0){
			sb.append(MessageFormat.format("The employee {0} is associated with {1,number,integer} intelligence queries.", SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		if (sb.length() == 0) return null;
		
		return sb.append("The above links must be removed before the employee can be deleted.").toString();
	}

}
