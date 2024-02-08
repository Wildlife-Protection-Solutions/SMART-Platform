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
package org.wcs.smart.query.map;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IGeometryResultItem;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.model.IMemoryQuery;
import org.wcs.smart.query.model.IPagedQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Feature reader for waypoint/observation query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	private SimpleFeatureType ftype;
	private Iterator<? extends IResultItem> fIterator;
	private List<QueryColumn> columns;
	private QueryColumn geometrycolumn;
		
	/**
	 * Creates a new feature reader.
	 * 
	 * @param query the query
	 * @param ftype the feature type
	 */
	public QueryFeatureReader(Query query, QueryColumn geometryColumn, 
			SimpleFeatureType ftype, List<QueryColumn> columns) {
		
		this.ftype = ftype;
		this.fIterator = null;
		this.columns = columns;
		this.geometrycolumn = geometryColumn;
		
		if (query instanceof IPagedQuery){
			try {
				IQueryResult cachedResults = query.getCachedResults();
				if (cachedResults != null){
					fIterator = ((IPagedQueryResultSet<?>)cachedResults).iterator(IPagedQueryResultSet.MAP_PAGE_SIZE);
				}
			} catch (Exception e) {
				QueryPlugIn.log(e.getMessage(), e);
			}	
		}else if (query instanceof IMemoryQuery) {
			fIterator = ((MemoryQueryResult<IResultItem>)query.getCachedResults()).getData().iterator();
		}
	}
	

	/**
	 * @see org.geotools.data.FeatureReader#close()
	 */
	@Override
	public void close() throws IOException {
		if (fIterator != null && fIterator instanceof Closeable c) c.close();
	}

	/**
	 * @see org.geotools.data.FeatureReader#getFeatureType()
	 */
	@Override
	public SimpleFeatureType getFeatureType() {
		return ftype;
	}

	/**
	 * @see org.geotools.data.FeatureReader#hasNext()
	 */
	@Override
	public boolean hasNext() throws IOException {
		if (fIterator == null){
			return false;
		}
		return fIterator.hasNext();
	}

	/**
	 * @see org.geotools.data.FeatureReader#next()
	 */
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		IResultItem next = this.fIterator.next();
		if (next instanceof IGeometryResultItem gi) {
			return gi.toSimpleFeature(ftype, geometrycolumn, columns);
		}
		return null;
	}

}
