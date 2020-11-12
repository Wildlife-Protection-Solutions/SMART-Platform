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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;

/**
 * Interface for query engines which query waypoint/observations
 * and return the results
 * 
 * @author Emily
 *
 * @param <T>
 */
public interface IDesktopWOEngine<T extends IResultItem> extends IQueryEngine{

	/**
	 * The temporary database table containing in the queyr results
	 * @return
	 */
	public String getQueryDataTable();
	
	/**
	 * Generates the name for a temporary table
	 * @return
	 */
	public String createTempTableName();
	
	/**
	 * Convert the row represented in the result set to a query result item
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	public T asQueryResultItem(ResultSet rs, Session session) throws SQLException;
	
	/**
	 * Convert the row represented in the result set to an
	 * attachment result item 
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	public IAttachmentResultItem asQueryAttachmentResultItem(ResultSet rs, Session session) throws SQLException;
	
	
	/**
	 * Drops all temporary created database tables
	 * @param c
	 * @throws SQLException
	 */
	public void dropTables(Connection c) throws SQLException;
	
	/**
	 * Drops an individual database table
	 * @param c
	 * @param table
	 * @throws SQLException
	 */
	public void dropTable(Connection c, String table) throws SQLException;
}
