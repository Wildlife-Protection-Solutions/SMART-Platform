/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.columns;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.er.query.model.ISurveyQueryColumnProvider;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Query column implementation for survey queries.
 * 
 * @author Emily
 *
 */
public class SurveyQueryColumnProvider implements ISurveyQueryColumnProvider {

	@Override
	public QueryColumn[] getQueryColumns(Query query, Locale l, Session session) {
		return null;
//		String queryTypeKey = query.getTypeKey();
//		String surveyDesignKey = ((ISurveyQuery)query).getSurveyDesign();
//		
//		if (queryTypeKey.equals(SurveyObservationQuery.KEY)){
//			SurveyQueryColumnManager.getInstance()
//				.getObservationQueryColumns(
//						getSurveyDesignAsObject(surveyDesignKey));
//		}else if (queryTypeKey.equals(SurveyWaypointQuery.KEY)){
//			SurveyQueryColumnManager.getInstance()
//			.getWaypointQueryColumns(
//					getSurveyDesignAsObject(surveyDesignKey));
//		}else if (queryTypeKey.equals(SurveyGriddedQuery.KEY)){
//			SurveyQueryColumnManager.getInstance()
//			.getGridColumns();
//		}else if (queryTypeKey.equals(MissionQuery.KEY)){
//			SurveyQueryColumnManager.getInstance()
//			.getMissionQueryColumns(
//					getSurveyDesignAsObject(surveyDesignKey));
//		}else if (queryTypeKey.equals(MissionTrackQuery.KEY)){
//			SurveyQueryColumnManager.getInstance()
//			.getMissionTrackQueryColumns(
//					getSurveyDesignAsObject(surveyDesignKey));
//		}
//		return null;
	}
}
