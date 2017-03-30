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
package org.wcs.smart.i2.udig.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.opengis.feature.type.Name;
import org.wcs.smart.i2.query.IPagedQueryResultSet;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDataSource extends ContentDataStore{

	/*
	 * Query have both point and polygon result layers
	 */
	public static final Name POINT_TYPE = FeatureGenerator.POINT_TYPE;
	public static final Name POLYGON_TYPE = FeatureGenerator.POLYGON_TYPE;
	
	private IPagedQueryResultSet results;
	
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(IPagedQueryResultSet results){
		this.results = results;
	}
	
	public void setResults(IPagedQueryResultSet results){
		this.results = results;
	}

	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
	}
	
	public IPagedQueryResultSet getResultSet(){
		return this.results;
	}


	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		return new QueryContentFeatureSource(entry, this);
	}


	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> typeNames = new ArrayList<>();
		typeNames.add(POINT_TYPE);
		typeNames.add(POLYGON_TYPE);
		return typeNames;
	}
	
}
