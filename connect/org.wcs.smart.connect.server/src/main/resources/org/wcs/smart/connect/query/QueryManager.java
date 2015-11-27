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
package org.wcs.smart.connect.query;

import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityGridEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityObservationEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntitySummaryEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityWaypointEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsGridEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsSummaryEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsWaypointEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsObservationEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolGridEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolObservationEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolSummaryEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolWaypointEngine;
import org.wcs.smart.connect.security.ActionManager;
import org.wcs.smart.connect.security.ISmartConnectAction;
import org.wcs.smart.entity.query.model.EntityGriddedQuery;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntitySummaryQuery;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.MissionTrackDateField;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.SurveyGriddedQuery;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveySummaryQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.intelligence.query.model.RecievedDateFilter;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Query manager for SMART Connect queries.
 * 
 * @author Emily
 *
 */
public enum QueryManager {

	INSTANCE;
	
	private static List<Class<? extends Query>> queryClasses = new ArrayList<Class<? extends Query>>();
	static{
		queryClasses.add(PatrolObservationQuery.class);
		queryClasses.add(PatrolQuery.class);
		queryClasses.add(PatrolWaypointQuery.class);
		queryClasses.add(PatrolSummaryQuery.class);
		queryClasses.add(PatrolGriddedQuery.class);
		
		queryClasses.add(ObsObservationQuery.class);
		queryClasses.add(ObservationWaypointQuery.class);
		queryClasses.add(ObservationSummaryQuery.class);
		queryClasses.add(ObservationGriddedQuery.class);
		
		queryClasses.add(EntityObservationQuery.class);
		queryClasses.add(EntityWaypointQuery.class);
		queryClasses.add(EntitySummaryQuery.class);
		queryClasses.add(EntityGriddedQuery.class);
		
		queryClasses.add(SurveyObservationQuery.class);
		queryClasses.add(SurveyWaypointQuery.class);
		queryClasses.add(SurveySummaryQuery.class);
		queryClasses.add(SurveyGriddedQuery.class);
		queryClasses.add(MissionQuery.class);
		queryClasses.add(MissionTrackQuery.class);
		
	}

	
	private static final AbstractQueryEngine[] engines = new AbstractQueryEngine[]{
		new PsqlPatrolObservationEngine(),
		new PsqlPatrolWaypointEngine(),
		new PsqlPatrolGridEngine(),
		new PsqlPatrolSummaryEngine(),
		new PsqlPatrolEngine(),
		new PsqlObsObservationEngine(),
		new PsqlObsWaypointEngine(),
		new PsqlObsSummaryEngine(),
		new PsqlObsGridEngine(),
		new PsqlEntityGridEngine(),
		new PsqlEntityObservationEngine(),
		new PsqlEntitySummaryEngine(),
		new PsqlEntityWaypointEngine()
	};
	
	private static IDateFieldFilter[] dateFields = new IDateFieldFilter[]{
		MissionEndDateField.INSTANCE,
		MissionStartDateField.INSTANCE, 
		MissionTrackDateField.INSTANCE, 
		PatrolEndDateField.INSTANCE, 
		PatrolStartDateField.INSTANCE, 
		WaypointDateField.INSTANCE, 
		RecievedDateFilter.INSTANCE
	};
	
	/**
	 * Find a given query based on the uuid.
	 * @param uuid
	 * @param session
	 * @return
	 */
	public Query findQuery(UUID uuid, Session session){
		for (Class<?> table : queryClasses){
			Query q = (Query) session.get(table, uuid);
			if (q != null){
				return q;
			}
		}
		return null;
	}
	
	/**
	 * Lists all queries.
	 * 
	 * @param session
	 * @param l
	 * @param includeMyQueries  - by default we don't include shared queries, i.e. "My Queries" as they should not be shown most of the time. 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<QueryProxy> getQueries(Session session, final Locale l, Boolean includeMyQueries){
		List<QueryProxy> proxies = new ArrayList<QueryProxy>();
		for (Class<? extends Query> q : queryClasses){
			List<Query> queries = session.createCriteria(q).list();
			for (Query qq : queries){
				QueryProxy proxy = new QueryProxy(qq.getUuid(), qq.getName(), q.getSimpleName(), qq.getConservationArea().getId(), qq.getId(), qq.getIsShared(), qq.getConservationArea().getUuid());
				if(qq.getIsShared() || includeMyQueries){
					proxies.add(proxy);
				}
			}
		}
		Collections.sort(proxies, new Comparator<QueryProxy>() {

			@Override
			public int compare(QueryProxy o1, QueryProxy o2) {
				Collator textCompare = Collator.getInstance(l);
//This is the method to sort by CA, then type, then name.
//				int r = textCompare.compare(o1.getConservationArea(), o2.getConservationArea());
//				if (r != 0) return r;
//				r = textCompare.compare(o1.getType(), o2.getType());
//				if (r != 0) return r;
//				r = textCompare.compare(o1.getName(), o2.getName());
//				return r;
				
				//I want to sort by Name only for the user's security page. 
				return textCompare.compare(o1.getName(), o2.getName());
			}
		});
		return proxies;
	}
	
	/*
	 * If no third parameter is given, assume false.
	 */
	public List<QueryProxy> getQueries(Session session, final Locale l){
		return getQueries(session, l, false);
	}
	
	/**
	 * Find the query engine for running the given query.
	 * 
	 * @param query
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public AbstractQueryEngine findQueryEngine(Query query) throws InstantiationException, IllegalAccessException{
		for (AbstractQueryEngine e : engines){
			if (e.canExecute(query.getTypeKey())){
				return e.getClass().newInstance();
			}
		}
		return null;
	}
	
	/**
	 * Finds all valid date field filters.
	 * @param key
	 * @return
	 */
	public IDateFieldFilter findDateField(String key){
		for (IDateFieldFilter field : dateFields){
			if (field.getKey().equalsIgnoreCase(key)){
				return field;
			}
		}
		return null;
	}
	
	/**
	 * Determines the maximum category depth for a given conservation area.
	 * abc. = 1
	 * abc.def. = 2
	 * 
	 * @param session
	 * @param caUuid
	 * @return
	 * @throws SQLException
	 */
	public int getCategoryDepth(Session session, UUID caUuid) throws SQLException{
		org.hibernate.Query q = session.createQuery("SELECT max(length(hkey) - length(replace(hkey, '.', ''))) FROM Category WHERE conservationArea.uuid = :cauuid");
		q.setParameter("cauuid", caUuid);
		return ((Integer)q.uniqueResult() + 1);
	}

	public void RemoveAccessToQueriesFromCa(UUID uuid, Session s, Locale locale) {
		List<UUID> list = null;
		for (Class<? extends Query> q : queryClasses){
			list = s.createCriteria(q).setProjection(Projections.property("uuid")).list();
			if(list != null && list.size() > 0){
				org.hibernate.Query q1 = s.createQuery("DELETE FROM SmartUserAction WHERE resource in (:list)");
				q1.setParameterList("list", list);
				q1.executeUpdate();
			}
		}
	}
}
