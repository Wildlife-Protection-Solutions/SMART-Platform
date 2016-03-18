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
package org.wcs.smart.query.common.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;

/**
 * Query result set backed by table(s) in the database.
 *  
 * @author Emily
 *
 */
public interface ITablePagedQueryResultSet extends IPagedQueryResultSet{
	
	/**
	 * Gets results from the given result get.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException;
	
	/**
	 * Opens a result set in the given session that accessed the query results
	 */
	public ResultSet getResultSet(Session session);
	
//	/**
//	 * Destroys the result set and any table associated with it
//	 * 
//	 * @param session
//	 */
//	public void destory(Session session) throws SQLException;
	
}
