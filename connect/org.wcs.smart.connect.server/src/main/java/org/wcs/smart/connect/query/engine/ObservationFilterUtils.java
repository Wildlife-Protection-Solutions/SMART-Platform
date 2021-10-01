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
package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.IFilterVisitor;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Utilities for observation filters
 * 
 * @author Emily
 *
 */
public class ObservationFilterUtils {

	private final static Logger logger = Logger.getLogger(ObservationFilterUtils.class.getName());

	public interface IDateFilterProcessor {
		
		public void processDateFilter(AbstractQueryEngine engine, StringBuilder sql) throws SQLException;
	}

		
	/**
	 * Creates a table of observations with columns for data
	 * model filters
	 * @param observationTable
	 * @param c
	 * @param filter
	 * @param engine
	 * @param dateFilter
	 * @param caFilter
	 * @param sources
	 * @throws SQLException
	 */
	public static void createObservationTable(String observationTable, 
			Connection c, IFilter filter,
			AbstractQueryEngine engine,
			IDateFilterProcessor dateFilterProcessor, ConservationAreaFilter caFilter,
			Collection<IWaypointSource> sources)
			throws SQLException {
		createObservationTable(observationTable, c, filter, engine, dateFilterProcessor,
				caFilter, sources, new AttributeFilterCollectorVisitor());
	}
	
	public static void createObservationTable(String observationTable, 
			Connection c, IFilter filter,
			AbstractQueryEngine engine,
			IDateFilterProcessor dateFilterProcessor,
			ConservationAreaFilter caFilter,
			Collection<IWaypointSource> sources, AttributeFilterCollectorVisitor collector)
			throws SQLException {
		
		
		filter.accept(collector);

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
		sql.append("CREATE TABLE " + observationTable + " (observation_uuid uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " " //$NON-NLS-1$ //$NON-NLS-2$
					+ engine.getDataType(key.getType()));
		}
		int i = 0;
		for (AttributeFilter f : mlistFilters) {
			String colname = f.getAttributeKey() + "_" + i; //$NON-NLS-1$
			sql.append(", " + colname + " boolean ");  //$NON-NLS-1$//$NON-NLS-2$
			i++;
			engine.filterTables.put(f,  new FilterTable("qa", colname)); //$NON-NLS-1$
		}
		
		sql.append(")"); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + engine.getIndexName(observationTable) + "_obuuid_idx on " + observationTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		String attributeTempTable = engine.createTempTableName();
			
