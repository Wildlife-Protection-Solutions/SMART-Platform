package org.wcs.smart.i2.query.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
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
public class IntelObservationQueryEngine {

	
	/**
	 * parameters requires a session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(IntelRecordObservationQuery query,  HashMap<String, Object> parameters) throws Exception{
		
		
		Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		monitor.beginTask("Processing Query", 5);
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		monitor.subTask("Parsing Query");
		ParsedObservationQuery parsedQuery = IntelRecordObservationQuery.parseQuery(query.getQueryString());
		monitor.worked(1);
		if (parsedQuery.getFilterType() == IQueryFilter.FilterType.OBSERVATION){
			
			session.beginTransaction();
			ObservationFilterProcessor parser = new ObservationFilterProcessor(parsedQuery.getFilter(), dfilter, session); 
			String dataTable = parser.processFilter(new SubProgressMonitor(monitor, 2));
			
			monitor.subTask("Configuring results table");
			Object[] resultsTable = configureTableContents(dataTable, parser.getFilterToColumnNames(), session);
			
			String sql = "DROP TABLE " + dataTable;
			logString(sql);
			session.createSQLQuery(sql);
			monitor.worked(1);
			
			monitor.subTask("Loading Results");
			Integer cnt = (Integer) session.createSQLQuery("SELECT count(*) FROM " + resultsTable[0]).uniqueResult();
			IntelObservationQueryResults results = new IntelObservationQueryResults((String)resultsTable[0], cnt, (Integer)resultsTable[1], parser.getFilterToColumnNames(), (HashMap<String,Integer>)resultsTable[2]);
			monitor.worked(1);
			session.getTransaction().commit();
			return results;
		}else{
			//process waypoint filter
		}
		
		
		return null;
		
	}
	
	private Object[] configureTableContents(String observationTable, HashMap<IQueryFilter, String> filterToColumn, Session session){
		
		HashMap<String, Integer> columnNameToIndex = new HashMap<>();
		
		String newTable = SqlGenerator.createTempTableName();
		
		String[][] columns = new String[][]{
			{"observation_uuid", "char(16) for bit data"},
			{"location_uuid", "char(16) for bit data"},
			{"record_uuid", "char(16) for bit data"},
			{"record_status", "varchar(256)"},
			{"record_title", "varchar(1024)"},
			{"loc_id", "varchar(1024)"},
			{"loc_datetime", "timestamp"},
			{"loc_comment", "varchar(4096)"},
			{"loc_geometry", "blob"},
			{"category_uuid", "char(16) for bit data"}
		};
		
		String[][] sortColumns = new String[][]{
			{"str_sort", "varchar(1024)"},
			{"dbl_sort", "double"},
			{"date_sort", "date"}		
		};
		
		StringBuilder insert = new StringBuilder();
		StringBuilder select = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		int columnIndex = 0;
		sb.append("CREATE TABLE " + newTable + " ( ");
		for (String[] c : columns){
			sb.append( c[0] + " " + c[1] + ",");
			columnNameToIndex.put(c[0], columnIndex++);
			insert.append(c[0] + ",");
		}
		for (String[] c : sortColumns){
			sb.append( c[0] + " " + c[1] + ",");
			columnNameToIndex.put(c[0], columnIndex++);
		}
		for (String v : filterToColumn.values()){
			sb.append( v + " boolean,");
			columnNameToIndex.put(v, columnIndex++);
			insert.append(v + ",");
			select.append(", a." + v );
		}
		
		insert.deleteCharAt(insert.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		
		logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " + newTable + " ");
		sb.append(" ( " + insert.toString() + ")" );
		sb.append("SELECT a.observation_uuid, a.location_uuid, r.uuid, r.status, r.title, l.id, l.datetime, l.comment, l.geometry, o.category_uuid ");
		sb.append(select);
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
				columnNameToIndex.put("category_" + i, columnIndex++);
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
		
		return new Object[]{newTable, maxCategoryLength, columnNameToIndex};
		
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
