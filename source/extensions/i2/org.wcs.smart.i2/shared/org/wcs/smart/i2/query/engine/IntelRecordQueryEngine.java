package org.wcs.smart.i2.query.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.util.UuidUtils;

/*
 * drop table test1;
create table test1 (location_uuid char(16) for bit data, observation_uuid char(16) for bit data)
insert into test1 select l.uuid, o.uuid from smart.I_location l join smart.i_observation o on l.uuid = o.location_uuid

alter table test1 add column c1 boolean ;

alter table test1 add column c2 boolean ;

update test1 set c1 = true where location_uuid in (select location_uuid from smart.i_entity_location where entity_uuid = x'0758ade7af094d71bda1cd42b36924c1');

update test1 set c2 = true where location_uuid in (select location_uuid from smart.i_entity_location l join smart.i_entity e on l.entity_uuid = e.uuid join smart.i_entity_type t on e.entity_type_uuid = t.uuid and t.keyId = 'type2');

select * from test1;
 */
public class IntelRecordQueryEngine {

	
	/**
	 * parameters requires a session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(IntelRecordQuery query,  HashMap<String, Object> parameters) throws Exception{
		
		
		Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
	
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		
		ParsedObservationQuery parsedQuery = IntelRecordQuery.parseQuery(query.getQueryString());
		if (parsedQuery.getFilterType() == IQueryFilter.FilterType.OBSERVATION){
			session.beginTransaction();
			String dataTable = (new ObservationFilterProcessor(parsedQuery.getFilter(), dfilter, session)).processFilter();
			
			Object[] resultsTable = configureTableContents(dataTable, session);
			
			String sql = "DROP TABLE " + dataTable;
			logString(sql);
			session.createSQLQuery(sql);
			
			Integer cnt = (Integer) session.createSQLQuery("SELECT count(*) FROM " + resultsTable[0]).uniqueResult();
			
			IntelRecordQueryResults results = new IntelRecordQueryResults((String)resultsTable[0], cnt, (Integer)resultsTable[1]);
			session.getTransaction().commit();
			return results;
		}else{
			//process waypoint filter
		}
		
		
		return null;
		
	}
	
	private Object[] configureTableContents(String observationTable, Session session){
		
		String newTable = SqlGenerator.createTempTableName();
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE " + newTable + " ( ");
		sb.append("observation_uuid char(16) for bit data, ");
		sb.append("location_uuid char(16) for bit data, ");
		
		sb.append("record_uuid char(16) for bit data, ");
		sb.append("record_status varchar(256), ");
		sb.append("record_title varchar(1024), ");
		
		sb.append("loc_id varchar(1028), ");
		sb.append("loc_datetime timestamp, ");
		sb.append("loc_comment varchar(4096), ");
		sb.append("loc_geometry blob, ");
		sb.append("category_uuid char(16) for bit data");
		sb.append(")");
		
		logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " + newTable + " ");
		sb.append(" (observation_uuid, location_uuid, record_uuid, record_status, record_title, loc_id, loc_datetime, loc_comment, loc_geometry, category_uuid )");
		sb.append("SELECT a.observation_uuid, a.location_uuid, r.uuid, r.status, r.title, l.id, l.datetime, l.comment, l.geometry, o.category_uuid ");
		sb.append(" FROM " + observationTable + " a ");
		sb.append(" JOIN smart.i_location l on a.location_uuid = l.uuid ");
		sb.append(" JOIN smart.i_record r on r.uuid = l.record_uuid ");
		sb.append(" LEFT JOIN smart.i_observation o on (o.location_uuid = a.location_uuid and o.uuid = a.observation_uuid) ");
		
		logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		//add category details
		sb = new StringBuilder();
		sb.append("SELECT distinct category_uuid FROM " + newTable + " WHERE category_uuid is not null ");
		List<byte[]> categories = session.createSQLQuery(sb.toString()).list();
		HashMap<Category, List<String>> categoryItems = new HashMap<>();
		int maxCategoryLength = 0;
		for (byte[] c : categories){
			Category category = (Category) session.get(Category.class, UuidUtils.byteToUUID(c));
			List<String> labels = parseCategoryLabels(category);
			maxCategoryLength = Math.max(maxCategoryLength,labels.size());
			
			categoryItems.put(category, labels);
		}
		
		if (maxCategoryLength >= 0){
			for (int i = 0; i < maxCategoryLength; i ++){
				session.createSQLQuery("ALTER TABLE " + newTable + " ADD column category_" + i + " varchar(1024) ").executeUpdate();
			}
			for (Entry<Category, List<String>> update : categoryItems.entrySet()){
				sb = new StringBuilder();
				sb.append("UPDATE " + newTable );
				sb.append(" SET ");
				
				for (int i = 0; i < update.getValue().size(); i ++){
					if (i > 0){
						sb.append(",");
					}
					sb.append("category_" + i + " = :c" + i);
				}
				sb.append(" WHERE category_uuid = :uuid");
				SQLQuery query = session.createSQLQuery(sb.toString());
				query.setParameter("uuid", update.getKey().getUuid());
				
				for (int i = 0; i < update.getValue().size(); i ++){
					query.setParameter("c" + i, update.getValue().get(i));
				}
				query.executeUpdate();
			}
		}
		
		return new Object[]{newTable, maxCategoryLength};
		
	}
	
	private List<String> parseCategoryLabels(Category c){
		List<String> labels = new ArrayList<>();
		while(c != null){
			labels.add(0, c.getName());
			c = c.getParent();
		}
		return labels;
	}
	
	private void logString(String string){
		SqlGenerator.logString(string);
	}
}
