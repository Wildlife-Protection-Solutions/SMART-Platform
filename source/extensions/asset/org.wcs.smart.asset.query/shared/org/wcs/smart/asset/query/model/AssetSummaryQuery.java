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
package org.wcs.smart.asset.query.model;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn.FixedColumns;
import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.IStyledQuery;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="asset_summary_query", schema="smart")
public class AssetSummaryQuery extends SummaryQuery implements IStyledQuery {

	private static final long serialVersionUID = 1L;
	
	public static final String ASSET_SUMMARY_KEY = "assetsummary"; //$NON-NLS-1$
	public static final String DEPLOYMENT_SUMMARY_KEY = "assetdeploymentsummary"; //$NON-NLS-1$
	
	private String styleMemento;
	private String querytype;
	
	/**
	 * The string representation of the layer style
	 * @return
	 */
	@Column(name="style")
	public String getStyle(){
		return this.styleMemento;
	}
	
	/**
	 * Sets the string representation of the layer style
	 * @param style
	 */
	public void setStyle(String style){
		this.styleMemento = style;
	}
	/**
	 * Parse the string format of the query
	 * into the filter format.
	 * @return 
	 */
	@Transient
	protected SumQueryDefinition parseQuery() throws Exception {
		
		if (getQuery() == null || getQuery().length() == 0){
			return null;
		}
		try(Reader is = new StringReader(getQuery())){
			Parser parser = new Parser(is);
			SumQueryDefinition myQuery = parser.SumQuery();
			return myQuery;
		}
	}


	/**
	 * @see org.wcs.smart.query.model.Query#getType()
	 */
	@Override
	@Column(name="query_type_key")
	public String getTypeKey() {
		return this.querytype;
	}
	
	public void setTypeKey(String querytype) {
		this.querytype = querytype.toLowerCase(Locale.ROOT);
	}
	
	/**
	 * Creates a copy of the summary query
	 * with a null uuid, and null id;
	 * 
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Transient
	public AssetSummaryQuery clone(Employee newOwner){
		AssetSummaryQuery q = new AssetSummaryQuery();
		q.setUuid(null);
		q.setId( null );
		q.setName(getName());
		q.setConservationArea(getConservationArea());
		q.setConservationAreaFilter(getConservationAreaFilter());
		q.setDateFilter(getDateFilter());
		q.setOwner(newOwner);
		q.setQuery(getQuery());
		q.setTypeKey(getTypeKey());
		return q;
	}
	
	public static final boolean canAddGeometry(SumQueryDefinition definition) {
		if (definition.getRowGroupByPart().getGroupBys().size() != 1) return false;
			
		IGroupBy gb = definition.getRowGroupByPart().getGroupBys().get(0);
		if (!(gb instanceof AssetGroupBy)) return false;
		AssetGroupBy asset = (AssetGroupBy)gb;
		if (asset.getOption() == AssetFilterOption.STATION) return true;
		if (asset.getOption() == AssetFilterOption.STATIONLOCATION) return true;
		
		return false;
	}
	
	public static boolean isAssetSummary(String typeKey) {
		return typeKey.equalsIgnoreCase(ASSET_SUMMARY_KEY) || 
				typeKey.equalsIgnoreCase(DEPLOYMENT_SUMMARY_KEY);
	}

	@Override
	public List<QueryColumn> computeQueryColumns(Locale l, Session session, IProjectionProvider prjProvider) {
			SummaryQueryResult results = (SummaryQueryResult)getCachedResults();
			List<QueryColumn> columns = new ArrayList<>();
			
			//add a row for the station id or station location id
			try {
				if (!AssetSummaryQuery.canAddGeometry(getQueryDefinition())) {
					throw new Exception("Cannot create map layer for field sensor summary query that does not have a single column group by that is station or location");
				}
				AssetGroupBy assetGp = (AssetGroupBy)getQueryDefinition().getRowGroupByPart().getGroupBys().get(0);
				if (assetGp.getOption() == AssetFilterOption.STATION) {
					columns.add(new EmptyQueryColumn(
							(new FixedQueryColumn(FixedColumns.STATION, l).getName()),
							"assetstationid", ColumnType.STRING));  //$NON-NLS-1$
				}else if (assetGp.getOption() == AssetFilterOption.STATIONLOCATION) {
					columns.add(new EmptyQueryColumn(
							(new FixedQueryColumn(FixedColumns.LOCATION, l).getName()),
							"assetlocationid", ColumnType.STRING));  //$NON-NLS-1$
				}else {
					throw new Exception("Cannot create map layer for field sensor summary query that does not have a single column group by that is station or location");
				}
			}catch (Exception ex) {
				throw new RuntimeException(ex);
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
					sb.append("_"); //$NON-NLS-1$
					sbkey.append(items[i].getKey());
					sbkey.append("_"); //$NON-NLS-1$
				}
				
				columns.add(new EmptyQueryColumn(sb.toString(), sbkey.toString(), ColumnType.NUMBER ));
			}
			
			columns.add(new PointGeometryQueryColumn(l));
			
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
		public QueryColumn clone() {
			return new EmptyQueryColumn(getName(), getKey(), getType());
		}
		
	}
	
	

}
