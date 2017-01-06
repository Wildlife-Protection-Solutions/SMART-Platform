package org.wcs.smart.i2.query.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.joda.time.field.ImpreciseDateTimeField;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.observation.filter.AreaFilter;
import org.wcs.smart.i2.query.observation.filter.DataModelFilter;
import org.wcs.smart.i2.query.observation.filter.EntityFilter;
import org.wcs.smart.i2.query.observation.filter.EntityTypeFilter;
import org.wcs.smart.i2.query.observation.filter.IFilterVisitor;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.IntelAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;

public class IntelRecordQueryEngine {

	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	private static AtomicLong tableCnter = new AtomicLong();
	public synchronized String createTempTableName(){
		return "query_temp_i2_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
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
		
		//1. - Observation Query Filter
		//create a table of all observations using date filter
		String obsTable = createTempTableName();
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + obsTable + " ( location_uuid char(16) for bit data, observation_uuid char(16) for bit data ) ");
		
		sql = new StringBuilder();
		sql.append("INSERT INTO " + obsTable);
		sql.append(" SELECT l.uuid, o.uuid FROM smart.i_location l ");
		sql.append(" JOIN smart.i_observation o on l.uuid = o.location_uuid ");
		String dateFilter = SqlGenerator.generateDateClause(dfilter, "datetime");
		if (dateFilter != null){
			sql.append( " WHERE ");
			sql.append(dateFilter);
		}
		
		//for each filter add a column for that filter
		//set the filter value to true or false depending on the filter
		HashMap<IQueryFilter, String> filterToColumnName = new HashMap<IQueryFilter, String>();
		
		parsedQuery.getFilter().accept(new IFilterVisitor() {
			private int columnCnt = 0;
			
			private String createColumn(){
				String columnName = "filter_" + columnCnt++;
				StringBuilder sql = new StringBuilder();
				sql.append("ALTER TABLE " + obsTable + " ADD COLUMN " + columnName + " boolean ");
				return columnName;
			}
			@Override
			public void visitElement(IQueryFilter filter) {
				if (filter instanceof AreaFilter){
					String columnName = createColumn();
					addFilterColumn((AreaFilter) filter, obsTable, columnName);
				}else if (filter instanceof DataModelFilter){
					String columnName = createColumn();
					addFilterColumn((DataModelFilter) filter, obsTable, columnName);
				}else if (filter instanceof EntityFilter){
					String columnName = createColumn();
					addFilterColumn((EntityFilter) filter, obsTable, columnName);
				}else if (filter instanceof EntityTypeFilter){
					String columnName = createColumn();
					addFilterColumn((EntityTypeFilter) filter, obsTable, columnName);
				}else if (filter instanceof IntelAttributeFilter){
					String columnName = createColumn();
					addFilterColumn((IntelAttributeFilter) filter, obsTable, columnName);
				}
				
			}
		});
		
		
		//run the query; getting a list of observations
		
		//create a results table based on that list of observations; adding the fields necessary 
		
		
		
		
		
		return null;
		
	}
	
	private void addFilterColumn(DataModelFilter filter, String obsTable, String columnName){
		
	}
	
	//select * from table where (column name is not null) OR NOT (columnname is not null)
	private void addFilterColumn(EntityFilter filter, String obsTable, String columnName){
		//todo: configure uuid
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");
		sql.append(" SELECT location_uuid FROM smart.i_entity_location WHERE entity_uuid = :uuid )");
		filter.getEntityUuid();
		//TODO: execute
	}
	
	private void addFilterColumn(EntityTypeFilter filter, String obsTable, String columnName){
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE " + obsTable );
		sql.append(" SET " + columnName + " = true ");
		sql.append(" WHERE location_uuid IN ( ");
		sql.append(" SELECT location_uuid FROM smart.i_entity_location l ");
		sql.append(" JOIN smart.i_entity e on l.entity_uuid = e.uuid ");
		sql.append(" JOIN smart.i_entity_type t on e.entity_type_uuid = t.uuid on t.keyId = '" + filter.getTypeKey() + "')");
		//TODO: execute
	}
	
	private void addFilterColumn(IntelAttributeFilter filter, String obsTable, String columnName){
		
	}
	
	private void addFilterColumn(AreaFilter filter, String obsTable, String columnName){
		
	}
}
