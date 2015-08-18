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
package org.wcs.smart.observation.query.model;

import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.types.ObservationGridQueryType;
import org.wcs.smart.observation.query.model.types.ObservationQueryType;
import org.wcs.smart.observation.query.model.types.ObservationSummaryQueryType;
import org.wcs.smart.observation.query.model.types.ObservationWaypointQueryType;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;


/**
 * Factory class for creating new queries
 * @author egouge
 *
 */
public class ObservationQueryFactory {

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
		if (querytype.getClass().equals(ObservationQueryType.class)){
			return new ObsObservationQuery();
		}else if (querytype.getClass().equals(ObservationGridQueryType.class)){
			return new ObservationGriddedQuery();
		}else if (querytype.getClass().equals(ObservationWaypointQueryType.class)){
			return new ObservationWaypointQuery();
		}else if (querytype.getClass().equals(ObservationSummaryQueryType.class)){
			return new ObservationSummaryQuery();
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
		if (querytype.getClass().equals(ObservationQueryType.class)){
			return createObservationQuery();
		}else if (querytype.getClass().equals(ObservationGridQueryType.class)){
			return createGriddedQuery();
		}else if (querytype.getClass().equals(ObservationWaypointQueryType.class)){
			return createWaypointQuery();
		}else if (querytype.getClass().equals(ObservationSummaryQueryType.class)){
			createSummaryQuery();
		}
		return null;
	}
	
	public static ObsObservationQuery createObservationQuery(){
		ObsObservationQuery query = new ObsObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		return query;
	}
	
	public static ObservationWaypointQuery createWaypointQuery(){
		ObservationWaypointQuery query = new ObservationWaypointQuery();
		initQuery(query, null);
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		return query;
	}
	
		
	public static ObservationGriddedQuery createGriddedQuery(){
		ObservationGriddedQuery query = new ObservationGriddedQuery();
		initQuery(query, Messages.GriddedQuery_DefaultQueryName);
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		query.setDateFilter(null);
		return query;
	}
	
	public static ObservationSummaryQuery createSummaryQuery(){
		ObservationSummaryQuery query = new ObservationSummaryQuery();
		initQuery(query, Messages.SummaryQuery_DefaultQueryName);
		
		query.setConservationAreaFilter( (new ConservationAreaFilter(true, SmartDB.getCurrentConservationArea())).asString());
		query.setDateFilter(null);
		return query;
	}
}