		for (AttributeInfo key : keys){
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid uuid, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
			try {
				engine.clearParameters();
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(engine.tablePrefix(WaypointObservationAttribute.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" "); //$NON-NLS-1$

				sql.append("FROM "); //$NON-NLS-1$

				sql.append(engine.tableNamePrefix(Waypoint.class));
				

				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservationGroup.class));
				sql.append(" on " + engine.tablePrefix(Waypoint.class) + ".uuid = " + engine.tablePrefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				
				if (caFilter != null) {
					String cfilter = PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, engine.tablePrefix(Waypoint.class), engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
			
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservation.class));
				sql.append(" on " + engine.tablePrefix(WaypointObservationGroup.class) + ".uuid = " + engine.tablePrefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
				sql.append(" on " + engine.tablePrefix(WaypointObservation.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(Attribute.class));					
				sql.append(" on " + engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableName(AttributeListItem.class));
					sql.append(" l on l.uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableName(AttributeTreeNode.class));
					sql.append(" t on t.uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				if (dateFilterProcessor != null) dateFilterProcessor.processDateFilter(engine, sql);
				
				
				sql.append("WHERE "); //$NON-NLS-1$
				String p = engine.addParameterValue(key.getKey());
				sql.append(" " + engine.tablePrefix(Attribute.class) + ".keyid = " + p ); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(Waypoint.class) + ".source in ("); //$NON-NLS-1$
				for(IWaypointSource src : sources) {
					sql.append("'" + src.getKey() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")"); //$NON-NLS-1$
				
				logger.finest(sql.toString());
				
				try(NamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}
				
				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(engine.getIndexName(attributeTempTable));
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
		ObservationFilterUtils.processMultiListFilters(mlistFilters, engine, 
				caFilter, dateFilterProcessor, attributeTempTable, observationTable, c);
		
	}
	
	/**
	 * adds a true/false column to the observation table for each
	 * attribute in the mlistFilters.  These must be only MLIST attributes
	 * 
	 * @param mlistFilters
	 * @param engine
	 * @param caFilter
	 * @param dateFilter
	 * @param attributeTempTable
	 * @param observationTable
	 * @param c
	 * @throws SQLException
	 */
	private static void processMultiListFilters(List<AttributeFilter> mlistFilters, 
			AbstractQueryEngine engine,
			ConservationAreaFilter caFilter, IDateFilterProcessor dateFilterProcessor,
			String attributeTempTable, 
			String observationTable, Connection c) throws SQLException {
		
		for (AttributeFilter listfilter : mlistFilters){
			if (listfilter.getAttributeType() != Attribute.AttributeType.MLIST) throw new IllegalArgumentException();
			
			//create temporary table for attribute observations
			String columnName = engine.filterTables.get(listfilter).primarykey;
			
			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid uuid, value boolean )"); //$NON-NLS-1$
			logger.finest(sql.toString());

			c.createStatement().execute(sql.toString());
			
			engine.clearParameters();
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT distinct "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$
				sql.append(" true "); //$NON-NLS-1$
				sql.append("FROM "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(Waypoint.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservationGroup.class));
				sql.append(" on " + engine.tablePrefix(Waypoint.class) + ".uuid = " + engine.tablePrefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservation.class));
				sql.append(" on " + engine.tablePrefix(WaypointObservationGroup.class) + ".uuid = " + engine.tablePrefix(WaypointObservation.class) + ".wp_group_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
				if (caFilter != null) {
					String cfilter = PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, engine.tablePrefix(Waypoint.class), engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(WaypointObservationAttribute.class));
				sql.append(" on " + engine.tablePrefix(WaypointObservation.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(Attribute.class)); 
				sql.append(" on " + engine.tablePrefix(Attribute.class) + ".uuid = " + engine.tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
				String[] mkeys = ((String) listfilter.getValue()).split(AttributeFilter.MLIST_SEPERATOR);
				Operator op = listfilter.getOperator();

				if (op == Operator.OR) {
					sql.append(" JOIN ("); //$NON-NLS-1$
					sql.append("SELECT "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(WaypointObservationAttributeList.class));
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(AttributeListItem.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
					sql.append(" AND "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(AttributeListItem.class) +".keyid in ("); //$NON-NLS-1$
					for (String key : mkeys) {
						String px = engine.addParameterValue(key);
						sql.append(px);
						sql.append(","); //$NON-NLS-1$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.append(")) foo"); //$NON-NLS-1$
					sql.append(" ON foo.observation_attribute_uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
						
					
				}else if (op == Operator.AND || op == Operator.EXACT) {
					sql.append(" JOIN ("); //$NON-NLS-1$
					
					int cnt = 0;
					for (String key : mkeys) {
						String px = engine.addParameterValue(key);
						if (cnt != 0) sql.append(" INTERSECT "); //$NON-NLS-1$
						cnt++;
						sql.append("SELECT "); //$NON-NLS-1$
						sql.append(engine.tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
						sql.append(engine.tableNamePrefix(WaypointObservationAttributeList.class));
						sql.append(" JOIN "); //$NON-NLS-1$
						sql.append(engine.tableNamePrefix(AttributeListItem.class));
						sql.append(" ON "); //$NON-NLS-1$
						sql.append(engine.tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
						sql.append(engine.tablePrefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
						sql.append(" AND "); //$NON-NLS-1$
						sql.append(engine.tablePrefix(AttributeListItem.class) +".keyid =" + px );	 //$NON-NLS-1$
					}
					
					if (op == Operator.EXACT) {
						String px = engine.addParameterValue(mkeys.length);
						sql.append(" INTERSECT "); //$NON-NLS-1$
						sql.append("SELECT "); //$NON-NLS-1$
						sql.append(engine.tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
						sql.append(engine.tableNamePrefix(WaypointObservationAttributeList.class));
						sql.append(" GROUP BY observation_attribute_uuid HAVING count(*) = " + px); //$NON-NLS-1$
					}
					sql.append(" ) k"); //$NON-NLS-1$
					
					sql.append(" ON k.observation_attribute_uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
				}
				
				if (dateFilterProcessor != null) dateFilterProcessor.processDateFilter(engine, sql);
				
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(" " + engine.tablePrefix(Attribute.class) + ".keyid = '" + listfilter.getAttributeKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				logger.finest(sql.toString());
				try(PsqlNamedPreparedStatement ps = engine.parseQueryString(c, sql.toString())){
					ps.executeUpdate();
				}

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(" on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				logger.finest(sql.toString());
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
				logger.finest(sql.toString());
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
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				logger.finest(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
	}

}
