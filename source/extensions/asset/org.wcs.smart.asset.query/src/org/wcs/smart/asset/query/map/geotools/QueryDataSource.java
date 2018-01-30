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
package org.wcs.smart.asset.query.map.geotools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;

/**
 * Geotools data source for waypoint query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDataSource extends ContentDataStore {

	/**
	 * waypoint query data source
	 */
	public static final String WAYPOINT_TYPE = "Waypoint"; //$NON-NLS-1$
	
	private Query query;
	private List<QueryColumn> columns;
	private IProjectionProvider prjProvider;
	
	//single feature source
	private QueryFeatureSource tableSrc = null;
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(AssetSummaryQuery query){
		this.query = query;
	}
	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(AssetObservationQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
	}

	
	/**
	 * Creates a new data source from the give query.
	 * 
	 * @param query
	 */
	public QueryDataSource(AssetWaypointQuery query, IProjectionProvider prjProvider){
		this.query = query;
		this.prjProvider = prjProvider;
	}
	
	public Query getQuery() {
		return this.query;
	}

	public synchronized List<QueryColumn> getColumns(){
		if (columns != null) return columns;
		
		if (query instanceof SimpleQuery) {
			columns = ((SimpleQuery)query).computeQueryColumns(Locale.getDefault(),  null, prjProvider);
		}else if (query instanceof AssetSummaryQuery) {
			try {
				columns = getSummaryColumns((AssetSummaryQuery)query);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return columns;
	}
	
	@Override
	public void removeSchema(Name typeName) throws IOException {
		this.columns = null;
		if (tableSrc != null) {
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setFeatureType(null);
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setBounds(null);
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setCount( -1 );
		}
		super.dispose();
	}
	
	@Override
	public void removeSchema(String typeName) throws IOException {
		this.columns = null;
		if (tableSrc != null) {
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setFeatureType(null);
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setBounds(null);
			tableSrc.getEntry().getState(tableSrc.getTransaction()).setCount( -1 );
		}
		super.dispose();
	}
	
	@Override
	protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
		if (tableSrc == null) {
			tableSrc = new QueryFeatureSource(entry, this);
		}
		return tableSrc;
	}

	@Override
	protected List<Name> createTypeNames() throws IOException {
		return Collections.singletonList(new NameImpl(WAYPOINT_TYPE));
	}	

	private List<QueryColumn> getSummaryColumns(AssetSummaryQuery query) throws Exception{
		SummaryQueryResult results = (SummaryQueryResult) ((AssetSummaryQuery)query).getCachedResults();
		List<QueryColumn> columns = new ArrayList<>();
		
		//add a row for the station id or station location id
		GroupByPart rowPart =  (((AssetSummaryQuery) query).getQueryDefinition().getRowGroupByPart());
		if (rowPart.getGroupBys().size() != 1) {
			throw new Exception("Cannot create map layer for asset summary query that does not have a single column group by that is station or location");
		}
		IGroupBy gb = rowPart.getGroupBys().get(0);
		if (!(gb instanceof AssetGroupBy)) {
			throw new Exception("Cannot create map layer for asset summary query that does not have a single column group by that is station or location");
		}
		AssetGroupBy assetGp = (AssetGroupBy)gb;
		if (assetGp.getOption() == AssetFilterOption.STATION) {
			columns.add(new EmptyQueryColumn("Station ID", "assetstationid", ColumnType.STRING));
		}else if (assetGp.getOption() == AssetFilterOption.STATIONLOCATION) {
			columns.add(new EmptyQueryColumn("Location ID", "assetlocationid", ColumnType.STRING));
		}else {
			throw new Exception("Cannot create map layer for asset summary query that does not have a single column group by that is station or location");
		}
		
		// [ [TEAM_1, TEAM_1, TEAM_1, TEAM_2, TEAM_2, TEAM_2].
		//  [SA, SB, SC, SA, SB, SC]
		//  [V1, V1, V1, V1, V1, V1] ]
		SummaryHeader[][] headers = results.getColumnHeaderValues();
		
		int length = headers[0].length;
		for (int i = 0; i < length; i ++) {
			StringBuilder sb = new StringBuilder();
			StringBuilder sbkey = new StringBuilder();
			for (SummaryHeader[] items : headers) {
				sb.append(items[i].getFullName());
				sb.append("_");
				sbkey.append(items[i].getKey());
				sbkey.append("_");
			}
			
			columns.add(new EmptyQueryColumn(sb.toString(), sbkey.toString(), ColumnType.NUMBER ));
		}
		return columns;
	}
	
	private class EmptyQueryColumn extends QueryColumn{
		public EmptyQueryColumn(String name, String key, ColumnType type) {
			super(name, key, type);
		}
		@Override
		public Object getValue(IResultItem item) {
			return null;
		}
		
		@Override
		public QueryColumn clone() { return null; }
		
	}
}
