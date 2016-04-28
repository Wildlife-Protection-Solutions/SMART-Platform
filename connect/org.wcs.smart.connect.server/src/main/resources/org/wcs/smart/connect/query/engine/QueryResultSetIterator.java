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
package org.wcs.smart.connect.query.engine;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ITablePagedQueryResultSet;

/**
 * Iterator for iterating over lazy loaded query results.
 * 
 * @author Emily
 *
 * @param <T>
 */
public class QueryResultSetIterator<T extends IResultItem> implements IQueryResultSetIterator<IResultItem>{

	private int itOffset = -1; // offset of element at which list begins
	private int itIndex = 0;
	private List<IResultItem> data;
	private int pageSize = 0;
	private ITablePagedQueryResultSet rs;
	private ResultSet queryResults;
	
	private Session session = null;
	
	public QueryResultSetIterator(ITablePagedQueryResultSet rs, int pageSize, Session session) {
		this.pageSize = pageSize;
		this.rs = rs;
		this.session = session;
		this.queryResults = null;
	}
	
	@Override
	public boolean hasNext() {
		init();
		return itOffset + itIndex + 1 < rs.getItemCount();
	}
	private void init(){
		if (queryResults != null) return;
		if (rs.getItemCount() > 0) queryResults = rs.getResultSet(session);
	}

	@Override
	public IResultItem next() {
		init();
		if (!hasNext())
			throw new NoSuchElementException();
		if (data == null) {
			itOffset = 0;
			itIndex = 0;
			getData();
			return data.get(itIndex);
		}
		itIndex++;
		if (itIndex < data.size()) {
			return data.get(itIndex);
		}
		// we need to load new portion of data
		itOffset += data.size();
		itIndex = 0;
		getData();
		return data.get(itIndex);
	}

	private void getData(){
		try{
			data = rs.getResults(session, queryResults, itOffset, pageSize);
		}catch (Exception ex){
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
	
	@Override
	public void remove() {
		throw new IllegalStateException(
				"Remove operation is not supported."); //$NON-NLS-1$
	}

	@Override
	public void close() throws IOException {
	}

}
