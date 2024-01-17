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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.ca.IGeometryColumn;
import org.wcs.smart.query.model.IStyledQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDataSource extends ContentDataStore{
	
	protected Query query;
	protected IProjectionProvider prjProvider;
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(Query query, IProjectionProvider prjProvider){
		if (!(query instanceof IStyledQuery)) {
			throw new UnsupportedOperationException("cannot add non-styled queries to map");			
		}
		this.query = query;
		this.prjProvider = prjProvider;
	}
	

	public Query getQuery() {
		return this.query;
	}
	
	public IProjectionProvider getProjectionProvider() {
		return this.prjProvider;
	}
	
	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		this.prjProvider = null;
	}

	
	private Map<String, QueryColumn> nameToQcMap;
	
	public String getLayerName(Name name) {
		if (!nameToQcMap.containsKey(name.getLocalPart())) return null;
		return nameToQcMap.get(name.getLocalPart()).getName();
	}
	
	public QueryColumn getQueryColumn(Name name) {
		return nameToQcMap.get(name.getLocalPart()); 
	}
	
	@Override
	protected List<Name> createTypeNames() throws IOException {
		nameToQcMap = new HashMap<>();
		List<Name> types = new ArrayList<>();
		List<QueryColumn> columns = ((IStyledQuery) getQuery()).computeQueryColumns(Locale.getDefault(), null, this.prjProvider);
		for (QueryColumn qc : columns) {
			if (qc instanceof IGeometryColumn qcc) {
				Name name = new NameImpl("smartquery", qc.getKey()); //$NON-NLS-1$
				types.add(name);
				nameToQcMap.put(name.getLocalPart(), qc);
			}
		}
		
		return types;
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry)
			throws IOException {
		return new QueryFeatureSource(entry);
	}

	
}
