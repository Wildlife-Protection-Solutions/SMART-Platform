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
package org.wcs.smart.ca.datamodel;

import org.hibernate.Session;

/**
 * <p>
 * Data model edit advisors.
 * </p>
 * <p>
 * Plugins can implement this extension point to advise the data model editor
 * when categories or attributes should not be modified.  It will not prevent the
 * user from modifying the category/attribute however a warning message is displayed
 * to the users.
 * </p>
 * 
 * @author Emily
 *
 */
public interface IDmEditAdvisor {

	public static final String EXTENSION_ID = "org.wcs.smart.caDataModelAdvisor"; //$NON-NLS-1$
	
	/**
	 * 
	 * @param attribute  the attribute to be modified
	 * @param session current session
	 * @return reason item should not be edited or null if no issues
	 */
	public String canEdit(Attribute attribute, Session session);
	
	/**
	 * 
	 * @param category  the category to be modified
	 * @param session current session
	 * @return reason item should not be edited or null if no issues
	 */
	public String canEdit(Category category, Session session);
}
