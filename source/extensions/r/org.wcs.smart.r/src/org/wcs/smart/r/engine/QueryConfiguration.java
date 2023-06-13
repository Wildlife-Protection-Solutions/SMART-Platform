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
package org.wcs.smart.r.engine;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.util.UuidUtils;

/**
 * Query configuration for r script
 * 
 * @author Emily
 *
 */
public class QueryConfiguration {

	private Query query;
	private IQueryExporter exporter;
	private DateFilter dateFilter;
	
	public QueryConfiguration(Query query, IQueryExporter exporter, DateFilter dateFilter) {
		this.query = query;
		this.exporter = exporter;
		this.dateFilter = dateFilter;
	}
	
	public Query getQuery() {
		return this.query;
	}
	
	public IQueryExporter getQueryExporter() {
		return this.exporter;
	}
	
	public DateFilter getDateFilter() {
		return this.dateFilter;
	}
	
	/**
	 * Converts to configuration string to be stored with query
	 * 
	 * @param param
	 * @param queries
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String toConfigurationString(String param, List<QueryConfiguration> queries) {
		JSONObject items = new JSONObject();
		items.put(RQuery.PARAM_JSONKEY, param);
		
		JSONArray array = new JSONArray();
		for (QueryConfiguration cc : queries) {
			JSONObject jquery = new JSONObject();
			
			jquery.put(RQuery.QTYPE_JSONKEY, cc.getQuery().getTypeKey());
			jquery.put(RQuery.QUUID_JSONKEY, UuidUtils.uuidToString(cc.getQuery().getUuid()));
			jquery.put(RQuery.QEXPORT_JSONKEY, cc.getQueryExporter().getId());
			jquery.put(RQuery.QDATE_JSON_KEY, cc.getDateFilter().asString());
			
			array.add(jquery);
		}
		items.put(RQuery.QUERY_JSONKEY,array);
		return items.toJSONString();
	}
}
