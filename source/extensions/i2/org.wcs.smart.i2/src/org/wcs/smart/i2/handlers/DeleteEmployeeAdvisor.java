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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
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
			return Messages.DeleteEmployeeAdvisor_InvalidObject;
		}

		Employee em = (Employee)object;
		
		StringBuilder sb = new StringBuilder();
		
		long cnt = QueryFactory.buildCountQuery(session, IntelAttachment.class, new Object[] {"createdBy", em}); //$NON-NLS-1$
		if (cnt != 0){
			sb.append(MessageFormat.format(Messages.DeleteEmployeeAdvisor_AttachmentError, SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		cnt = getReferenceCount(session, IntelEntity.class, em);
		if (cnt != 0){
			sb.append(MessageFormat.format(Messages.DeleteEmployeeAdvisor_EntityError, SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		cnt = getReferenceCount(session, IntelRecord.class, em);
		if (cnt != 0){
			sb.append(MessageFormat.format(Messages.DeleteEmployeeAdvisor_RecordError, SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		cnt = getReferenceCount(session, IntelWorkingSet.class, em);
		if (cnt != 0){
			sb.append(MessageFormat.format(Messages.DeleteEmployeeAdvisor_WsError, SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		cnt = getReferenceCount(session, IntelRecordObservationQuery.class, em);
		if (cnt != 0){
			sb.append(MessageFormat.format(Messages.DeleteEmployeeAdvisor_QueryError, SmartLabelProvider.getFullLabel(em), cnt));
		}
		
		if (sb.length() == 0) return null;
		
		return sb.append(Messages.DeleteEmployeeAdvisor_LinkError).toString();
	}

	private <T> Long getReferenceCount(Session session, Class<T> clazz, Employee em) {
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<T> root = c.from(clazz);
		c.select(cb.count(root));
		c.where(cb.or(
				cb.equal( root.get("createdBy"), em), //$NON-NLS-1$
				cb.equal( root.get("lastModifiedBy"), em))); //$NON-NLS-1$
		
		return session.createQuery(c).uniqueResult();
	}
}
