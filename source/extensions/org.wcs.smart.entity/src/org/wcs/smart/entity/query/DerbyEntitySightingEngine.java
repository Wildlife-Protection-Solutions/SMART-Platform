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
package org.wcs.smart.entity.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.EntityFilter.EntityFilterType;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.SmartUtils;

/**
 * Query engine for executing entity sighting queries in a lazy
 * manner.  This engine creates a temporary table of results
 * that can feed the ui.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DerbyEntitySightingEngine extends AbstractQueryEngine {

	static {
		tablePrefix.put(Entity.class, "e"); //$NON-NLS-1$
		tablePrefix.put(EntityType.class, "et"); //$NON-NLS-1$
		tablePrefix.put(EntityAttributeValue.class, "eav"); //$NON-NLS-1$
	}

	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Entity.class, "smart.entity"); //$NON-NLS-1$
		tableNames.put(EntityType.class, "smart.entity_type"); //$NON-NLS-1$
		tableNames.put(EntityAttributeValue.class, "smart.entity_attribute_value"); //$NON-NLS-1$
	}

	private String queryDataTable;

	private EntityQuery query;
	private DateFilter localDateFilter;
	private int categoryCount;

	public SightingPagedResults executeDerbyQuery(EntityQuery query,
			final Session session, final IProgressMonitor monitor)
			throws SQLException {
		this.query = query;
		
		// create a date filter that caches the dates so the same
		// dates are used for all parts of the query;
		// otherwise different date filters will be computed
		// for different parts of the queries
		

		localDateFilter = new DateFilter( WaypointDateField.INSTANCE, new CachingDateFilter(query
				.getDateFilter().getDateFilterOption()));
		
		queryDataTable = createTempTableName();

		final SightingPagedResults result = new SightingPagedResults(
				queryDataTable, this);

		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				monitor.beginTask("Processing Query", 3);

				monitor.subTask("Creating Temporary Table");
				createResultsTable(c);
				monitor.worked(1);

				monitor.subTask("Populating Data Table");
				populateDataTable(c);
				monitor.worked(1);
				
				monitor.subTask("Adding Category Labels");
				addCategoryLabels(c, session);
				monitor.worked(1);
				
				
				//setting waypoint count
				ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable + ""); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					if (rs.next()) {
						result.setItemCount(rs.getInt(1));
					}
				 } finally {
					 rs.close();
				 }

				dropTemporaryTables(c, monitor.isCanceled());
			}
		});
		return result;
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c
	 *            connection
	 * @throws SQLException
	 */
	private void dropTemporaryTables(Connection c, boolean fullDrop)
			throws SQLException {
		if (!fullDrop)
			return;
		// original table
		dropTable(c, queryDataTable);
	}


	protected void populateDataTable(Connection c) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + queryDataTable + " ");
		sql.append(" (ca_uuid,ca_id,ca_name,wp_uuid,wp_source,wp_id,wp_x,wp_y,wp_direction,wp_distance,wp_time,wp_comment,ob_uuid,ob_category_uuid,entity_uuid,entity_id,entity_status) ");
		sql.append(" SELECT "); //$NON-NLS-1$
		
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(ConservationArea.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(ConservationArea.class) + ".name, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".source, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".status "); //$NON-NLS-1$
		
		sql.append(" FROM ");
		sql.append(tableNamePrefix(Waypoint.class));
		sql.append(" join ");
		sql.append(tableNamePrefix(ConservationArea.class));
		sql.append(" on " + tablePrefix(Waypoint.class) + ".ca_uuid = " + tablePrefix(ConservationArea.class) + ".uuid");
		sql.append(" join ");
		sql.append(tableNamePrefix(WaypointObservation.class));
		sql.append(" on " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".wp_uuid");
		sql.append(" join ");
		sql.append(tableNamePrefix(WaypointObservationAttribute.class));
		sql.append(" on " + tablePrefix(WaypointObservation.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid");
		sql.append(" join ");
		sql.append(tableNamePrefix(Entity.class));
		sql.append(" on " + tablePrefix(Entity.class) + ".attribute_list_item_uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid");
		sql.append(" join ");
		sql.append(tableNamePrefix(EntityType.class));
		sql.append(" on " + tablePrefix(EntityType.class) + ".uuid = " + tablePrefix(Entity.class) + ".entity_type_uuid");
		
		sql.append(" WHERE ");
		
		DerbyFilterToSqlGenerator sqlGenerator = new DerbyFilterToSqlGenerator();
		
		//entity type
		sql.append(tablePrefix(EntityType.class) + ".uuid = x'" + SmartUtils.encodeHex(query.getEntityType().getUuid()) + "'");
		
		//ca filter
		sql.append(" AND ");
		sql.append(sqlGenerator.asSql(query.getConservationAreaFilter(), tablePrefix(Waypoint.class)));

		//date filter
		String dFilter = sqlGenerator.toSql(localDateFilter, this);
		if (dFilter != null && dFilter.length() > 0){
			sql.append(" AND ");
			sql.append(dFilter);
		}
		
		//entity filter		
		if (query.getEntityFilter().getType() == EntityFilterType.ALLACTIVE){
			sql.append(" AND ");
			sql.append(tablePrefix(Entity.class) + ".status = '" + Entity.Status.ACTIVE +"' ");
		}else if (query.getEntityFilter().getType() == EntityFilterType.CUSTOM){
			if(query.getEntityFilter().getEntities().size() > 0){
				sql.append(" AND ");
				sql.append(tablePrefix(Entity.class) + ".uuid IN (");
			
				for (Entity e : query.getEntityFilter().getEntities()){
					sql.append("x'" + SmartUtils.encodeHex(e.getUuid()) + "',");
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")");
			}else{
				sql.append(" AND ");
				sql.append(tablePrefix(Entity.class) + ".uuid IS NULL ");
			}
		}
	
	
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		//create index on entity_uuid
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryDataTable + "_entityuuid_idx ON ");
		sql.append(queryDataTable);
		sql.append("(entity_uuid)");
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		
		//update entity values
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			sql = new StringBuilder();
			sql.append("UPDATE " );
			sql.append(queryDataTable);
			sql.append(" SET ");
			sql.append("ea" + SmartUtils.encodeHex(ea.getUuid()));
			sql.append(" = ");
			
			
			if (ea.getDmAttribute().getType() == AttributeType.TEXT || 
					ea.getDmAttribute().getType() == AttributeType.DATE){ 
				sql.append(" ( SELECT string_value FROM ");
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" WHERE ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = ");
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_attribute_uuid = x'" + SmartUtils.encodeHex(ea.getUuid()) + "' )");
				
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN || 
					ea.getDmAttribute().getType() == AttributeType.NUMERIC){
				sql.append(" ( SELECT number_value FROM ");
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" WHERE ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = ");
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_attribute_uuid = x'" + SmartUtils.encodeHex(ea.getUuid()) + "' )");
		
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}else if (ea.getDmAttribute().getType() == AttributeType.LIST ||
					ea.getDmAttribute().getType() == AttributeType.TREE){
				
				StringBuilder sql2 = new StringBuilder();
				sql2.append("SELECT DISTINCT  ");
				if(ea.getDmAttribute().getType() == AttributeType.LIST){
					sql2.append("list_element_uuid ");
				}else{
					//tree
					sql2.append("tree_node_uuid ");
				}
				sql2.append(" FROM ");
				sql2.append(tableNamePrefix(EntityAttributeValue.class));
				sql2.append(" JOIN ");
				sql2.append( tableNamePrefix(Entity.class));
				sql2.append(" ON " +tablePrefix(Entity.class) + ".uuid = " + tablePrefix(EntityAttributeValue.class) + ".entity_uuid");
				sql2.append(" WHERE ");
				sql2.append( tablePrefix(Entity.class));
				sql2.append(".entity_type_uuid = x'" + SmartUtils.encodeHex(query.getEntityType().getUuid()) + "'");
				sql2.append(" AND ");
				sql2.append( tablePrefix(EntityAttributeValue.class));
				sql2.append(".entity_attribute_uuid = x'" + SmartUtils.encodeHex(ea.getUuid()) + "'");
				QueryPlugIn.logSql(sql2.toString());
				ResultSet rs = c.createStatement().executeQuery(sql2.toString());
				
				sql.append(" ( SELECT CASE ");
				
				try{
					while(rs.next()){
						byte[] uuid = rs.getBytes(1);
						String key = Label.getDescription(uuid);
					
						sql.append(" when ");
						sql.append(tablePrefix(EntityAttributeValue.class));
						if(ea.getDmAttribute().getType() == AttributeType.LIST){
							sql.append(".list_element_uuid ");
						}else{
							//tree
							sql.append(".tree_node_uuid ");
						}
						sql.append(" = x'" + SmartUtils.encodeHex(uuid) + "' ");
						
						sql.append("THEN '" + key + "' " );
					}
					
				}finally{
					rs.close();
				}
				sql.append(" ELSE NULL END  FROM ");
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" WHERE ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = ");
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND ");
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_attribute_uuid = x'" + SmartUtils.encodeHex(ea.getUuid()) + "' )");
				
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
	}

	protected void createResultsTable(Connection c) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + queryDataTable + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ca_id varchar(8),"); //$NON-NLS-1$
		sql.append("ca_name varchar(256),"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$ 
		sql.append("wp_source varchar(16),"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double,"); //$NON-NLS-1$
		sql.append("wp_y double,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("entity_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("entity_id varchar(32), "); //$NON-NLS-1$
		sql.append("entity_status varchar(8)"); //$NON-NLS-1$
		
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			sql.append(", ea" + SmartUtils.encodeHex(ea.getUuid()));
			//+ " varchar(1024)"
			if (ea.getDmAttribute().getType() == AttributeType.TEXT ||
					ea.getDmAttribute().getType() == AttributeType.DATE ||
					ea.getDmAttribute().getType() == AttributeType.LIST ||
					ea.getDmAttribute().getType() == AttributeType.TREE){
				sql.append(" varchar(1024)");
			}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN || 
				ea.getDmAttribute().getType() == AttributeType.NUMERIC){
					sql.append(" double");
			}
		}
		sql.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	protected SightingResultItem asQueryResultItem(ResultSet rs,
			Session session) throws SQLException {
		
		SightingResultItem it = new SightingResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		it.setWaypointUuid(rs.getBytes("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWaypointDateTime(rs.getDate("wp_time")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getFloat("wp_direction")); //$NON-NLS-1$
		it.setWaypointDistance(rs.getFloat("wp_distance")); //$NON-NLS-1$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$

		it.setObservationUuid(rs.getBytes("ob_uuid")); //$NON-NLS-1$
		it.setEntityId(rs.getString("entity_id"));
		it.setEntityStatus(EntityType.Status.valueOf(rs.getString("entity_status")));
		// build categories
		byte[] entityUuid = rs.getBytes("entity_uuid");
		it.setEntityUuid(entityUuid);
		
		
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			Object x = rs.getObject("ea" + SmartUtils.encodeHex(ea.getUuid()));
			it.setEntityAttribute("entity:" + SmartUtils.encodeHex(ea.getUuid()), x);
		}
		
		
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < categoryCount; i++) {
			String category = rs.getString("category_" + i); //$NON-NLS-1$
			if (category == null) {
				break;
			}
			categories.add(category);
		}
		it.setCategory(categories.toArray(new String[categories.size()]));
		
		
		return it;
	}

	
	private void addCategoryLabels(Connection c, Session session) throws SQLException {
		
		// add data model category columns
		categoryCount = QueryDataModelManager.getInstance().getActiveDepth();
		
		if (categoryCount < 0){
			//nothing to update
			return;
		}
		
		for (int i = 0; i <= categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		}
		
		Map<Integer, PreparedStatement> num2Statement = new HashMap<Integer, PreparedStatement>();
		String sql = "SELECT DISTINCT OB_CATEGORY_UUID FROM " + queryDataTable;  //$NON-NLS-1$
		QueryPlugIn.logSql(sql);
		ResultSet rs = c.createStatement().executeQuery(sql);
		
		try {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(uuid, session);
				int count = names.length;
				int depth = Math.min(categoryCount + 1, count);	//the full category name may be longer than the number of columns in cross-ca analysis 
				PreparedStatement statement = num2Statement.get(count); //try to reuse already created prepare statement
				if (statement == null) {
					//that means that we didn't create update statement for this number of columns to update -> create one
					StringBuilder colunms = new StringBuilder();
					for (int j = 0; j < depth; j++) {
						if (j > 0){
							colunms.append(", "); //$NON-NLS-1$
						}
						colunms.append("category_").append(j).append("=?"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql = "UPDATE "+queryDataTable+" SET "+colunms.toString()+" where OB_CATEGORY_UUID = ?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					QueryPlugIn.logSql(sql);
					statement = c.prepareStatement(sql);
					
					num2Statement.put(count, statement);
				}
				
				for (int i = 0; i <  depth; i++) {
					statement.setString(i+1, names[i]);
				}
				statement.setBytes( depth+1, uuid);
				statement.executeUpdate();
			}
		} finally {
			rs.close();
		}
	}
	
}
