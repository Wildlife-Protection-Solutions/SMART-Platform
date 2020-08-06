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

import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.types.AssetObservationQueryType;
import org.wcs.smart.asset.query.model.types.AssetWaypointQueryType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;


/**
 * Factory class for creating new queries
 * @author egouge
 *
 */
public class AssetQueryFactory {

	private static void initQuery(Query q, String defaultName){
		if (defaultName == null){
			defaultName = Messages.Query_DefaultQueryName;
		}
		q.setConservationArea(SmartDB.getCurrentConservationArea());
		q.setOwner(SmartDB.getCurrentEmployee());
		q.setFolder(null);
		q.setIsShared(false);
		
		q.updateName(SmartDB.getCurrentLanguage(), defaultName);
		q.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), defaultName);
		q.setName(defaultName);
		
	}
	
	/**
	 * Creates a new query with none of the query fields initialized.
	 * @param querytype
	 * @return
	 */
	public static Query createBlankQuery(IQueryType querytype){
		if (querytype.getClass().equals(AssetObservationQueryType.class)){
			return new AssetObservationQuery();
		}else if (querytype.getClass().equals(AssetWaypointQueryType.class)){
			return new AssetWaypointQuery();
		}else if (AssetSummaryQuery.isAssetSummary(querytype.getKey())) {
			AssetSummaryQuery query = new AssetSummaryQuery();
			query.setTypeKey(querytype.getKey());
			return query;
		
		}
		return null;
	}
	
	/**
	 * Creates a new query with the various known (conservation area etc)
	 *  query fields initialized.
	 * 
	 * @param querytype
	 * @return
	 */
	public static Query createQuery(IQueryType querytype){
		if (querytype.getClass().equals(AssetObservationQueryType.class)){
			return createObservationQuery();
		}else if (querytype.getClass().equals(AssetWaypointQueryType.class)){
			return createWaypointQuery();
		}else if (AssetSummaryQuery.isAssetSummary(querytype.getKey()) ){
			return createSummaryQuery(querytype.getKey());
		}
		return null;
	}
	
	public static AssetObservationQuery createObservationQuery(){
		AssetObservationQuery query = new AssetObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString() );
		return query;
	}
	
	public static AssetWaypointQuery createWaypointQuery(){
		AssetWaypointQuery query = new AssetWaypointQuery();
		initQuery(query, null);
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString() );
		return query;
	}

	public static AssetSummaryQuery createSummaryQuery(String typeKey){
		AssetSummaryQuery query = new AssetSummaryQuery();
		query.setTypeKey(typeKey);
		initQuery(query, Messages.SummaryQuery_DefaultQueryName);
		
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString() );
		query.setDateFilter(null);
		return query;
	}
}
