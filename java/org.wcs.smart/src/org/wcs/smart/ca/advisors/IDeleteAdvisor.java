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
package org.wcs.smart.ca.advisors;

import org.hibernate.Session;

/**
 * Advisor for validating deletion of hibernate
 * objects.  Plugins can extend the deleteAdvisor extension point
 * and implement an IDeleteAdvisor to validate deletions.
 * 
 * Plugsin should not change the state of the 
 * database during this check.  They can only validate
 * if the paricular item can be removed.
 * 
 * @author Emily
 *
 */
public interface IDeleteAdvisor {

	/**
	 * 
	 * @param object the object to be removed
	 * @param session the current open hibernate session
	 * @return <code>null</code> if okay to delete object; otherwise a string message to 
	 * display to user explaining whey item cannot be deleted.
	 */
	String canDelete(Object object, Session session);
	
}
