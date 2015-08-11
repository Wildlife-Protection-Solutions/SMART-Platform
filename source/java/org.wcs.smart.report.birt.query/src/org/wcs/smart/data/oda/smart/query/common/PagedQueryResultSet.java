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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.model.Query;

/**
 * Result set for a SMART observation query.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PagedQueryResultSet extends AbstractQueryResultSet {

	private SimpleQueryResultSetMetadata metadata;

	private IPagedQueryResultSet pagedQueryResults;
	private Map<Integer, IResultItem> weakMap = new WeakHashMap<Integer, IResultItem>();

	/**
	 * Creates a new results set
	 * 
	 * @param metadata
	 *            query metadata
	 */
	public PagedQueryResultSet(Query query, 
			SimpleQueryResultSetMetadata metadata,
			SmartConnection connection) {
		super(metadata);
		this.metadata = metadata;

		try {
			pagedQueryResults = (IPagedQueryResultSet) query.getCachedResults();
			if (pagedQueryResults == null){
				pagedQueryResults = (IPagedQueryResultSet) QueryExecutor.INSTANCE.executeQuery(query, connection.getSession(), new NullProgressMonitor());
			}
			setMaxRows(pagedQueryResults.getItemCount());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected int getDatasetSize() {
		return pagedQueryResults.getItemCount();
	}

	@Override
	protected Object getCurrentItem(int colIndex) {
		return metadata.getQueryColumn(colIndex - 1).getValue(getItemForIndex(getRow()));
	}

	private IResultItem getItemForIndex(int index) {
		IResultItem item = weakMap.get(index);
		if (item != null)
			return item;
		//item was not previously loaded or was garbage collected
		List<? extends IResultItem> data = pagedQueryResults.getData(index, 500);
		for (int i = 0; i < data.size(); i++) {
			weakMap.put(index + i, data.get(i));
		}
		return data.get(0);
	}
	
	@Override
	public void close() throws OdaException {
		super.close();
		if (pagedQueryResults != null)
			pagedQueryResults.destroy();
		pagedQueryResults = null;
	}
	
}
