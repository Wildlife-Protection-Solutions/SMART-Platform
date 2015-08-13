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
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlGridEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlObservationEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlSummaryEngine;
import org.wcs.smart.connect.query.engine.patrol.PsqlWaypointEngine;
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
		new PsqlObservationEngine(),
		new PsqlWaypointEngine(),
		new PsqlGridEngine(),
		new PsqlSummaryEngine()
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
	
	//4c4facd2-50bc-4533-8b30-26dc84828e61
	public Query findQuery(UUID uuid, Session session){
		for (Class<?> table : queryClasses){
			Query q = (Query) session.get(table, uuid);
			if (q != null){
				return q;
			}
		}
		return null;
	}
	
	public List<QueryProxy> getQueries(Session session, final Locale l){
		List<QueryProxy> proxies = new ArrayList<QueryProxy>();
		for (Class<? extends Query> q : queryClasses){
			List<Query> queries = session.createCriteria(q).list();
			for (Query qq : queries){
				QueryProxy proxy = new QueryProxy(qq.getUuid(), qq.getName(), q.getSimpleName(), qq.getConservationArea().getId(), qq.getId());
				proxies.add(proxy);
			}
		}
		Collections.sort(proxies, new Comparator<QueryProxy>() {

			@Override
			public int compare(QueryProxy o1, QueryProxy o2) {
				Collator textCompare = Collator.getInstance(l);
				int r = textCompare.compare(o1.getConservationArea(), o2.getConservationArea());
				if (r != 0) return r;
				r = textCompare.compare(o1.getType(), o2.getType());
				if (r != 0) return r;
				r = textCompare.compare(o1.getName(), o2.getName());
				return r;
			}
		});
		return proxies;
	}
	
	public AbstractQueryEngine findQueryEngine(Query query) throws InstantiationException, IllegalAccessException{
		for (AbstractQueryEngine e : engines){
			if (e.canExecute(query.getTypeKey())){
				return e.getClass().newInstance();
			}
		}
		return null;
	}
	
	public IDateFieldFilter findDateField(String key){
		for (IDateFieldFilter field : dateFields){
			if (field.getKey().equalsIgnoreCase(key)){
				return field;
			}
		}
		return null;
	}
	
	public int getCategoryDepth(Session session, UUID caUuid) throws SQLException{
		org.hibernate.Query q = session.createQuery("SELECT max(length(hkey) - length(replace(hkey, '.', ''))) FROM Category WHERE conservationArea.uuid = :cauuid");
		q.setParameter("cauuid", caUuid);
		return ((Integer)q.uniqueResult());
//		PreparedStatement ps = c.prepareStatement("SELECT max(length(hkey) - length(replace(hkey, '.', ''))) FROM Category WHERE ca_uuid = ?");
//		ps.setObject(1, caUuid);
//		ResultSet rs = ps.executeQuery();
//		if(rs.next()){
//			return rs.getInt(1);
//		}
//		return 0;
	}
}
