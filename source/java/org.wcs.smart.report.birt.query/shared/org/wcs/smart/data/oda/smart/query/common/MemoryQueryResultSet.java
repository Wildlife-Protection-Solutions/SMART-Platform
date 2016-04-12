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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Result set for a simple SMART query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class MemoryQueryResultSet extends AbstractQueryResultSet {

	private List<IResultItem> items = null;
	private SimpleQueryResultSetMetadata metadata;


	/**
	 * Creates a new results set
	 * 
	 * @param metadata query metadata
	 * @param query gridded query
	 */
	public MemoryQueryResultSet(MemoryQueryResult<?> results,
			SimpleQueryResultSetMetadata metadata,
			SmartConnection connection) {
		super(metadata);
		this.metadata = metadata;

		init(results.getData());
	}


	public MemoryQueryResultSet(GridQueryResult results,
			SimpleQueryResultSetMetadata metadata,
			SmartConnection connection) {
		super(metadata);
		this.metadata = metadata;
		
		init(results.getData());
	}
	
	
	@SuppressWarnings("unchecked")
	private void init(Collection<? extends IResultItem> queryResults){
		if (queryResults == null){
			queryResults = new ArrayList<IResultItem>();
		}
		setMaxRows(queryResults.size());
		
		if (queryResults instanceof List){
			items =  (List<IResultItem>)queryResults;
		}else{
			items = new ArrayList<IResultItem>();
			for (IResultItem i : queryResults){
				items.add(i);
			}
		}
	}
	
	@Override
	protected int getDatasetSize() {
		return items.size();
	}

	@Override
	protected Object getCurrentItem(int colIndex) throws OdaException{
		QueryColumn col = metadata.getQueryColumn(colIndex - 1);
		if (col != null) return col.getValue(items.get(getRow()));
		
		IResultItem item = items.get(getRow());
		if (item instanceof IGeometryResultItem){
			return ((IGeometryResultItem)item).asGeometry(getMetaData().getColumnName(colIndex));
		}
		return null;
	}

	@Override
	public void close() throws OdaException {
		super.close();
		items = null;
	}
}
