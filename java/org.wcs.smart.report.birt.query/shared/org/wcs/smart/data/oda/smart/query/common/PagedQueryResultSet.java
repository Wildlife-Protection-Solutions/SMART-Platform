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
package org.wcs.smart.data.oda.smart.query.common;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;

/**
 * Result set for a SMART observation query.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PagedQueryResultSet extends AbstractQueryResultSet {

	private SimpleQueryResultSetMetadata metadata;
	private IPagedQueryResultSet pagedQueryResults;
	private IQueryResultSetIterator<? extends IResultItem> iterator;
	private IResultItem currentItem = null;
	/**
	 * Creates a new results set
	 * 
	 * @param metadata
	 *            query metadata
	 */
	public PagedQueryResultSet(IPagedQueryResultSet results,
			SimpleQueryResultSetMetadata metadata,
			SmartConnection connection) {
		super(metadata);
		this.metadata = metadata;

		try {
			pagedQueryResults = results;
			setMaxRows(pagedQueryResults.getItemCount());
			this.iterator = pagedQueryResults.iterator(500, connection.getSession());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected int getDatasetSize() {
		return pagedQueryResults.getItemCount();
	}

	@Override
	public boolean next() throws OdaException{
		boolean value = super.next();
		if (value){
			currentItem = iterator.next();
		}
		return value;
	}
	@Override
	protected Object getCurrentItem(int colIndex) {
		return metadata.getQueryColumn(colIndex - 1).getValue(currentItem);
	}
	
	@Override
	public void close() throws OdaException {
		super.close();
		try{
			this.iterator.close();
			this.iterator = null;
		}catch (Exception ex){
			throw new OdaException(ex);
		}
		if (pagedQueryResults != null)
			pagedQueryResults.destroy();
		pagedQueryResults = null;
	}
	
}
