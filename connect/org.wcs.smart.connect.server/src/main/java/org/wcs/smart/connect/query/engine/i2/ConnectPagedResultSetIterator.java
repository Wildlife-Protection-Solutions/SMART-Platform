/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.i2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.i2.query.PagedResultSetIterator;

/**
 * Query result set iterator
 * 
 * @author Emily
 *
 */
public class ConnectPagedResultSetIterator extends PagedResultSetIterator{
	
	private ResultSet resultSet;
	private int numcols = 0;
	
	/**
	 * results can be null if results not computed yest
	 * @param results
	 * @param session
	 */
	public ConnectPagedResultSetIterator(IConnectPagedQueryResultSet results, Session session){
		super(results, session);
	}
	
	protected void createResultSet() {
		resultSet = session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection connection) throws SQLException {
				ResultSet rs = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery(((IConnectPagedQueryResultSet)results).getSelectQuery(session));
				numcols = rs.getMetaData().getColumnCount();
				return rs;
			}
		});
	}
	
	public boolean hasNext(){
		try {
			if (results == null) return false;
			if (resultSet == null) createResultSet();
			return resultSet.next();
		}catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public IResultItem next(){
		try {
			Object[] data = new Object[numcols];
			for (int i = 0; i < data.length; i ++) {
				data[i] = resultSet.getObject(i+1);
			}
			return results.asResultItem(data, session);
		}catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public void close() {
		try {
			resultSet.close();
		}catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
