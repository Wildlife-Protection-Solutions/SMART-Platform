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

import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;

/**
 * The start of a permission manager for providing security around
 * objects.  This is minimally populated and is being populated on
 * an as required basis. 
 * 
 * @author Emily
 *
 */
public enum PermissionManager {
	
	INSTANCE;
	
	/**
	 * Determine if the current user has permission to delete
	 * a particular object type.
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean canDelete(Class<?> clazz){
		if (clazz.equals(Station.class)){
			return isAdmin();
		}else if (clazz.equals(Employee.class)){
			return isAdmin();
		}
		return false;
	}
	
	/**
	 * Determines if the current user can configure smart desktop
	 * accounts
	 * @return
	 */
	public boolean canConfigureSmartUser(){
		return isAdmin();
	}
	
	/**
	 * Determines if the current user is an admin user.
	 * @return
	 */
	public boolean isAdmin(){
		SmartUserLevel level =SmartDB.getCurrentEmployee().getSmartUserLevel();
		if (level == null) return false;
		return level == SmartUserLevel.ADMIN;
	}
}
