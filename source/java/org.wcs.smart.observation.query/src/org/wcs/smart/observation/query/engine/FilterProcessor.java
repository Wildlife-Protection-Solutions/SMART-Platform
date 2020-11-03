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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
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
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class FilterProcessor implements IFilterProcessor {

	protected String tableName;
	protected String observationTable;
	
	protected AbstractQueryEngine engine;
	protected Query query;
	
	protected HasObservationFilterVisitor observationFilterVisitor = new HasObservationFilterVisitor();
	
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public FilterProcessor(String tableName, AbstractQueryEngine engine, Query query){
		this.tableName = tableName;
		this.engine = engine;
		this.observationTable = engine.createTempTableName();
		this.query = query;
	}
	
	protected DerbyFilterToSqlGenerator getSqlGenerator() {
		return ObservationFilterToSqlGenerator.INSTANCE;
	}
	
	protected Collection<IWaypointSource> getWaypointSources(){
		return WaypointSourceEngine.INSTANCE.getSupportedSources();
	}
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c){
		engine.dropTable(c, observationTable);
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
		qFilter.accept(observationFilterVisitor);	
		
		SubMonitor sub = progress.split(1);
		if (observationFilterVisitor.hasAttributeFilter()){
			createObservationTable(c, qFilter, dateFilter, caFilter, sub);
		}

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
	protected void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		QueryPlugIn.logSql(createTableStatement);
		c.createStatement().execute(createTableStatement);
		
		engine.buildTemporaryTableIndexes(c, tableName);
	}
	
	
	/*
	 * return the table name for the associate object 
	 */
	protected String name(Class<?> clazz){
		return engine.tableName(clazz);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	protected String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
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
	 * @throws Exception
	 */
	protected void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
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
		sql.append(" FROM " ); //$NON-NLS-1$
		usedTables.add(Waypoint.class);
		
		//this is done to improve derby performance; otherwise performance is very slow
		sql.append ("(SELECT * FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		boolean innerwhere = true;
		if (caFilter != null) {
			String filter = getSqlGenerator().toSql(caFilter, engine);
			if (filter.length() > 0) {
				if (innerwhere){
					sql.append(" WHERE "); //$NON-NLS-1$
					innerwhere = false;
				}
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (dateFilter != null) {
			String filter = getSqlGenerator().toSql(dateFilter, engine);
			if (filter.length() > 0) {
				if (innerwhere){
					sql.append(" WHERE "); //$NON-NLS-1$
					innerwhere = false;
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(filter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}
		sql.append(") " + prefix(Waypoint.class)); //$NON-NLS-1$
		sql.append(" "); //$NON-NLS-1$
		
		if (populateObservation || 
				observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
		
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservationGroup.class));
			usedTables.add(WaypointObservationGroup.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".wp_uuid "); //$NON-NLS-1$
			
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			usedTables.add(WaypointObservation.class);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservationGroup.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_group_uuid "); //$NON-NLS-1$
		}	
		
		if (observationFilterVisitor.hasAttributeFilter() || 
				observationFilterVisitor.hasCategoryFilter()){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(name(Category.class));
			usedTables.add(Category.class);
			sql.append(" "); //$NON-NLS-1$
			sql.append(prefix(Category.class));
			
			sql.append(" on " + prefix(Category.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ prefix(WaypointObservation.class)
					+ ".category_uuid "); //$NON-NLS-1$
				if (observationFilterVisitor.hasAttributeFilter()){
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(observationTable + " qa on qa.observation_uuid = "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservation.class) + ".uuid"); //$NON-NLS-1$
				}
		}
		
		AreaFilterVisitor areaVisitor = new AreaFilterVisitor(sql, engine, query.getConservationArea());
		queryFilter.accept(areaVisitor);

		sql.append(engine.appendFromClause(usedTables));
		
		boolean where = true;
		if (caFilter != null) {
			String filter = getSqlGenerator().toSql(caFilter, engine);
			if (filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (dateFilter != null) {
			String filter = getSqlGenerator().toSql(dateFilter, engine);
			if (filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(filter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}
				
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = getSqlGenerator().toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}else{
					sql.append(" AND "); //$NON-NLS-1$	
				}
				sql.append(" ( "); //$NON-NLS-1$
			    sql.append(filter);
			    sql.append(" ) "); //$NON-NLS-1$
			}
		}
		
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	

	
	protected void createObservationTable(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask(Messages.DerbyQueryEngine2_Progress_ProcessingAttributes);
		
		AttributeFilterCollectorVisitor collector = new AttributeFilterCollectorVisitor();
		filter.accept(collector);
		//filter out mlist attributes
		Collection<AttributeInfo> keys = collector.getAttributeInfo().stream().filter(e->e.getType() != AttributeType.MLIST).collect(Collectors.toSet());
		
		
		//mlist attributes
		List<AttributeFilter> mlistFilters = new ArrayList<>();
		IFilterVisitor mlistcollectors = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof AttributeFilter){
					AttributeFilter f = (AttributeFilter) filter;
					if (f.getAttributeType() == AttributeType.MLIST) mlistFilters.add(f);
				}
			}};
		filter.accept(mlistcollectors);
				
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + observationTable + " (observation_uuid char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", \"" + key.getKey() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
					+ engine.getDataType(key.getType()));
		}
		int i = 0;
		for (AttributeFilter f : mlistFilters) {
			String colname = f.getAttributeKey() + "_" + i; //$NON-NLS-1$
			sql.append(", " + colname + " boolean "); //$NON-NLS-1$ //$NON-NLS-2$
			i++;
			engine.filterTables.put(f, new FilterTable("qa", colname)); //$NON-NLS-1$
		}
		sql.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + observationTable + "_obuuid_idx on " + observationTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		String attributeTempTable = engine.createTempTableName();
			
		progress.setWorkRemaining(keys.size());
		for (AttributeInfo key : keys){
			progress.split(1);
			progress.subTask(Messages.DerbyQueryEngine2_Progress_ProcessingAttribute + key.getKey());
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid char(16) for bit data, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
			
			engine.clearParameters();
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(prefix(WaypointObservationAttribute.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append("\"" + key.getKey() + "\"");  //$NON-NLS-1$//$NON-NLS-2$
				sql.append(" "); //$NON-NLS-1$

				sql.append("FROM "); //$NON-NLS-1$

				sql.append(name(Waypoint.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(Waypoint.class)); 
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservationGroup.class));
				sql.append(" as " + prefix(WaypointObservationGroup.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservation.class));
				sql.append(" as " + prefix(WaypointObservation.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(WaypointObservationGroup.class) + ".uuid = " + prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (caFilter != null) {
					String cfilter = getSqlGenerator().toSql(caFilter, engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
				if (dateFilter != null) {
					String dfilter = getSqlGenerator().toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservationAttribute.class)
						+ " as " + prefix(WaypointObservationAttribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(WaypointObservation.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Attribute.class)
						+ " as " + prefix(Attribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Attribute.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeTreeNode.class));
					sql.append(" t on t.uuid = " + prefix(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} 
				sql.append("WHERE "); //$NON-NLS-1$
				sql.append(" " + prefix(Attribute.class) + ".keyid = '"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(key.getKey());
				sql.append("'"); //$NON-NLS-1$
				
				Collection<IWaypointSource> src = getWaypointSources();
				if (src != null && !src.isEmpty()) {
					sql.append(" AND source IN ("); //$NON-NLS-1$
					for (IWaypointSource s : src) {
						sql.append("'" + s.getKey() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.append(")"); //$NON-NLS-1$
				}
				

				QueryPlugIn.logSql(sql.toString());
				try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append("\"" + key.getKey() + "\"");  //$NON-NLS-1$//$NON-NLS-2$
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append("\"" + key.getKey() + "\"");  //$NON-NLS-1$//$NON-NLS-2$
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
		
		for (AttributeFilter listfilter : mlistFilters){
			progress.split(1);
			progress.subTask(Messages.DerbyQueryEngine2_Progress_ProcessingAttribute + listfilter.toString());
			
			//create temporary table for attribute observations
			String columnName = engine.filterTables.get(listfilter).columnname;
			
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid char(16) for bit data, value boolean )"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
			
			engine.clearParameters();
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$
	
				
				sql.append(" true "); //$NON-NLS-1$
	
				sql.append("FROM "); //$NON-NLS-1$
				sql.append(namePrefix(Waypoint.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationGroup.class));
				sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservation.class));
				sql.append(" on " + prefix(WaypointObservationGroup.class) + ".uuid = " + prefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
				if (caFilter != null) {
					String cfilter = getSqlGenerator().toSql(caFilter, engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
				if (dateFilter != null) {
					String dfilter = getSqlGenerator().toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationAttribute.class));
				sql.append(" on " + prefix(WaypointObservation.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(Attribute.class)); 
				sql.append(" on " + prefix(Attribute.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
				String[] mkeys = ((String) listfilter.getValue()).split(AttributeFilter.MLIST_SEPERATOR);
				Operator op = listfilter.getOperator();

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
					for (String key : mkeys) {
						String px = engine.addParameterValue(key);
						sql.append(px);
						sql.append(","); //$NON-NLS-1$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.append(")) foo"); //$NON-NLS-1$
					sql.append(" ON foo.observation_attribute_uuid = "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
						
					
				}else if (op == Operator.AND || op == Operator.EXACT) {
					sql.append(" JOIN ("); //$NON-NLS-1$
					
					int cnt = 0;
					for (String key : mkeys) {
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
						String px = engine.addParameterValue(mkeys.length);
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
				
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(" " + prefix(Attribute.class) + ".keyid = '" + listfilter.getAttributeKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				QueryPlugIn.logSql(sql.toString());
				try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append(columnName );
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append( columnName );
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
		
	}
	
}
