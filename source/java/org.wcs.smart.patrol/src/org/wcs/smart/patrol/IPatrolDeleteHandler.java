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
package org.wcs.smart.patrol;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface IPatrolDeleteHandler {

	
	/**
	 * Throw an exception if you cannot delete and the delete
	 * should be cancelled.
	 * 
	 * If the users returns false, the user should display a message
	 * to the user informing why it cannot be removed.  Otherwise the user
	 * can throw an exception and the delete manager will display the exception 
	 * to the user.
	 * 
	 * @param patrol the patrol to delete
	 * @param session the delete session in transaction
	 * @param monitor the progress monitor
	 * @return true if the patrol can be removed, false if it should not be removed
	 * @throws Exception if error occurs and the patrol information could not be removed
	 */
	public boolean beforeDelete(Patrol patrol, Session session, IProgressMonitor monitor) throws Exception;

	/**
	 * Perform some operation if needed after patrol was actually removed from database.
	 * 
	 * @param patrol the patrol to delete
	 * @param monitor the progress monitor
	 */
	public void afterDelete(Patrol patrol, IProgressMonitor monitor);
}
