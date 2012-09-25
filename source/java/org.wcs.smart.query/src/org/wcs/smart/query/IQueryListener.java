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
package org.wcs.smart.query;

import org.wcs.smart.query.model.Query;

/**
 * Listener for query changes
 * 
 * @author Emily
 * @since 1.0.0
 */
public interface IQueryListener {

	/**
	 * Fired when the given query has changed.  A change in query:
	 * occurs when the query filter has changed. 
	 * 
	 * @param query the query being changed.
	 */
	public void queryChanged(Query query);
	
	
	/**
	 * Fired when the given query should be run 
	 * 
	 * @param query the query to run
	 */
	public void queryRun(Query query);
	
	
	/**
	 * Fired when the query name has been modified.
	 * @param query with newName set
	 */
	public void queryNameUpdated(Query query);
	
}
