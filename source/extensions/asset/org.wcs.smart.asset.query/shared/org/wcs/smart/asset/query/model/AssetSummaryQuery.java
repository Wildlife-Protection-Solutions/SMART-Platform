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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.model.IStyledQuery;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.SumQueryDefinition;

/**
 * A class to represent a summary query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.asset_summary_query")
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
		this.querytype = querytype;
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
}
