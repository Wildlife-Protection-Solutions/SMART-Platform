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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.internal.util.ReflectHelper;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityGridEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityObservationEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntitySummaryEngine;
import org.wcs.smart.connect.query.engine.entity.PsqlEntityWaypointEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErGridEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErMissionEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErMissionTrackEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErObservationEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErSummaryEngine;
import org.wcs.smart.connect.query.engine.er.PsqlErWaypointEngine;
import org.wcs.smart.connect.query.engine.intelligence.PsqlRecordQueryIntelligenceEngine;
import org.wcs.smart.connect.query.engine.intelligence.PsqlSummaryIntelligenceQueryEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsGridEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsObservationEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsSummaryEngine;
import org.wcs.smart.connect.query.engine.observation.PsqlObsWaypointEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolGridEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolObservationEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolSummaryEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlPatrolWaypointEngine;
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
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceSummaryQuery;
import org.wcs.smart.intelligence.query.model.ReceivedDateFilter;
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
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
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
		
		queryClasses.add(IntelligenceRecordQuery.class);
		queryClasses.add(IntelligenceSummaryQuery.class);
		
		queryClasses.add(CompoundMapQuery.class);
	}
	
	private static final IQueryEngine[] engines = new IQueryEngine[]{
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
		new PsqlEntityWaypointEngine(),
		new PsqlErGridEngine(),
		new PsqlErMissionEngine(),
		new PsqlErMissionTrackEngine(),
		new PsqlErObservationEngine(),
		new PsqlErSummaryEngine(),
		new PsqlErWaypointEngine(),
		new PsqlRecordQueryIntelligenceEngine(),
		new PsqlSummaryIntelligenceQueryEngine()
	};
	
	public static IDateFieldFilter[] dateFields = new IDateFieldFilter[]{
		MissionEndDateField.INSTANCE,
		MissionStartDateField.INSTANCE, 
		MissionTrackDateField.INSTANCE, 
		PatrolEndDateField.INSTANCE, 
		PatrolStartDateField.INSTANCE, 
		WaypointDateField.INSTANCE, 
		ReceivedDateFilter.INSTANCE
	};
	
	public static HashMap<String, String[]> DATE_FILTERS = new HashMap<String, String[]>();
	static{
		DATE_FILTERS.put(PatrolGriddedQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),PatrolStartDateField.INSTANCE.getKey(),PatrolEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(PatrolObservationQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),PatrolStartDateField.INSTANCE.getKey(),PatrolEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(PatrolSummaryQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),PatrolStartDateField.INSTANCE.getKey(),PatrolEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(PatrolWaypointQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),PatrolStartDateField.INSTANCE.getKey(),PatrolEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(PatrolQuery.KEY, new String[]{PatrolStartDateField.INSTANCE.getKey(),PatrolEndDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(SurveyGriddedQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(SurveyObservationQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(SurveySummaryQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(SurveyWaypointQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey(),MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(MissionQuery.KEY, new String[]{MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(MissionTrackQuery.KEY, new String[]{MissionTrackDateField.INSTANCE.getKey(),MissionStartDateField.INSTANCE.getKey(),MissionEndDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(ObservationGriddedQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(ObsObservationQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(ObservationSummaryQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(ObservationWaypointQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(EntityGriddedQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(EntityObservationQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(EntitySummaryQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(EntityWaypointQuery.KEY, new String[]{WaypointDateField.INSTANCE.getKey()});
		
		DATE_FILTERS.put(IntelligenceRecordQuery.KEY, new String[]{ReceivedDateFilter.INSTANCE.getKey()});
		DATE_FILTERS.put(IntelligenceSummaryQuery.KEY, new String[]{ReceivedDateFilter.INSTANCE.getKey()});			
	}
	
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
				QueryProxy proxy = new QueryProxy(qq.getUuid(), qq.getName(), q.getSimpleName(), 
						qq.getConservationArea().getId(), qq.getId(), qq.getIsShared(), 
						qq.getConservationArea().getUuid(),
						qq.getConservationArea().getIsCcaa(),
						qq.getTypeKey());
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
	public IQueryEngine findQueryEngine(Query query) throws InstantiationException, IllegalAccessException{
		for (IQueryEngine e : engines){
			if (e.canExecute(query.getTypeKey())){
				return e.getClass().newInstance();
			}
		}
		return null;
	}
	
	/**
	 *Determines if the given query type has an associated query engine
	 * 
	 * @return true if the given query type has an associated query
	 * engine
	 */
	public boolean hasQueryEngine(String queryTypeKey){
		for (IQueryEngine e : engines){
			if (e.canExecute(queryTypeKey)) return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return array of all supported query type keys
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public String[] getQueryTypes() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String[] types = new String[queryClasses.size()];
		for (int i = 0; i < queryClasses.size(); i ++){
			Constructor<?> c = ReflectHelper.getDefaultConstructor(queryClasses.get(i));
			Query q = (Query)c.newInstance( );
			types[i] = q.getTypeKey();
		}
		return types;
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
	 * Determines the maximum category depth for a set of conservation areas
	 * abc. = 1
	 * abc.def. = 2
	 * 
	 * @param session
	 * @param caUuid
	 * @return
	 * @throws SQLException
	 */
	public int getCategoryDepth(Session session, ConservationAreaFilter caFilter) throws SQLException{
		org.hibernate.Query q = session.createQuery("Select hkey, length(hkey) - length(replace(hkey, '.', '')) as hkey_length, count(*) FROM  Category WHERE conservationArea.uuid IN (:cauuids) group by hkey having count(*) = :cnt order by length(hkey) - length(replace(hkey, '.', '')) desc"); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		q.setParameter("cnt", new Long(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
		q.setMaxResults(1);
		Object[] x = (Object[])q.uniqueResult();
		return (Integer)x[1];
	}

	/**
	 * Deletes from the smartuseraction table and record that
	 * references a query in the given Conservation Area.
	 * 
	 * @param caUuid
	 * @param s
	 * @throws SQLException
	 */
	public void removeAccessToQueriesFromCa(UUID caUuid, Session s) throws SQLException{
		for (Class<? extends Query> q : queryClasses){
			org.hibernate.Query delete = s.createQuery("DELETE FROM SmartUserAction WHERE resource IN (SELECT uuid FROM " + q.getSimpleName() + " WHERE conservationArea.uuid = :ca)"); //$NON-NLS-1$ //$NON-NLS-2$
			delete.setParameter("ca", caUuid); //$NON-NLS-1$
			delete.executeUpdate();
		}
	}
	
	/**
	 * Determines if a given query type can support a given date field.
	 * 
	 * @param queryTypeKey
	 * @param field
	 * @return
	 */
	public boolean supportsDateField(String queryTypeKey, IDateFieldFilter field){
		String[] ops = DATE_FILTERS.get(queryTypeKey);
		if (ops == null) return false;
		return stringIn(field.getKey(), ops);
	}
	
	private boolean stringIn(String key, String... in1){
		for (String item : in1){
			if (item.equalsIgnoreCase(key)){
				return true;
			}
		}
		return false;
	}

	public SharedLink findSharedLink(UUID uuid, Session s) {
		SharedLink q = (SharedLink) s.get(SharedLink.class, uuid);
		return q;
	}

	public SmartUser findUser(UUID uuid, Session s) {
		return (SmartUser) s.get(SmartUser.class, uuid);
	}
}
