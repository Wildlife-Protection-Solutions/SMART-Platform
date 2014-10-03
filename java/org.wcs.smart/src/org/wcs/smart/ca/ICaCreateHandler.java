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
package org.wcs.smart.ca;

import org.hibernate.Session;

/**
 * A handler to implement and add to the ConservationAreaManager
 * delete handlers. These handlers are used when a
 * conservation area is created.  Use this handle if you need to setup/populate
 * some default settings/data when new conservation area is added to the database.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public interface ICaCreateHandler {
	
	public static final String EXTENSION_ID = "org.wcs.smart.ca.create"; //$NON-NLS-1$

	/**
	 * Throw an exception if you cannot delete and the delete
	 * should be cancelled.
	 * 
	 * @param ca the conservation that is just created
	 * @param session the delete session
	 * @throws Exception if error occurs and the ca should not be created
	 */
	public void afterCreate(ConservationArea ca, Session session) throws Exception;
	
}
