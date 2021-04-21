/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.engine.visitor.AreaFilterVisitor;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.CategoryAttributeFilter;
import org.wcs.smart.filter.CategoryFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class PsqlEntityWaypointFilterProcessor implements IFilterProcessor{

	private final Logger logger = Logger.getLogger(PsqlEntityWaypointFilterProcessor.class.getName());
	
	private String tableName;
	private String waypointTable;
	
	private AbstractQueryEngine engine;

	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public PsqlEntityWaypointFilterProcessor(String tableName, AbstractQueryEngine engine){
		this.tableName = tableName;
		this.engine = engine;
		this.waypointTable = engine.createTempTableName();
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c) throws SQLException{
		engine.dropTable(c, waypointTable);
		
		for (FilterTable tableName: engine.filterTables.values()){
			engine.dropTable(c,  tableName.tablename);
		}
	}

	/**
	 * 
	 * @param c database connection
	 * @param queryFilter query filter
	 * @param dateFilter date filter
	 * @param caFilter conservation area filter
	 * @param populateObservation if observation fields (wp_uuid, wp_ob_uuid) are to be populated
	 * @param includeEmptyObservations if waypoints with no observations should be included
	 * @param monitor
	 * @throws SQLException
	 */
	@Override
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, Query query, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations) throws SQLException{
		IFilter qFilter = queryFilter;
		
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		createWaypointFilterTables(c, qFilter, dateFilter, caFilter);
		createTemporaryTable(c);
		populateTemporaryTable(qFilter, dateFilter, query, caFilter, 
				includeEmptyObservations, c, populateObservation);

	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		logger.finest(createTableStatement);
		c.createStatement().execute(createTableStatement);
		
		engine.buildTemporaryTableIndexes(c, tableName);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	private String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
	}
	
	/*
	 * combine the table name with the table prefix for
	 * the given class
	 * Patrol.cass = "smart.patrol p"
	 */
	private String namePrefix(Class<?> clazz){
		return engine.tableNamePrefix(clazz);
	}
	
	/**
	 * Populates the query temporary table.
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param onlyObservations if only observation patrol records with observations
	 * are to be returned,  false will return all patrol records
	 * even if they don't have an observation
	 * @param c database connection
	 * @param populateObservation if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws SQLException
	 */
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			Query query,
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		engine.clearParameters();
		StringBuilder sql = new StringBuilder();
		
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(waypointTable + " as waypointTable "); //$NON-NLS-1$
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_uuid "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		
		if (dateFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		if (populateObservation){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$
		}
			
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			FilterTable t = cols.getValue();
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(t.tablename +"." + t.columnname + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$
		}
			
		AreaFilterVisitor av = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(av);

		sql.append(engine.appendFromClause(usedTables));
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	private void createWaypointFilterTables(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {
		
		engine.createWaypointTable(c, waypointTable,
				WaypointSourceEngine.INSTANCE.getSupportedSources(),
				caFilter, dateFilter);
		
		
		IFilterVisitor attProcessor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if ( filter instanceof AttributeFilter ||
					filter instanceof CategoryFilter  ||	
					filter instanceof CategoryAttributeFilter ||
					filter instanceof EntityAttributeFilter){						
					
					String colName = engine.createTempTableName();
					engine.filterTables.put(filter, new FilterTable(colName, "wp_uuid")); //$NON-NLS-1$
				}
			}
		};
		filter.accept(attProcessor);
		
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			IFilter lfilter = cols.getKey();
			FilterTable t = cols.getValue();
			
			engine.createFilterTable(c, t);
			if ( lfilter instanceof AttributeFilter ||
					lfilter instanceof CategoryFilter  ||	
					lfilter instanceof CategoryAttributeFilter ){
				engine.processWaypointDataModelFilter(t, lfilter, waypointTable, c);
			}else if (lfilter instanceof EntityAttributeFilter) {
				processEntityAttributeFilter(t, (EntityAttributeFilter)lfilter, caFilter, waypointTable, c);
			}else {
				throw new UnsupportedOperationException("Filter not supported for observation queries"); //$NON-NLS-1$
			}
			
		}
	}
	
	private void processEntityAttributeFilter(FilterTable t, EntityAttributeFilter lfilter, ConservationAreaFilter caFilter, String waypointTable, Connection c ) throws SQLException{

		engine.clearParameters();
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT distinct ");  //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class));
		sql.append(".wp_uuid");  //$NON-NLS-1$
		
		sql.append(" FROM ");  //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append(" join ");  //$NON-NLS-1$

		sql.append(namePrefix(WaypointObservationGroup.class));
		sql.append(" on " + waypointTable + ".wp_uuid = "); //$NON-NLS-1$  //$NON-NLS-2$
		sql.append(prefix(WaypointObservationGroup.class));
		sql.append(".wp_uuid "); //$NON-NLS-1$
		
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservation.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class));
		sql.append(".wp_group_uuid "); //$NON-NLS-1$

		
		//get the dm model attribute repesenting the entity
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationAttribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(Attribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Attribute.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(AttributeListItem.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationAttribute.class) + ".list_element_uuid = "); //$NON-NLS-1$
		sql.append(prefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
			
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(" ( SELECT ");  //$NON-NLS-1$
		sql.append("el.keyid as entity_key_id , ");  //$NON-NLS-1$
		
			
		if (lfilter.getAttributeType() == AttributeType.NUMERIC || 
				lfilter.getAttributeType() == AttributeType.BOOLEAN){
			sql.append(engine.tablePrefix(EntityAttributeValue.class));
			sql.append(".number_value");  //$NON-NLS-1$
		}else if (lfilter.getAttributeType() == AttributeType.TEXT ||
				lfilter.getAttributeType() == AttributeType.DATE){
			sql.append(engine.tablePrefix(EntityAttributeValue.class));
			sql.append(".string_value");  //$NON-NLS-1$
		}else if (lfilter.getAttributeType() == AttributeType.LIST){
			sql.append(engine.tablePrefix(AttributeListItem.class));
			sql.append(".keyid");  //$NON-NLS-1$
		}else if(lfilter.getAttributeType() == AttributeType.TREE){
			sql.append(engine.tablePrefix(AttributeTreeNode.class));
			sql.append(".hkey");  //$NON-NLS-1$
		}else{
			throw new RuntimeException(MessageFormat.format(Messages.getString("PsqlEntityWaypointFilterProcessor.AttributeTypeNotSupported", engine.getLocale()), new Object[]{lfilter.getAttributeType()})); //$NON-NLS-1$
		}
		sql.append(" as value FROM "); //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityType.class));
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(Entity.class));
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class) + ".uuid = " + engine.tablePrefix(Entity.class) + ".entity_type_uuid");  //$NON-NLS-1$  //$NON-NLS-2$
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(engine.tableName(AttributeListItem.class));
		sql.append(" el on el.uuid = ");  //$NON-NLS-1$
		sql.append(engine.tablePrefix(Entity.class));
		sql.append(".attribute_list_item_uuid");  //$NON-NLS-1$
		sql.append(" join ");  //$NON-NLS-1$ 
		sql.append(engine.tableNamePrefix(EntityAttributeValue.class));
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".entity_uuid = " + engine.tablePrefix(Entity.class) + ".uuid");  //$NON-NLS-1$  //$NON-NLS-2$
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(engine.tableNamePrefix(EntityAttribute.class));
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttribute.class) + ".entity_type_uuid = " + engine.tablePrefix(EntityType.class) + ".uuid");  //$NON-NLS-1$  //$NON-NLS-2$  
		sql.append(" and " + engine.tablePrefix(EntityAttribute.class) + ".uuid = " + engine.tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid");  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
		
		if (lfilter.getAttributeType() == AttributeType.LIST){
			sql.append(" join ");  //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(AttributeListItem.class));
			sql.append(" ON ");  //$NON-NLS-1$
			sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".list_element_uuid = ");  //$NON-NLS-1$
			sql.append(engine.tablePrefix(AttributeListItem.class) + ".uuid");  //$NON-NLS-1$
		}else if(lfilter.getAttributeType() == AttributeType.TREE){
			sql.append(" join ");  //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(AttributeTreeNode.class));
			sql.append(" ON ");  //$NON-NLS-1$
			sql.append(engine.tablePrefix(EntityAttributeValue.class) + ".tree_node_uuid = ");  //$NON-NLS-1$
			sql.append(engine.tablePrefix(AttributeTreeNode.class) + ".uuid");  //$NON-NLS-1$
		}
			
		sql.append(" WHERE ");  //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityType.class));
		String p1 = engine.addParameterValue(lfilter.getEntityKey());
		sql.append(".keyId = " + p1 ); //$NON-NLS-1$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(engine.tablePrefix(EntityAttribute.class));
		p1 = engine.addParameterValue(lfilter.getEntityAttributeKey());
		sql.append(".keyId = " + p1); //$NON-NLS-1$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, engine.tablePrefix(EntityType.class), engine));
		sql.append(") foo "); //$NON-NLS-1$
		sql.append(" on foo.entity_key_id = "); //$NON-NLS-1$
		sql.append(prefix(AttributeListItem.class) + ".keyid"); //$NON-NLS-1$
				
		
		sql.append(" WHERE "); //$NON-NLS-1$
		
		if (lfilter.getAttributeType() == AttributeType.BOOLEAN){
			sql.append( " (foo.value  > 0.5 ) ");			//$NON-NLS-1$ 
		}else if (lfilter.getAttributeType() == AttributeType.NUMERIC){
			p1 = engine.addParameterValue((Double)lfilter.getValue());
			sql.append( " ( foo.value " + PsqlFilterToSqlGenerator.asSql(lfilter.getOperator()) + " " + p1 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (lfilter.getAttributeType() == AttributeType.TEXT){
			String queryStr = ""; //$NON-NLS-1$
			String val = (String)lfilter.getValue();
			if (lfilter.getOperator() == Operator.STR_CONTAINS || 
					lfilter.getOperator() == Operator.STR_NOTCONTAINS){
				p1 = engine.addParameterValue("%" + val + "%"); //$NON-NLS-1$ //$NON-NLS-2$
				queryStr = "( LOWER(foo.value) " + PsqlFilterToSqlGenerator.asSql(lfilter.getOperator()) + " lower(" + p1 + ") )"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
				
			}else if (lfilter.getOperator() == Operator.STR_EQUALS){
				p1 = engine.addParameterValue(val);
				queryStr = "( LOWER(foo.value) " + PsqlFilterToSqlGenerator.asSql(lfilter.getOperator()) + " lower(" + p1 + ") )";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			}
			sql.append( queryStr);
		}else if (lfilter.getAttributeType() == AttributeType.DATE){
			String date1 = (String) lfilter.getValue();
			String date2 = (String) lfilter.getValue2();	
			p1 = engine.addParameterValue(date1);
			String p2 = engine.addParameterValue(date2);
				
			sql.append ("( foo.value is not null AND DATE(foo.value) "); //$NON-NLS-1$
			sql.append(PsqlFilterToSqlGenerator.asSql(lfilter.getOperator()));
			sql.append(" CAST(" +  p1 + " as date) "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(PsqlFilterToSqlGenerator.asSql(Operator.AND));
			sql.append(" CAST(" + p2 + " as date) )");  //$NON-NLS-1$ //$NON-NLS-2$ 
		}else if (lfilter.getAttributeType() == AttributeType.LIST ){
			if (lfilter.getValue().equals(AttributeFilter.ANY_OPTION_KEY)){
				//any option
				sql.append( "( foo.value is not null )" );  //$NON-NLS-1$ 
			}else{
				p1 = engine.addParameterValue((String)lfilter.getValue());
				sql.append( "( foo.value " + PsqlFilterToSqlGenerator.asSql(lfilter.getOperator()) + " " + p1 + " )" );  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			}
		}else if (lfilter.getAttributeType() == AttributeType.TREE){
			p1 = engine.addParameterValue((String)lfilter.getValue()+ "%"); //$NON-NLS-1$
			sql.append( "( foo.value like " + p1 + " ) ");  //$NON-NLS-1$ //$NON-NLS-2$ 
		}
		
	
		logger.finest(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();
	}
}
