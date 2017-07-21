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

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;

/**
 * A handler to implement and add to the ConservationAreaManager
 * delete handlers. These handlers are used when a
 * conservation area is deleted.  It should remove
 * all information associated with the conservation area
 * that the plug-in has added to the database.
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface ICaDeleteHandler {

	/**
	 * Throw an exception if you cannot delete and the delete
	 * should be cancelled.
	 * 
	 * @param ca the conservation being deleted
	 * @param session the delete session
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility to call done() on the given monitor
	 * @throws Exception if error occurs and the ca information could not be removed
	 */
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception;
}
