/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.DataModelColumn;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IntelQueryColumnProvider;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for intelligence observation queries.
 * 
 * @author Emily
 *
 */
public class IntelObservationQueryEngine {

	
	private IntelObservationQueryResults queryResults;
	
	/**
	 * Parameters required are session, monitor, and date filter object
	 * @param query
	 * @param parameters
	 * @return
	 */
	public IPagedQueryResultSet executeQuery(IntelRecordObservationQuery query,  HashMap<String, Object> parameters) throws Exception{
		
		Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		monitor.beginTask("Processing Query", 6);
		//one or both element of array may be null
		Date[] dfilter = (Date[]) parameters.get(Date.class.getName());
		if (dfilter == null) return null;
		
		Locale locale = (Locale) parameters.get(Locale.class.getName());
		if (locale == null){
			locale = Locale.getDefault();
		}
		
		monitor.subTask("Parsing Query");
		ParsedObservationQuery parsedQuery = IntelRecordObservationQuery.parseQuery(query.getQueryString());
		monitor.worked(1);
		if (parsedQuery.getFilterType() == IQueryFilter.FilterType.OBSERVATION){
			queryResults = new IntelObservationQueryResults();
			
			session.beginTransaction();
			ObservationFilterProcessor parser = new ObservationFilterProcessor(parsedQuery.getFilter(), dfilter, session); 
			String dataTable = parser.processFilter(new SubProgressMonitor(monitor, 2));
			queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
			
			monitor.subTask("Configuring results table");
			configureTableContents(dataTable, parser.getFilterToColumnNames(), true, session);
			
			String sql = "DROP TABLE " + dataTable;
			SqlGenerator.logString(sql);
			session.createSQLQuery(sql).executeUpdate();
			monitor.worked(1);
			
			monitor.subTask("Computing Query Columns");
			computeQueryColumns(session, locale, query);
			monitor.worked(1);
			
			monitor.subTask("Loading Results");
			Integer cnt = (Integer) session.createSQLQuery("SELECT count(*) FROM " + queryResults.getQueryDataTable()).uniqueResult();
			queryResults.setResultCount(cnt);
			monitor.worked(1);
			
			session.getTransaction().commit();
			return queryResults;
		}else if (parsedQuery.getFilterType() == IQueryFilter.FilterType.WAYPOINT){
			queryResults = new IntelObservationQueryResults();
			
			session.beginTransaction();
			WaypointFilterProcessor parser = new WaypointFilterProcessor(parsedQuery.getFilter(), dfilter, session); 
			String dataTable = parser.processFilter(new SubProgressMonitor(monitor, 2));
			queryResults.setFilterToColumnMap(parser.getFilterToColumnNames());
			
			monitor.subTask("Configuring results table");
			configureTableContents(dataTable, parser.getFilterToColumnNames(), false, session);
			
			String sql = "DROP TABLE " + dataTable;
			SqlGenerator.logString(sql);
			session.createSQLQuery(sql).executeUpdate();
			monitor.worked(1);
			
			monitor.subTask("Computing Query Columns");
			computeQueryColumns(session, locale, query);
			monitor.worked(1);
			
			monitor.subTask("Loading Results");
			Integer cnt = (Integer) session.createSQLQuery("SELECT count(*) FROM " + queryResults.getQueryDataTable()).uniqueResult();
			queryResults.setResultCount(cnt);
			monitor.worked(1);
			
			session.getTransaction().commit();
			return queryResults;
			//process waypoint filter
		}
		return null;		
	}
	
	/*
	 * Configures the query columns; removing non populated attribute columns
	 */
	@SuppressWarnings("unchecked")
	private void computeQueryColumns(Session session, Locale locale, IntelRecordObservationQuery query) throws Exception{
		List<IQueryColumn> columns = IntelQueryColumnProvider.getInstance().getQueryColumns(query, locale, session);
		//remove unused attribute columns
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT distinct a.keyid FROM ");
		sb.append(" smart.dm_attribute a join smart.i_observation_attribute b ON a.uuid = b.attribute_uuid ");
		sb.append(" join " + queryResults.getQueryDataTable() + " c on c.observation_uuid = b.observation_uuid ");
		List<String> populatedAttributes = session.createSQLQuery(sb.toString()).list();
		for (Iterator<IQueryColumn> iterator = columns.iterator(); iterator.hasNext();) {
			IQueryColumn column = (IQueryColumn) iterator.next();
			if (column instanceof DataModelColumn ){
				String attribute = ((DataModelColumn)column).getAttributeKey();
				if (attribute != null && !populatedAttributes.contains(attribute)){
					//iterator.remove();
					((DataModelColumn) column).setVisible(false);
				}
			}
			
		}
		queryResults.setQueryColumns(columns);
	}
	
	/*
	 * configure results table added necessary results columsn
	 */
	@SuppressWarnings("unchecked")
	private void configureTableContents(String observationTable, HashMap<IQueryFilter, String> filterToColumn, boolean obsFilter, Session session){
		
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
		
		SqlGenerator.logString(sb.toString());
		session.createSQLQuery(sb.toString()).executeUpdate();
		
		
		sb = new StringBuilder();
		sb.append(" INSERT INTO " + newTable + " ");
		sb.append(" ( " + insert.toString() + ")" );
		sb.append("SELECT o.uuid, a.location_uuid, r.uuid, r.status, r.title, l.id, l.datetime, l.comment, l.geometry, o.category_uuid ");
		sb.append(select);
		sb.append(" FROM " + observationTable + " a ");
		sb.append(" JOIN smart.i_location l on a.location_uuid = l.uuid ");
		sb.append(" JOIN smart.i_record r on r.uuid = l.record_uuid ");
		sb.append(" LEFT JOIN smart.i_observation o on o.location_uuid = a.location_uuid ");
		if (obsFilter){
			sb.append(" AND o.uuid = a.observation_uuid ");
		}
		SqlGenerator.logString(sb.toString());
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
		
		queryResults.setResultsTable(newTable);
		queryResults.setCategoryCount(maxCategoryLength);
		queryResults.setColumnNameToIndexMap(columnNameToIndex);
		
	}
	
	private List<String> parseCategoryLabels(Category c){
		List<String> labels = new ArrayList<>();
		while(c != null){
			labels.add(0, c.getName());
			c = c.getParent();
		}
		return labels;
	}
	
}
