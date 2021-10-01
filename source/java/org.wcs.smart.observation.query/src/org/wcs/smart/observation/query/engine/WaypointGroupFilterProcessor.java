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
package org.wcs.smart.observation.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.CategoryAttributeFilter;
import org.wcs.smart.filter.CategoryFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.observation.query.engine.visitor.AreaFilterVisitor;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IFilterProcessor;
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
public class WaypointGroupFilterProcessor implements IFilterProcessor{

	protected String tableName;
	protected String waypointTable;
	
	protected AbstractQueryEngine engine;
	protected Query query;
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public WaypointGroupFilterProcessor(String tableName, AbstractQueryEngine engine, Query query){
		this.tableName = tableName;
		this.engine = engine;
		this.waypointTable = engine.createTempTableName();
		this.query = query;
	}
	
	
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return ObservationFilterToSqlGenerator.INSTANCE;
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c){
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
			DateFilter dateFilter, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations,
			IProgressMonitor monitor) throws SQLException{
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		progress.subTask(Messages.DerbySummaryEngine_Progress_CreatingObservationTable);
		
		IFilter qFilter = queryFilter;
		
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		createWaypointTable(c, dateFilter, caFilter, progress.split(1));
		processFilters(qFilter, caFilter, c, progress);
		
		progress.subTask(Messages.DerbySummaryEngine_Progress_CreatingTempTable);
		progress.split(1);
		createTemporaryTable(c);
		
		progress.split(1);
		populateTemporaryTable(qFilter, dateFilter, caFilter, 
				includeEmptyObservations, c, populateObservation);

	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		QueryPlugIn.logSql(createTableStatement);
		c.createStatement().execute(createTableStatement);
		
		engine.createTemporaryTableIndexes(c, tableName);
	}
		
	/*
	 * return the sql prefix for the given class
	 */
	protected String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	protected String name(Class<?> clazz){
		return engine.tableName(clazz);
	}
	
	/*
	 * combine the table name with the table prefix for
	 * the given class
	 * Patrol.cass = "smart.patrol p"
	 */
	protected String namePrefix(Class<?> clazz){
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
	protected void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		
		sql.append(waypointTable + " as waypointTable "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_uuid "); //$NON-NLS-1$
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationGroup.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_group_uuid "); //$NON-NLS-1$
		
		if (populateObservation){
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
			
			sql.append(t.tablename + "." + t.primarykey + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$
			
			if (t.secondarykey != null) {
				sql.append(" AND "); //$NON-NLS-1$
				
				sql.append("("); //$NON-NLS-1$
				sql.append(t.tablename +"." + t.secondarykey + " = "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(prefix(WaypointObservationGroup.class) + ".uuid "); //$NON-NLS-1$
			
				sql.append(" OR ("); //$NON-NLS-1$
				sql.append(t.tablename +"." + t.secondarykey + " is null and "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(prefix(WaypointObservationGroup.class) + ".uuid is null)"); //$NON-NLS-1$
				
				sql.append(")"); //$NON-NLS-1$
			}
		}
			
		AreaFilterVisitor av = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(av);

		sql.append(engine.appendFromClause(usedTables));
		
		// ---- WHERE CLAUSE -----
		List<String> filters = new ArrayList<>();
		if (caFilter != null) {
			String filter = getSqlGenerator().toSql(caFilter, engine);
			if (filter.length() > 0) filters.add(filter);
		}
	
		if (dateFilter != null) {
			String filter = getSqlGenerator().toSql(dateFilter, engine);
			if (filter.length() > 0) filters.add(filter);
			
		}
		
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = getSqlGenerator().toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) filters.add(filter);
		}
				
		if (!filters.isEmpty()) {
			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(filters.get(0));
			for (int i = 1; i < filters.size(); i ++) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(filters.get(i));
			}
		}
		
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	protected void createWaypointTable(Connection c,  
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask(Messages.WaypointFilterProcessor_progress1);
		//HashMap<IFilter, String> filter2Column = new HashMap<IFilter, String>();
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid char(16) for bit data, wp_group_uuid char(16) for bit data)"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + waypointTable + "_wpuuid_idx on " + waypointTable + " (wp_group_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		engine.clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid, wp_group_uuid) SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class));
		sql.append(".uuid, "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservationGroup.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$

		sql.append(namePrefix(Waypoint.class));
		sql.append(" left join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservationGroup.class));
		sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		List<String> filters = new ArrayList<>();
		
		if (caFilter != null) {
			String cfilter = getSqlGenerator().toSql(caFilter, engine);
			if (cfilter.length() > 0) filters.add(cfilter);
		}
		if (dateFilter != null) {
			String dfilter = getSqlGenerator().toSql(dateFilter, engine);
			if (dfilter.length() > 0) filters.add(dfilter);
		}
		
		if (!filters.isEmpty()) {
			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(filters.get(0));
			for (int i = 1; i < filters.size(); i ++) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(filters.get(i));
			}
		}

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	protected boolean columnRequired(IFilter filter) {
		return filter instanceof AttributeFilter ||
				filter instanceof CategoryFilter  ||	
				filter instanceof CategoryAttributeFilter ;
	}
	
	protected void processFilters(IFilter filter, ConservationAreaFilter caFilter, Connection c, SubMonitor progress) throws SQLException {
			IFilterVisitor attProcessor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if ( columnRequired(filter) ){						
					
					String colName = engine.createTempTableName();
					engine.filterTables.put(filter, new FilterTable(colName, "wp_uuid", "wp_group_uuid")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		};
		filter.accept(attProcessor);
		
		progress.setWorkRemaining(engine.filterTables.entrySet().size());
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			IFilter lfilter = cols.getKey();
			FilterTable t = cols.getValue();
			
			progress.subTask(Messages.WaypointFilterProcessor_filterProgress + lfilter.asString() );
			progress.split(1);
			
			processFilter(lfilter, caFilter, t, c);
		}
		
	}
	
	protected void processFilter(IFilter filter, ConservationAreaFilter caFilter, FilterTable table, Connection c) throws SQLException {
	
		if (filter instanceof AttributeFilter ||
				filter instanceof CategoryAttributeFilter ||
				filter instanceof CategoryFilter) {
			processDmFilter(filter, table, c);
		}
	}

	protected void createTemporaryFilterTable(FilterTable table, Connection c) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(table.tablename);
		sql.append("("); //$NON-NLS-1$
		sql.append(table.primarykey + " char(16) for bit data"); //$NON-NLS-1$
		if (table.secondarykey != null) {
			sql.append(","); //$NON-NLS-1$
			sql.append(table.secondarykey + " char(16) for bit data");	 //$NON-NLS-1$
		}
		sql.append(")"); //$NON-NLS-1$ 
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());


		sql = new StringBuilder();
		sql.append("CREATE INDEX "); //$NON-NLS-1$
		sql.append(table.tablename + "_wp_uuid_idx on "); //$NON-NLS-1$
		sql.append(table.tablename + "(" + table.primarykey + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	
	protected void processDmFilter(IFilter filter, FilterTable table, Connection c) throws SQLException {
		
		engine.clearParameters();

		createTemporaryFilterTable(table, c);

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(table.tablename);
		sql.append(" ("); //$NON-NLS-1$
		sql.append(table.primarykey);
		sql.append(","); //$NON-NLS-1$
		sql.append(table.secondarykey);
		sql.append(")"); //$NON-NLS-1$
		sql.append(" SELECT distinct a.wp_uuid, "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class));
		sql.append(".wp_group_uuid"); //$NON-NLS-1$

		AttributeFilter attfilter = null;
		CategoryFilter catfilter = null;
		if (filter instanceof AttributeFilter) {
			attfilter = (AttributeFilter) filter;
		} else if (filter instanceof CategoryAttributeFilter) {
			attfilter = ((CategoryAttributeFilter) filter).getAttributeFilter();
			catfilter = ((CategoryAttributeFilter) filter).getCategoryFilter();
		} else if (filter instanceof CategoryFilter) {
			catfilter = (CategoryFilter) filter;
		}

		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(waypointTable + " a "); //$NON-NLS-1$

		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(WaypointObservation.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(WaypointObservation.class));
		sql.append(".wp_group_uuid = "); //$NON-NLS-1$
		sql.append("a.wp_group_uuid "); //$NON-NLS-1$

		if (catfilter != null) {
			sql.append(" join "); //$NON-NLS-1$
			sql.append(namePrefix(Category.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Category.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".category_uuid "); //$NON-NLS-1$
		}
		if (attfilter != null) {
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
			if (attfilter.getAttributeType() == AttributeType.LIST) {
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(AttributeListItem.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class) + ".list_element_uuid = "); //$NON-NLS-1$
				sql.append(prefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
			}
			if (attfilter.getAttributeType() == AttributeType.TREE) {
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(AttributeTreeNode.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class) + ".tree_node_uuid = "); //$NON-NLS-1$
				sql.append(prefix(AttributeTreeNode.class) + ".uuid"); //$NON-NLS-1$
			}
			if (attfilter.getAttributeType().equals(AttributeType.MLIST)) {
				processMultiSelectAttributeFilter(sql, attfilter);
			}
		}
		sql.append(" WHERE "); //$NON-NLS-1$
		if (catfilter != null) {
			String keyPart = catfilter.getCategoryKey();
			String p1 = engine.addParameterValue(keyPart);
			String p2 = engine.addParameterValue(keyPart.substring(0, keyPart.length() - 1) + "/"); //$NON-NLS-1$
			sql.append(" ( "); //$NON-NLS-1$
			sql.append(prefix(Category.class));
			sql.append(".hkey >= " + p1 + " and "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(Category.class));
			sql.append(".hkey <  " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (attfilter != null) {
			if (catfilter != null) {
				sql.append(" AND "); //$NON-NLS-1$
			}
			String p1 = engine.addParameterValue(attfilter.getAttributeKey());
			sql.append(prefix(Attribute.class) + ".keyid = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
			if (attfilter.getAttributeType() != AttributeType.MLIST)
				sql.append(" AND "); //$NON-NLS-1$

			if (attfilter.getAttributeType() == AttributeType.NUMERIC) {
				sql.append("("); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".number_value "); //$NON-NLS-1$
				sql.append(getSqlGenerator().asSql(attfilter.getOperator()));
				String p2 = engine.addParameterValue((Double) attfilter.getValue());
				sql.append(p2 + " )"); //$NON-NLS-1$

			} else if (attfilter.getAttributeType() == AttributeType.BOOLEAN) {
				sql.append("("); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".number_value > 0.5 "); //$NON-NLS-1$
				sql.append(") "); //$NON-NLS-1$
			} else if (attfilter.getAttributeType() == AttributeType.TEXT) {
				sql.append("(lower("); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".string_value) "); //$NON-NLS-1$

				if (attfilter.getOperator() == Operator.STR_CONTAINS
						|| attfilter.getOperator() == Operator.STR_NOTCONTAINS) {
					String p2 = engine.addParameterValue("%" + ((String) attfilter.getValue()) + "%"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(getSqlGenerator().asSql(attfilter.getOperator()) + " LOWER(" + p2 + ") )"); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (attfilter.getOperator() == Operator.STR_EQUALS) {
					String p2 = engine.addParameterValue(((String) attfilter.getValue()));
					sql.append(getSqlGenerator().asSql(attfilter.getOperator()) + " LOWER(" + p2 + ") )"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} else if (attfilter.getAttributeType() == AttributeType.LIST) {
				sql.append("("); //$NON-NLS-1$
				sql.append(prefix(AttributeListItem.class));
				sql.append(".keyid "); //$NON-NLS-1$

				if (((String) attfilter.getValue()).equals(AttributeFilter.ANY_OPTION_KEY)) {
					sql.append(" is not null "); //$NON-NLS-1$
				} else {
					sql.append(getSqlGenerator().asSql(attfilter.getOperator()));
					String p2 = engine.addParameterValue((String) attfilter.getValue());
					sql.append(p2);
				}
				sql.append(") "); //$NON-NLS-1$
			} else if (attfilter.getAttributeType() == AttributeType.TREE) {
				String p2 = engine.addParameterValue(((String) attfilter.getValue()));
				String p3 = engine.addParameterValue(
						((String) attfilter.getValue()).substring(0, ((String) attfilter.getValue()).length() - 1)
								+ "/"); //$NON-NLS-1$
				sql.append("("); //$NON-NLS-1$
				sql.append(prefix(AttributeTreeNode.class));
				sql.append(".hkey >= " + p2 + " and "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(prefix(AttributeTreeNode.class));
				sql.append(".hkey < " + p3 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (attfilter.getAttributeType() == AttributeType.DATE) {
				String p2 = engine.addParameterValue(attfilter.getValue());
				String p3 = engine.addParameterValue(attfilter.getValue2());
				sql.append("("); //$NON-NLS-1$
				sql.append(" DATE ("); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".string_value ) "); //$NON-NLS-1$
				sql.append(getSqlGenerator().asSql(attfilter.getOperator()));
				sql.append(" CAST(" + p2 + " as date) "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(getSqlGenerator().asSql(Operator.AND));
				sql.append(" CAST(" + p3 + " as date) "); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(") "); //$NON-NLS-1$

			}
		}

		QueryPlugIn.logSql(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();

	}
	
	private void processMultiSelectAttributeFilter(StringBuilder sql, AttributeFilter attfilter) {
		String[] keys = ((String) attfilter.getValue()).split(AttributeFilter.MLIST_SEPERATOR);
		Operator op = attfilter.getOperator();

		if (op == Operator.OR) {
			sql.append(" JOIN ("); //$NON-NLS-1$
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationAttributeList.class));
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AttributeListItem.class));
			sql.append(" ON "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
			sql.append(prefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
			sql.append(" AND "); //$NON-NLS-1$
			sql.append(prefix(AttributeListItem.class) +".keyid in ("); //$NON-NLS-1$
			for (String key : keys) {
				String px = engine.addParameterValue(key);
				sql.append(px);
				sql.append(","); //$NON-NLS-1$
			}
			sql.deleteCharAt(sql.length() - 1);
			sql.append(")) foo"); //$NON-NLS-1$
			sql.append(" ON foo.observation_attribute_uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
				
			
		}
		if (op == Operator.AND || op == Operator.EXACT) {
			sql.append(" JOIN ("); //$NON-NLS-1$
			
			int cnt = 0;
			for (String key : keys) {
				String px = engine.addParameterValue(key);
				if (cnt != 0) sql.append(" INTERSECT "); //$NON-NLS-1$
				cnt++;
				sql.append("SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationAttributeList.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append(namePrefix(AttributeListItem.class));
				sql.append(" ON "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
				sql.append(prefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(prefix(AttributeListItem.class) +".keyid =" + px );	 //$NON-NLS-1$
			}
			
			if (op == Operator.EXACT) {
				String px = engine.addParameterValue(keys.length);
				sql.append(" INTERSECT "); //$NON-NLS-1$
				sql.append("SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationAttributeList.class));
				sql.append(" GROUP BY observation_attribute_uuid HAVING count(*) = " + px); //$NON-NLS-1$
			}
			sql.append(" ) k"); //$NON-NLS-1$
			
			sql.append(" ON k.observation_attribute_uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
		}
	}
}
