package org.wcs.smart.shared.query.columns;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.er.query.model.ISurveyQueryColumnProvider;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;

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
