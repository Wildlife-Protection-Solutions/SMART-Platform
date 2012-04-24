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
package org.wcs.smart;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;

/**
 * A custom property tester that tests
 * the user level of the current logged in user.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SmartUserTester extends PropertyTester {
	
	/**
	 * Tests the current logged in user level against the level
	 * provided by the expectedValue.  
	 * 
	 * @param expectedValue one of "admin" or "readonly" otherwise false is returned.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (SmartDB.getCurrentEmployee() == null) return false;
		if (expectedValue.equals("admin")){
			return SmartDB.getCurrentEmployee().getSmartUserLevel() != null &&
				SmartDB.getCurrentEmployee().getSmartUserLevel().equals(Employee.SmartUserLevel.ADMIN);
		}else if (expectedValue.equals("manager")){
			return SmartDB.getCurrentEmployee().getSmartUserLevel() != null &&
					SmartDB.getCurrentEmployee().getSmartUserLevel().equals(Employee.SmartUserLevel.MANAGER);
		}else if (expectedValue.equals("analyst")){
			return SmartDB.getCurrentEmployee().getSmartUserLevel() != null &&
					SmartDB.getCurrentEmployee().getSmartUserLevel().equals(Employee.SmartUserLevel.ANALYST);
		}else if (expectedValue.equals("dataentry")){
			return SmartDB.getCurrentEmployee().getSmartUserLevel() != null &&
					SmartDB.getCurrentEmployee().getSmartUserLevel().equals(Employee.SmartUserLevel.DATA_ENTRY);
		}
		
		return false;
	}

}
