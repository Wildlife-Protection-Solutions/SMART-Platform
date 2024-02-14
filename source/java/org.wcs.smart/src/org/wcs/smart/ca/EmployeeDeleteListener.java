/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import org.hibernate.Session;

/**
 * @since smart 8.0.0
 */
public class EmployeeDeleteListener implements IEmployeeListener{

	public static final EmployeeDeleteListener INSTANCE = new EmployeeDeleteListener();
	
	private EmployeeDeleteListener() {
		
	}
	
	@Override
	public void beforeDelete(Employee e, Session s) {
		
		//this shouldn't be necessary as it should be managed by the database constraints but I couldn't
		//get it to work without exceptions
		//see  https://app.assembla.com/spaces/smart-cs/tickets/3673
		s.createMutationQuery("UPDATE Waypoint SET lastModifiedBy = null WHERE lastModifiedBy = :del") //$NON-NLS-1$
		.setParameter("del", e) //$NON-NLS-1$
		.executeUpdate();
			
	}

}
