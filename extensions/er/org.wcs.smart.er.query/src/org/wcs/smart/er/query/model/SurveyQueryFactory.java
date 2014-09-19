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
		}else if (querytype.getClass().equals(SurveyGridQueryType.class)){
			return new SurveyGriddedQuery();
		}else if (querytype.getClass().equals(SurveySummaryQueryType.class)){
			return new SurveySummaryQuery();
		}else if (querytype.getClass().equals(SurveyWaypointQueryType.class)){
			return new SurveyWaypointQuery();
		}else if (querytype.getClass().equals(MissionQueryType.class)){
			return new MissionQuery();
		}else if (querytype.getClass().equals(MissionTrackQueryType.class)){
			return new MissionTrackQuery();
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
		}else if (querytype.getClass().equals(SurveyGridQueryType.class)){
			return createGriddedQuery();
		}else if (querytype.getClass().equals(MissionQueryType.class)){
			return createMissionQuery();
		}else if (querytype.getClass().equals(SurveySummaryQueryType.class)){
			return createSummaryQuery();
		}else if (querytype.getClass().equals(SurveyWaypointQueryType.class)){
			return createSurveyWaypointQuery();
		}else if (querytype.getClass().equals(MissionTrackQueryType.class)){
			return createMissionTrackQuery();
		}
		return null;
	}
	
	public static SurveyObservationQuery createObservationQuery(){
		SurveyObservationQuery query = new SurveyObservationQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		
		
		return query;
	}
	
	public static SurveyWaypointQuery createSurveyWaypointQuery(){
		SurveyWaypointQuery query = new SurveyWaypointQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		return query;
	}
	
	public static SurveyGriddedQuery createGriddedQuery(){
		SurveyGriddedQuery query = new SurveyGriddedQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
	
	public static MissionQuery createMissionQuery(){
		MissionQuery query = new MissionQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
	
	public static SurveySummaryQuery createSummaryQuery(){
		SurveySummaryQuery query = new SurveySummaryQuery();
		initQuery(query, null);
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
	
	public static MissionTrackQuery createMissionTrackQuery(){
		MissionTrackQuery query = new MissionTrackQuery();
		initQuery(query, null);		
		query.setConservationAreaFilter(new ConservationAreaFilter(true));
		query.setDateFilter(null);
		return query;
	}
}
