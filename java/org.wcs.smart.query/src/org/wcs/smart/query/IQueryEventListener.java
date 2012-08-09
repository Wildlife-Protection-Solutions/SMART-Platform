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

import org.hibernate.Session;
import org.wcs.smart.query.model.Query;

/**
 * Query save listener for before save events.
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IQueryEventListener {

	/**
	 * Event fired before the query is saved
	 * to the database.
	 * @param query The query to save to the database
	 * @param session the current hiberante database session
	 * @return <code>true</code> if the save should
	 * proceed <code>false</code> if save should
	 * be cancelled.
	 */
	public boolean beforeSave(Query query, Session session);
	
	/**
	 * Event fired before the query is deleted
	 * from the database.
	 * 
	 * @param query The query to delete to the database; NOT attached
	 * to the hiberante session
	 * @param session the current hiberante database session in transaction
	 * 
	 * @return <code>true</code> if the delete should
	 * proceed <code>false</code> if delete should
	 * be cancelled.
	 */
	public boolean beforeDelete(Query query, Session session);
}
