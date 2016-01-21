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

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.internal.Messages;

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
		
		Long cnt = (Long) session.createCriteria(Employee.class)
				.add(Restrictions.eq("conservationArea", e.getConservationArea())) //$NON-NLS-1$
				.add(Restrictions.ne("uuid", e.getUuid())) //$NON-NLS-1$
				.add(Restrictions.eq("smartUserLevel", SmartUserLevel.ADMIN)) //$NON-NLS-1$
				.setProjection(Projections.rowCount()).uniqueResult();
		
		if (cnt == 0){
			return Messages.EmployeeDeleteAdvisor_Error_NoMoreAdmin;
		}else{
			return null;
		}
	}

}
