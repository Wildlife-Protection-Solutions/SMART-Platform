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
package org.wcs.smart.er.query.model;

import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;


/**
 * Factory class for creating new survey queries.
 * 
 * @author egouge
 *
 */
public class SurveyQueryFactory {

	private static void initQuery(Query q, String defaultName){
		if (defaultName == null){
			defaultName = Messages.SurveyQueryFactory_DefaultQueryName;
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
		if (querytype.getClass().equals(SurveyObservationQueryType.class)){
			return new SurveyObservationQuery();
//		}else if (querytype.getClass().equals(PatrolGridQueryType.class)){
//			return new PatrolGriddedQuery();
//		}else if (querytype.getClass().equals(PatrolQueryType.class)){
//			return new PatrolQuery();
//		}else if (querytype.getClass().equals(PatrolWaypointQueryType.class)){
//			return new PatrolWaypointQuery();
//		}else if (querytype.getClass().equals(PatrolSummaryQueryType.class)){
//			return new PatrolSummaryQuery();
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
		if (querytype.getClass().equals(SurveyObservationQueryType.class)){
			return createObservationQuery();
//		}else if (querytype.getClass().equals(PatrolGridQueryType.class)){
//			return createGriddedQuery();
//		}else if (querytype.getClass().equals(PatrolQueryType.class)){
//			return createPatrolQuery();
//		}else if (querytype.getClass().equals(PatrolWaypointQueryType.class)){
//			return createWaypointQuery();
//		}else if (querytype.getClass().equals(PatrolSummaryQueryType.class)){
//			return createSummaryQuery();
		}
		return null;
	}
	
	public static SurveyObservationQuery createObservationQuery(){
		SurveyObservationQuery query = new SurveyObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		
		
		return query;
	}
	
//	public static PatrolWaypointQuery createWaypointQuery(){
//		PatrolWaypointQuery query = new PatrolWaypointQuery();
//		initQuery(query, null);
//		query.setConservationAreaFilter(new ConservationAreaFilter(true));
//		return query;
//	}
//	
//	public static PatrolQuery createPatrolQuery(){
//		PatrolQuery query = new PatrolQuery();
//		initQuery(query, null);
//		query.setConservationAreaFilter(new ConservationAreaFilter(true));
//		return query;
//	}
//	
//	
//	public static PatrolGriddedQuery createGriddedQuery(){
//		PatrolGriddedQuery query = new PatrolGriddedQuery();
//		initQuery(query, Messages.GriddedQuery_DefaultQueryName);
//		query.setConservationAreaFilter(new ConservationAreaFilter(true));
//		query.setDateFilter(null);
//		return query;
//	}
//	
//	public static PatrolSummaryQuery createSummaryQuery(){
//		PatrolSummaryQuery query = new PatrolSummaryQuery();
//		initQuery(query, Messages.SummaryQuery_DefaultQueryName);
//		
//		query.setConservationAreaFilter(new ConservationAreaFilter(true));
//		query.setDateFilter(null);
//		return query;
//	}
}
