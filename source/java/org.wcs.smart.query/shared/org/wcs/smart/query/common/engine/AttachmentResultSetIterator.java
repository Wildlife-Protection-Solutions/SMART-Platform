/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;

/**
 * An iterator over a result set that includes attachments, one result
 * item per attachment
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class AttachmentResultSetIterator implements IQueryResultSetIterator<IAttachmentResultItem>{
	
	private ResultSet rs = null;
	
	IResultItemGenerator<ResultSet, IAttachmentResultItem> resultItemGenerator;
	Supplier<String> queryProvider;
	
	public AttachmentResultSetIterator(Session session,
			IResultItemGenerator<ResultSet, IAttachmentResultItem> resultItemGenerator,
			Supplier<String> queryProvider) {
		
		this.resultItemGenerator = resultItemGenerator;
		this.queryProvider = queryProvider;
		
		rs = getResultSet(session);
	}
	
	@Override
	public boolean hasNext() {
		try {
			return rs.next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IAttachmentResultItem next() {		
		try {
			return resultItemGenerator.apply(rs);
		}catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public void remove() {
		throw new IllegalStateException(
				"Remove operation is not supported."); //$NON-NLS-1$
	}

	@Override
	public void close() throws IOException {
		try {
			rs.close();
		} catch (SQLException e) {
			Logger.getLogger(AttachmentResultSetIterator.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		rs = null;
	}
	
	private ResultSet getResultSet(Session session) {
		final String dataSql = queryProvider.get();
		rs = session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(dataSql);	
			}
		});
		return rs;
	}
	
	
	public interface IResultItemGenerator<T, R>{
		R apply(T t) throws SQLException;
	}

}


