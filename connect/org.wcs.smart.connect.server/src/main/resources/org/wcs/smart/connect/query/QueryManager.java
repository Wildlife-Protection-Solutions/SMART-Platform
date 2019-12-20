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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.PostgresUUIDType;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.query.model.AssetObservationQuery;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.query.engine.asset.AssetObservationEngine;
import org.wcs.smart.connect.query.engine.asset.AssetSummaryEngine;
import org.wcs.smart.connect.query.engine.asset.AssetWaypointEngine;
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
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.IQueryEngineFactory;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelRecordSummaryQuery;
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
	
	private static List<Class<? extends Query>> queryClasses = new ArrayList<>();
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
		
		queryClasses.add(AssetObservationQuery.class);
		queryClasses.add(AssetWaypointQuery.class);
		queryClasses.add(AssetSummaryQuery.class);
		
		queryClasses.add(CompoundMapQuery.class);
		
	}
	
	private static List<Class<? extends AbstractIntelQuery >> advQueryClasses = new ArrayList<>();
	static {
		advQueryClasses.add(IntelRecordObservationQuery.class);
		advQueryClasses.add(IntelEntitySummaryQuery.class);
		advQueryClasses.add(IntelEntityRecordQuery.class);
		advQueryClasses.add(IntelRecordQuery.class);
		advQueryClasses.add(IntelRecordSummaryQuery.class);	
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
		new PsqlSummaryIntelligenceQueryEngine(),
		new AssetSummaryEngine(),
		new AssetObservationEngine(),
		new AssetWaypointEngine()
	};
	
	public static IDateFieldFilter[] dateFields = new IDateFieldFilter[]{
		MissionEndDateField.INSTANCE,
		MissionStartDateField.INSTANCE, 
		MissionTrackDateField.INSTANCE, 
		PatrolEndDateField.INSTANCE, 
		PatrolStartDateField.INSTANCE, 
		WaypointDateField.INSTANCE, 
		ReceivedDateFilter.INSTANCE,
		RecordDateDateField.INSTANCE,
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
		
		DATE_FILTERS.put(IntelRecordObservationQuery.KEY.toLowerCase(Locale.ROOT), new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(IntelRecordQuery.KEY.toLowerCase(Locale.ROOT), new String[]{RecordDateDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(IntelRecordSummaryQuery.KEY.toLowerCase(Locale.ROOT), new String[]{RecordDateDateField.INSTANCE.getKey()});

		DATE_FILTERS.put(AssetObservationQuery.KEY.toLowerCase(Locale.ROOT), new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(AssetWaypointQuery.KEY.toLowerCase(Locale.ROOT), new String[]{WaypointDateField.INSTANCE.getKey()});
		DATE_FILTERS.put(AssetSummaryQuery.KEY.toLowerCase(Locale.ROOT), new String[]{WaypointDateField.INSTANCE.getKey()});
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
	 * Find the intelligence query
	 * @param uuid
	 * @param session
	 * @return
	 */
	public AbstractIntelQuery findIntelQuery(UUID uuid, Session session){
		for (Class<? extends AbstractIntelQuery> q : getAdvIntelQueryTypes()) {
			AbstractIntelQuery query = session.get(q, uuid);
			if (query != null) return query;
		}
		return null;
	}
	
	/**
	 * Find a given query based on the uuid.
	 * @param uuid
	 * @param session
	 * @return
	 */
	public QueryProxy findQueryProxy(UUID uuid, Session session){
		
		for (Class<?> table : queryClasses){
			Query q = (Query) session.get(table, uuid);
			if (q != null){
				return new QueryProxy(q.getUuid(),q.getName(), table.getClass().toString(),q.getConservationArea().getNameLabel(),q.getId(),
						q.getIsShared(), q.getConservationArea().getUuid(), false, q.getTypeKey(), q.getIconName());
			}
		}
		return null;
	}
	

	/*
	 * If no third parameter is given, assume false.
	 */
	public List<QueryProxy> getQueries(Session session, final Locale l) throws Exception{
		return getQueries(session, l, false);
	}
	
	public List<QueryProxy> getQueries(Session session, Locale l, Boolean includeMyQueries) throws Exception{
		List<String> langs = new ArrayList<>();
		langs.add(l.getLanguage());
		if (!l.getCountry().isEmpty()) {
			langs.add(l.getLanguage() + "_" + l.getCountry()); //$NON-NLS-1$
			if (l.getVariant().isEmpty()) langs.add(l.getLanguage() + "_" + l.getCountry() + "_" + l.getVariant()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		HashMap<QueryProxy, String> query2names = new HashMap<>();
		String query = ""; //$NON-NLS-1$
		
		//hql doesn't support UNION
		//so we do some hql->sql conversion here and use
		//native queries instead.
		int cnt = 0;
		HashMap<String, Object> params = new HashMap<>();
		
		for (Class<? extends Query> q : queryClasses){
			
			Constructor<? extends Query> cq = q.getDeclaredConstructor();
			cq.setAccessible(true);
			Query c = cq.newInstance();
			
			String type = q.getSimpleName();
			String typeKey = c.getTypeKey();
			String icon = c.getIconName();
			
			if (!query.isEmpty()) query += " UNION "; //$NON-NLS-1$
			
			String querypart = "SELECT q.uuid, q.id, q.isShared, q.conservationArea.uuid, " //$NON-NLS-1$
				+ "q.conservationArea.id, l.value, z.code, '" + type +"', '" + typeKey + "', '" + icon + "' FROM " + q.getSimpleName()  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			 	+ " as q JOIN Label as l on l.id.element = q.uuid JOIN l.id.language as z WHERE l.id.element = q.uuid and (z.default = true or " //$NON-NLS-1$
			 	+ "z.code in (:langs)) "; //$NON-NLS-1$
	
			QueryTranslatorFactory translatorFactory = session.getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService(QueryTranslatorFactory.class);
			final SessionFactoryImplementor factory = (SessionFactoryImplementor) session.getSessionFactory();
			final QueryTranslator translator = translatorFactory.createQueryTranslator(querypart, querypart, Collections.EMPTY_MAP, factory, null);
			translator.compile(Collections.EMPTY_MAP, false);
			String sql = translator.getSQLString();
			sql = sql.replaceFirst("\\?", ":langs"+ cnt); //$NON-NLS-1$ //$NON-NLS-2$
			params.put("langs" + cnt, langs); //$NON-NLS-1$
			cnt++;
			query += sql;
		}
		
		NativeQuery<?> nq = session.createNativeQuery(query);
		for (Entry<String, Object> param : params.entrySet()) {
			nq.setParameter(param.getKey(),  param.getValue());
		}
		//hack based on the hibernate query conversion; might be invalid in future versions of hibernate
		//required for uuid data types
		nq.addScalar("col_0_0_", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		nq.addScalar("col_1_0_"); //$NON-NLS-1$
		nq.addScalar("col_2_0_"); //$NON-NLS-1$
		nq.addScalar("col_3_0_", PostgresUUIDType.INSTANCE); //$NON-NLS-1$
		nq.addScalar("col_4_0_"); //$NON-NLS-1$
		nq.addScalar("col_5_0_"); //$NON-NLS-1$
		nq.addScalar("col_6_0_"); //$NON-NLS-1$
		nq.addScalar("col_7_0_"); //$NON-NLS-1$
		nq.addScalar("col_8_0_"); //$NON-NLS-1$
		nq.addScalar("col_9_0_"); //$NON-NLS-1$
		List<?> items = nq.list();

		for (Object i : items) {
			Object[] data = (Object[])i;
			UUID uuid = (UUID) data[0];
			String id = (String)data[1];
			boolean isShared = (boolean)data[2];
			
			UUID cauuid = (UUID)data[3];
			String caid = (String)data[4];
			
			String value = (String)data[5];
			String code = (String)data[6];
			
			String type = (String)data[7];
			String typekey = (String)data[8];
			String icon = (String)data[9];
				
			if (isShared || includeMyQueries) {
				QueryProxy qp = new QueryProxy(uuid, value, type, caid, id, isShared,cauuid, cauuid.equals(ConservationArea.MULTIPLE_CA), typekey, icon);
					
				if (!query2names.containsKey(qp)) {
					query2names.put(qp, code);
				}else {
					String currentcode = query2names.get(qp);
					int cindex = langs.indexOf(currentcode);
					int nindex = langs.indexOf(code);
					if ((cindex == -1 && nindex >= 0) || (cindex != -1 && nindex > cindex)) {
						query2names.remove(qp);
						query2names.put(qp, code);
					}
				}
			}
		}

		return new ArrayList<>(query2names.keySet());
	}

	public List<QueryProxy> getAdvanedIntelligenceQueries(Session session, Locale l) throws Exception{
		List<String> langs = new ArrayList<>();
		langs.add(l.getLanguage());
		if (!l.getCountry().isEmpty()) {
			langs.add(l.getLanguage() + "_" + l.getCountry()); //$NON-NLS-1$
			if (l.getVariant().isEmpty()) langs.add(l.getLanguage() + "_" + l.getCountry() + "_" + l.getVariant()); //$NON-NLS-1$ //$NON-NLS-2$

		}
		
		HashMap<QueryProxy, String> query2names = new HashMap<>();
		String query = ""; //$NON-NLS-1$
		
		//hql doesn't support UNION
		//so we do some hql->sql conversion here and use
		//native queries instead.
		int cnt = 0;
		HashMap<String, Object> params = new HashMap<>();
				
		for (Class<? extends AbstractIntelQuery> q : getAdvIntelQueryTypes()){
			
			Constructor<? extends AbstractIntelQuery> cq = q.getDeclaredConstructor();
			cq.setAccessible(true);
			AbstractIntelQuery c = cq.newInstance();
			
			String type = q.getSimpleName();
			String typeKey = c.getTypeKey();
			String icon = c.getIconName();
			
			if (!query.isEmpty()) query += " UNION "; //$NON-NLS-1$
			
			String querypart = "SELECT q.uuid, q.conservationArea.uuid, " //$NON-NLS-1$
				+ "q.conservationArea.id, l.value, z.code, '" + type +"', '" + typeKey + "', '" + icon + "' FROM " + q.getSimpleName()  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			 	+ " as q JOIN Label as l on l.id.element = q.uuid JOIN l.id.language as z WHERE l.id.element = q.uuid and (z.default = true or " //$NON-NLS-1$
			 	+ "z.code in (:langs)) "; //$NON-NLS-1$
	
			QueryTranslatorFactory translatorFactory = session.getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService(QueryTranslatorFactory.class);
			final SessionFactoryImplementor factory = (SessionFactoryImplementor) session.getSessionFactory();
			final QueryTranslator translator = translatorFactory.createQueryTranslator(querypart, querypart, Collections.EMPTY_MAP, factory, null);
			translator.compile(Collections.EMPTY_MAP, false);
			String sql = translator.getSQLString();
			sql = sql.replaceFirst("\\?", ":langs"+ cnt);  //$NON-NLS-1$//$NON-NLS-2$
			params.put("langs" + cnt, langs); //$NON-NLS-1$
			cnt++;
			query += sql;

		}

		NativeQuery<?> nq = session.createNativeQuery(query);
		for (Entry<String, Object> param : params.entrySet()) {
			nq.setParameter(param.getKey(),  param.getValue());
		}
		//hack based on the hibernate query conversion; might be invalid in future versions of hibernate
		//required for uuid data types
		nq.addScalar("col_0_0_", PostgresUUIDType.INSTANCE);  //$NON-NLS-1$
		nq.addScalar("col_1_0_", PostgresUUIDType.INSTANCE);  //$NON-NLS-1$
		nq.addScalar("col_2_0_"); //$NON-NLS-1$
		nq.addScalar("col_3_0_"); //$NON-NLS-1$
		nq.addScalar("col_4_0_"); //$NON-NLS-1$
		nq.addScalar("col_5_0_"); //$NON-NLS-1$
		nq.addScalar("col_6_0_"); //$NON-NLS-1$
		nq.addScalar("col_7_0_"); //$NON-NLS-1$
		List<?> items = nq.list();

		for (Object i : items) {
			Object[] data = (Object[])i;
			UUID uuid = (UUID) data[0];
			
			UUID cauuid = (UUID)data[1];
			String caid = (String)data[2];
			
			String value = (String)data[3];
			String code = (String)data[4];
			
			String type = (String)data[5];
			String typekey = (String)data[6];
			String icon = (String)data[7];
				
			QueryProxy qp = new QueryProxy(uuid, value, type, caid, "-", true, cauuid, cauuid.equals(ConservationArea.MULTIPLE_CA), typekey, icon);  //$NON-NLS-1$
					
			if (!query2names.containsKey(qp)) {
				query2names.put(qp, code);
			}else {
				String currentcode = query2names.get(qp);
				int cindex = langs.indexOf(currentcode);
				int nindex = langs.indexOf(code);
				if (cindex == -1 && nindex >= 0) query2names.put(qp, code);
				if (cindex != -1 && nindex > cindex) query2names.put(qp, code);
			}
		}

		return new ArrayList<>(query2names.keySet());
	}
		
	/**
	 * Find the query engine for running the given query.
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public IQueryEngine findQueryEngine(Query query) throws Exception{
		for (IQueryEngine e : engines){
			if (e.canExecute(query.getTypeKey())){
				IQueryEngine engine = e.getClass().getDeclaredConstructor().newInstance();
				return engine;
			}
		}
		return null;
	}
	
	/**
	 * Find the query engine for intel record observation queries
	 * @param i2query
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public IIntelQueryEngine findQueryEngine(AbstractIntelQuery i2query) throws InstantiationException, IllegalAccessException{
			return IIntelQueryEngine.createEngine(i2query.getTypeKey());
		 
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
		IIntelQueryEngine e = SmartContext.INSTANCE.getClass(IQueryEngineFactory.class).findQueryEngine(queryTypeKey);
		return e != null;
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
	
	public List<Class<? extends AbstractIntelQuery>> getAdvIntelQueryTypes()  {
		return advQueryClasses;
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
		org.hibernate.query.Query<?> q = session.createQuery("Select hkey, length(hkey) - length(replace(hkey, '.', '')) as hkey_length, count(*) FROM  Category WHERE conservationArea.uuid IN (:cauuids) group by hkey having count(*) = :cnt order by length(hkey) - length(replace(hkey, '.', '')) desc"); //$NON-NLS-1$
		q.setParameterList("cauuids", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
		q.setParameter("cnt", Long.valueOf(caFilter.getConservationAreaFilterIds().size())); //$NON-NLS-1$
		q.setMaxResults(1);
		Object[] x = (Object[])q.uniqueResult();
		if (x == null) return 0;
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
			org.hibernate.query.Query<?> delete = s.createQuery("DELETE FROM SmartUserAction WHERE resource IN (SELECT uuid FROM " + q.getSimpleName() + " WHERE conservationArea.uuid = :ca)"); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	/**
	 * Returns the attribute with the given key
	 * @param attributeKey
	 * @param session
	 * @return
	 */

	public Attribute.AttributeType getAttributeType(Session session, String attributeKey, ConservationAreaFilter caFilter){
		if (caFilter.getConservationAreaFilterIds().size() == 1){
			org.hibernate.query.Query<Attribute> q = session.createQuery("From Attribute where conservationArea.uuid = :ca and keyid = :key", Attribute.class); //$NON-NLS-1$
			q.setParameter("ca", caFilter.getConservationAreaFilterIds().get(0)); //$NON-NLS-1$
			q.setParameter("key", attributeKey); //$NON-NLS-1$
			q.setCacheable(true);
			
			List<Attribute> results = q.list();
			if (results.size() != 1 ){
				return null;
			}else{
				return results.get(0).getType();
			}
		}else if (caFilter.getConservationAreaFilterIds().size() == 0){
			//no conservation areas in filter; this should not be valid
			return null;
			
		}else{
			org.hibernate.query.Query<Attribute> q = session.createQuery("From Attribute where conservationArea.uuid in (:cas) and keyid = :key", Attribute.class); //$NON-NLS-1$
			q.setParameterList("cas", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
			q.setParameter("key", attributeKey); //$NON-NLS-1$
			
			List<Attribute> allAttributes = q.list();
			if (allAttributes.size() == 0) return null;
			
			Set<AttributeType> types = allAttributes.stream().map(a->a.getType()).distinct().collect(Collectors.toSet());
			if (types.size() == 1) return types.iterator().next();
			return null;	//not a valid column as the key has different types in different cas (or is not valid in any cas)
		}
	}
}
