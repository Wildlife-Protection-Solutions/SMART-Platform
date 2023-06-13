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
package org.wcs.smart.patrol.internal.advisors;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.ui.SmartLabelProvider;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Delete advisor for determining if an
 * employee can be deleted.  
 * Validates if the employee is a member of a patrol
 * or not.
 * 
 * @author Emily
 *
 */
public class EmployeeDeleteAdvisor  implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Employee)){
			return Messages.EmployeeDeleteAdvisor_InvalidObjectType;
		}
		Employee e = (Employee)object;
		if (e.getUuid() == null){
			return null;
		}
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Long> c = cb.createQuery(Long.class);
		Root<PatrolLegMember> root = c.from(PatrolLegMember.class);
		c.select(cb.count(root));
		c.where(cb.equal(root.get("id").get("member"), e));			  //$NON-NLS-1$//$NON-NLS-2$
		Long cnt = session.createQuery(c).uniqueResult();
		
		if (cnt == 0){
			return null;
		}else{
			return  MessageFormat.format(
					Messages.EmployeeDeleteAdvisor_DeleteError,
					new Object[]{cnt, SmartLabelProvider.getFullLabel(e)});
		}
	}

}
