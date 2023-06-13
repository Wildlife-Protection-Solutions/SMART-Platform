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
package org.wcs.smart.internal.ca.advisors;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.user.UserLevelManager;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Delete advisor to ensure that when an employee
 * is deleted at least one other admin user still
 * exists. 
 * 
 * @author egouge
 *
 */
public class EmployeeDeleteAdvisor  implements IDeleteAdvisor {

	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof Employee)){
			return Messages.EmployeeDeleteAdvisor_Error_NotEmployee;
		}
		Employee e = (Employee)object;
		if (e.getConservationArea().getIsCcaa()){
			//ccaa analysis does not require any users; they are created as needed
			return null;
		}
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Employee> query = cb.createQuery(Employee.class);
		Root<Employee> root = query.from(Employee.class);
		query.where(cb.and(
				cb.equal(root.get("conservationArea"), e.getConservationArea()), //$NON-NLS-1$
				cb.isNull(root.get("endEmploymentDate")), //$NON-NLS-1$
				cb.notEqual(root.get("uuid"), e.getUuid()), //$NON-NLS-1$
				cb.isNotNull(root.get("smartUserLevelKeys")) //$NON-NLS-1$
				)); 
		
		//find another active admin user
		List<Employee> others  = session.createQuery(query).getResultList();
		for (Employee other : others){
			if (UserLevelManager.INSTANCE.supportsUser(other, UserLevelManager.ADMIN)) return null;
		}
		
		return Messages.EmployeeDeleteAdvisor_Error_NoMoreAdmin;
	}

}
