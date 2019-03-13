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
package org.wcs.smart.patrol.query.map.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDataSource extends ContentDataStore{

	/**
	 * waypoint query data source
	 */
	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	
	private SimpleQuery query;
	private IProjectionProvider prjProvider;
	private List<QueryColumn> columns;
		
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(PatrolObservationQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
	}
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(PatrolWaypointQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
	}
	
	public SimpleQuery getQuery() {
		return this.query;
	}
	
	public List<QueryColumn> getColumns(){
		return this.columns;
	}
	
	/**
	 * @see org.geotools.data.AbstractDataStore#dispose()
	 */
	@Override
	public void dispose(){
		this.prjProvider = null;
		this.columns = null;
		super.dispose();
	}

	/**
	 * @see org.geotools.data.store.ContentDataStore#createTypeNames()
	 */
	@Override
	protected List<Name> createTypeNames() throws IOException {
		List<Name> names = new ArrayList<>();
		names.add(new NameImpl(WAYPOINT_TYPE));
		return names;
	}

	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		if (this.columns == null) {
			this.columns = query.computeQueryColumns(Locale.getDefault(),  null, prjProvider);
		}
		return new QueryFeatureSource(entry);
	}
	
}
