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
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.Status;
import org.wcs.smart.entity.query.EntityFilter.EntityFilterType;
import org.wcs.smart.entity.query.SightingQueryColumn.FixedColumns;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.UuidUtils;

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
		tablePrefix.put(EntityAttribute.class, "ea"); //$NON-NLS-1$
	}

	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Entity.class, "smart.entity"); //$NON-NLS-1$
		tableNames.put(EntityType.class, "smart.entity_type"); //$NON-NLS-1$
		tableNames.put(EntityAttributeValue.class, "smart.entity_attribute_value"); //$NON-NLS-1$
		tableNames.put(EntityAttribute.class, "smart.entity_attribute"); //$NON-NLS-1$
	}

	private String queryDataTable;

	private EntitySightingQuery query;
	private DateFilter localDateFilter;
	private int categoryCount;

	@Override
	public boolean canExecute(String querytype) {
		return EntitySightingQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{

		this.query = (EntitySightingQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
		final IProgressMonitor monitor = (IProgressMonitor) parameters.get(IProgressMonitor.class.getName());
		
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
				monitor.beginTask(Messages.DerbyEntitySightingEngine_ProgressTaskName, 3);

				monitor.subTask(Messages.DerbyEntitySightingEngine_Progress1);
				createResultsTable(c);
				monitor.worked(1);

				monitor.subTask(Messages.DerbyEntitySightingEngine_Progress2);
				populateDataTable(c, session);
				monitor.worked(1);
				
				monitor.subTask(Messages.DerbyEntitySightingEngine_Progress3);
				addCategoryLabels(c, session);
				monitor.worked(1);
				
				
				//setting waypoint count
			
				try(ResultSet rs = c.createStatement().executeQuery("select count(*) from " + queryDataTable + "")) { //$NON-NLS-1$ //$NON-NLS-2$
					if (rs.next()) {
						result.setItemCount(rs.getInt(1));
					}
				 }

				if (monitor.isCanceled()){
					dropTemporaryTables(c, monitor.isCanceled());
					result.setItemCount(0);
				}
				
				c.commit();
			}
		});
		if (monitor.isCanceled()){
			return null;
		}
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


	protected void populateDataTable(Connection c, Session session) throws SQLException {
		
		String lastSightingReport = null;
		
		//custom last sighting date filter
		if (query.getDateFilter().getDateFilterOption() == LastSightingDateFilter.INSTANCE){
			lastSightingReport = createTempTableName();;
			
			StringBuilder s = new StringBuilder();
			s.append("CREATE TABLE "); //$NON-NLS-1$
			s.append(lastSightingReport);
			s.append("(last_sighting timestamp, entity char(16) for bit data)"); //$NON-NLS-1$
			QueryPlugIn.logSql(s.toString());
			c.createStatement().execute(s.toString());
			
			//find the last sighting date for each entity
			s = new StringBuilder();
			s.append("INSERT INTO " + lastSightingReport); //$NON-NLS-1$
			s.append("(last_sighting, entity) "); //$NON-NLS-1$
			s.append("SELECT "); //$NON-NLS-1$
			s.append("max(" + tablePrefix(Waypoint.class) + ".datetime) as last_sighting,"); //$NON-NLS-1$ //$NON-NLS-2$
			s.append(tablePrefix(Entity.class) + ".uuid"); //$NON-NLS-1$
			s.append(" FROM "); //$NON-NLS-1$
			s.append(tableNamePrefix(Waypoint.class));
			s.append(" JOIN "); //$NON-NLS-1$
			s.append(tableNamePrefix(WaypointObservation.class));
			s.append(" ON " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			s.append(" join "); //$NON-NLS-1$
			s.append(tableNamePrefix(WaypointObservationAttribute.class));
			s.append(" on " + tablePrefix(WaypointObservation.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			s.append(" join "); //$NON-NLS-1$
			s.append(tableNamePrefix(Entity.class));
			s.append(" on " + tablePrefix(Entity.class) + ".attribute_list_item_uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			s.append(" join "); //$NON-NLS-1$
			s.append(tableNamePrefix(EntityType.class));
			s.append(" on " + tablePrefix(EntityType.class) + ".uuid = " + tablePrefix(Entity.class) + ".entity_type_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			s.append(" WHERE "); //$NON-NLS-1$
			s.append(tablePrefix(EntityType.class) + ".keyid = '" +query.getEntityType().getKeyId() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			
			//entity filter		
			if (query.getEntityFilter().getType() == EntityFilterType.ALLACTIVE){
				s.append(" AND "); //$NON-NLS-1$
				s.append(tablePrefix(Entity.class) + ".status = '" + Status.ACTIVE +"' "); //$NON-NLS-1$ //$NON-NLS-2$
			}else if (query.getEntityFilter().getType() == EntityFilterType.CUSTOM){
				if(query.getEntityFilter().getEntities().size() > 0){
					s.append(" AND "); //$NON-NLS-1$
					s.append(tablePrefix(Entity.class) + ".uuid IN ("); //$NON-NLS-1$
				
					for (Entity e : query.getEntityFilter().getEntities()){
						s.append("x'" + UuidUtils.uuidToString(e.getUuid()) + "',"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					s.deleteCharAt(s.length() - 1);
					s.append(")"); //$NON-NLS-1$
				}else{
					s.append(" AND "); //$NON-NLS-1$
					s.append(tablePrefix(Entity.class) + ".uuid IS NULL "); //$NON-NLS-1$
				}
			}			
			s.append("GROUP BY " + tablePrefix(Entity.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(s.toString());
			c.createStatement().execute(s.toString());
		}
		
		
		clearParameters();
		StringBuilder sql = new StringBuilder();
		sql.append(" INSERT INTO " + queryDataTable + " "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" (ca_uuid,ca_id,ca_name,wp_uuid,wp_source,wp_id,wp_x,wp_y,wp_direction,wp_distance,wp_time,wp_comment,ob_uuid,ob_observer_uuid,ob_category_uuid,entity_uuid,entity_id,entity_status) "); //$NON-NLS-1$
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
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Entity.class) + ".status "); //$NON-NLS-1$
		
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Waypoint.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(ConservationArea.class));
		sql.append(" on " + tablePrefix(Waypoint.class) + ".ca_uuid = " + tablePrefix(ConservationArea.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservation.class));
		sql.append(" on " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(WaypointObservation.class) + ".wp_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservationAttribute.class));
		sql.append(" on " + tablePrefix(WaypointObservation.class) + ".uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Entity.class));
		sql.append(" on " + tablePrefix(Entity.class) + ".attribute_list_item_uuid = " + tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(EntityType.class));
		sql.append(" on " + tablePrefix(EntityType.class) + ".uuid = " + tablePrefix(Entity.class) + ".entity_type_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		if (lastSightingReport != null){
			sql.append(" join "); //$NON-NLS-1$
			sql.append(lastSightingReport + " ll"); //$NON-NLS-1$
			sql.append(" on ll.last_sighting = " + tablePrefix(Waypoint.class) + ".datetime"); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" AND ll.entity = " + tablePrefix(Entity.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		sql.append(" WHERE "); //$NON-NLS-1$
		
		DerbyFilterToSqlGenerator sqlGenerator = new DerbyFilterToSqlGenerator();
		
		//entity type
		String p1 = addParameterValue(query.getEntityType().getKeyId());
		sql.append(tablePrefix(EntityType.class) + ".keyid = " + p1); //$NON-NLS-1$
		 
		
		//ca filter
		sql.append(" AND "); //$NON-NLS-1$
		ConservationAreaFilter cafilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
		sql.append(sqlGenerator.asSql(cafilter, tablePrefix(Waypoint.class), this));

		//date filter
		if (lastSightingReport == null){
			String dFilter = sqlGenerator.toSql(localDateFilter, this);
			if (dFilter != null && dFilter.length() > 0){
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(dFilter);
			}
		}
		
		
		//entity filter		
		if (query.getEntityFilter().getType() == EntityFilterType.ALLACTIVE){
			String p2 = addParameterValue(Status.ACTIVE.name());
			sql.append(" AND "); //$NON-NLS-1$
			sql.append(tablePrefix(Entity.class) + ".status = " + p2); //$NON-NLS-1$
		}else if (query.getEntityFilter().getType() == EntityFilterType.CUSTOM){
			if(query.getEntityFilter().getEntities().size() > 0){
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(tablePrefix(Entity.class) + ".uuid IN ("); //$NON-NLS-1$
			
				for (Entity e : query.getEntityFilter().getEntities()){
					String p2 = addParameterValue(e.getUuid());
					sql.append(p2 + ","); //$NON-NLS-1$
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")"); //$NON-NLS-1$
			}else{
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(tablePrefix(Entity.class) + ".uuid IS NULL "); //$NON-NLS-1$
			}
		}
	
	
		QueryPlugIn.logSql(sql.toString());
		parseQueryString(c, sql.toString()).executeUpdate();
		
		//create index on entity_uuid
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + queryDataTable + "_entityuuid_idx ON "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(queryDataTable);
		sql.append("(entity_uuid)"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		//update observer
		sql = new StringBuilder();
		sql.append("ALTER TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append (" ADD COLUMN ob_observer VARCHAR(512)"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		sql = new StringBuilder();
		sql.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		QueryPlugIn.logSql(sql.toString());

		
		String updateSql = "UPDATE "+queryDataTable+" SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		QueryPlugIn.logSql(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try(ResultSet rs1 = c.createStatement().executeQuery(sql.toString())) {
			while (rs1.next()) {
				byte[] uuid = rs1.getBytes(1);
				String name = getEmployeeName(UuidUtils.byteToUUID(uuid), session);
						
				if (name != null) {
					observerSt.setString(1, name);
					observerSt.setBytes(2, uuid);
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100){
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		}
		
		//update entity values
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			clearParameters();
			sql = new StringBuilder();
			sql.append("UPDATE " ); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET "); //$NON-NLS-1$
			sql.append("ea_" + ea.getKeyId() ); //$NON-NLS-1$
			sql.append(" = "); //$NON-NLS-1$
			
			
			if (ea.getDmAttribute().getType() == AttributeType.TEXT || 
					ea.getDmAttribute().getType() == AttributeType.DATE){ 
				sql.append(" ( SELECT string_value FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class) + ".uuid "); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class));
				p1 = addParameterValue(ea.getKeyId());
				sql.append(".keyid = " + p1 + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				 
				QueryPlugIn.logSql(sql.toString());
				parseQueryString(c, sql.toString()).executeUpdate();
				
			}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN || 
					ea.getDmAttribute().getType() == AttributeType.NUMERIC){
				sql.append(" ( SELECT number_value FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class) + ".uuid "); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class));
				p1 = addParameterValue(ea.getKeyId());
				sql.append(".keyid = " + p1 + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				
				QueryPlugIn.logSql(sql.toString());
				parseQueryString(c, sql.toString()).executeUpdate();
			}else if (ea.getDmAttribute().getType() == AttributeType.LIST ||
					ea.getDmAttribute().getType() == AttributeType.TREE){
				
				StringBuilder sql2 = new StringBuilder();
				sql2.append("SELECT DISTINCT  "); //$NON-NLS-1$
				if(ea.getDmAttribute().getType() == AttributeType.LIST){
					sql2.append("list_element_uuid "); //$NON-NLS-1$
				}else{
					//tree
					sql2.append("tree_node_uuid "); //$NON-NLS-1$
				}
				sql2.append(" FROM "); //$NON-NLS-1$
				sql2.append(tableNamePrefix(EntityAttributeValue.class));
				sql2.append(" JOIN "); //$NON-NLS-1$
				sql2.append( tableNamePrefix(Entity.class));
				sql2.append(" ON " +tablePrefix(Entity.class) + ".uuid = " + tablePrefix(EntityAttributeValue.class) + ".entity_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql2.append(" join "); //$NON-NLS-1$
				sql2.append(tableNamePrefix(EntityAttribute.class));
				sql2.append(" on "); //$NON-NLS-1$
				sql2.append(tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid = "); //$NON-NLS-1$
				sql2.append(tablePrefix(EntityAttribute.class) + ".uuid "); //$NON-NLS-1$
				
				sql2.append(" join "); //$NON-NLS-1$
				sql2.append(tableNamePrefix(EntityType.class));
				sql2.append(" on "); //$NON-NLS-1$
				sql2.append(tablePrefix(Entity.class) + ".entity_type_uuid = "); //$NON-NLS-1$
				sql2.append(tablePrefix(EntityType.class) + ".uuid "); //$NON-NLS-1$
				
				sql2.append(" WHERE "); //$NON-NLS-1$
				sql2.append( tablePrefix(EntityType.class));
				sql2.append(".keyid = ? "); //$NON-NLS-1$
				sql2.append(" AND "); //$NON-NLS-1$
				sql2.append(tablePrefix(EntityAttribute.class));
				sql2.append(".keyid = ? "); //$NON-NLS-1$
				
				sql.append(" ( SELECT CASE "); //$NON-NLS-1$
				
				QueryPlugIn.logSql(sql2.toString());
				PreparedStatement ps = c.prepareStatement(sql2.toString());
				ps.setObject(1, query.getEntityType().getKeyId());
				ps.setObject(2, ea.getKeyId());
				boolean toUpdate = false;
				try(ResultSet rs = ps.executeQuery()){
					while(rs.next()){
						byte[] uuid = rs.getBytes(1);
						String key = Label.getDescription(UuidUtils.byteToUUID(uuid), session);
					
						sql.append(" when "); //$NON-NLS-1$
						sql.append(tablePrefix(EntityAttributeValue.class));
						if(ea.getDmAttribute().getType() == AttributeType.LIST){
							sql.append(".list_element_uuid "); //$NON-NLS-1$
						}else{
							//tree
							sql.append(".tree_node_uuid "); //$NON-NLS-1$
						}
						p1 = addParameterValue( uuid );
						sql.append(" = " + p1 + " "); //$NON-NLS-1$ //$NON-NLS-2$
						p1 = addParameterValue(key);
						sql.append("THEN cast(" + p1 + " as varchar(" + Label.MAX_LENGTH + ")) " ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						
						toUpdate = true;
					}
					
				}
				sql.append(" ELSE NULL END  FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttributeValue.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(EntityAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class) + ".entity_attribute_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class) + ".uuid "); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttributeValue.class));
				sql.append(".entity_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable);
				sql.append(".entity_uuid AND "); //$NON-NLS-1$
				sql.append(tablePrefix(EntityAttribute.class));
				p1 = addParameterValue(ea.getKeyId());
				sql.append(".keyid = " + p1 + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				
				
				if (toUpdate){
					QueryPlugIn.logSql(sql.toString());
					parseQueryString(c, sql.toString()).executeUpdate();
				}
			}
		}
		
	}

	protected void createResultsTable(Connection c) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + queryDataTable + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("ca_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append(FixedColumns.CA_ID.dbColName + " varchar(8),"); //$NON-NLS-1$
		sql.append(FixedColumns.CA_NAME.dbColName + " varchar(256),"); //$NON-NLS-1$
		sql.append("wp_uuid char(16) for bit data,"); //$NON-NLS-1$ 
		sql.append(FixedColumns.WAYPOINT_SOURCE.dbColName + " varchar(16),"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_ID.dbColName + " integer,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_X.dbColName + " double,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_Y.dbColName + " double,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_DIRECTION.dbColName + " real,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_DISTANCE.dbColName + " real,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_TIME.dbColName + " timestamp,"); //$NON-NLS-1$
		sql.append(FixedColumns.WAYPOINT_COMMENT.dbColName + " varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_observer_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("ob_category_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append("entity_uuid char(16) for bit data,"); //$NON-NLS-1$
		sql.append(FixedColumns.ENTITY_ID.dbColName + " varchar(32), "); //$NON-NLS-1$
		sql.append(FixedColumns.ENTITY_STATUS.dbColName + " varchar(8)"); //$NON-NLS-1$
		
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			sql.append(", ea_" + ea.getKeyId()); //$NON-NLS-1$
			if (ea.getDmAttribute().getType() == AttributeType.TEXT ||
					ea.getDmAttribute().getType() == AttributeType.DATE ||
					ea.getDmAttribute().getType() == AttributeType.LIST ||
					ea.getDmAttribute().getType() == AttributeType.TREE){
				sql.append(" varchar(1024)"); //$NON-NLS-1$
			}else if (ea.getDmAttribute().getType() == AttributeType.BOOLEAN || 
				ea.getDmAttribute().getType() == AttributeType.NUMERIC){
					sql.append(" double"); //$NON-NLS-1$
			}
		}
		sql.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	protected SightingResultItem asQueryResultItem(ResultSet rs,
			Session session) throws SQLException {
		
		SightingResultItem it = new SightingResultItem();
		it.setConservationAreaId(rs.getString(FixedColumns.CA_ID.dbColName)); 
		it.setConservationAreaName(rs.getString(FixedColumns.CA_NAME.dbColName)); 
		it.setSourceId(rs.getString(FixedColumns.WAYPOINT_SOURCE.dbColName)); 
		it.setWaypointUuid(UuidUtils.byteToUUID(rs.getBytes("wp_uuid"))); //$NON-NLS-1$
		it.setWaypointId(rs.getInt(FixedColumns.WAYPOINT_ID.dbColName)); 
		it.setWaypointX(rs.getDouble(FixedColumns.WAYPOINT_X.dbColName)); 
		it.setWaypointY(rs.getDouble(FixedColumns.WAYPOINT_Y.dbColName)); 
		it.setWaypointDateTime(rs.getTimestamp(FixedColumns.WAYPOINT_TIME.dbColName));
		it.setWaypointDirection(rs.getFloat(FixedColumns.WAYPOINT_DIRECTION.dbColName)); 
		it.setWaypointDistance(rs.getFloat(FixedColumns.WAYPOINT_DISTANCE.dbColName)); 
		it.setWaypointComment(rs.getString(FixedColumns.WAYPOINT_COMMENT.dbColName));
		it.setWaypointObserver(rs.getString(FixedColumns.WAYPOINT_OBSERVER.dbColName)); 
		it.setObservationUuid(UuidUtils.byteToUUID(rs.getBytes("ob_uuid"))); //$NON-NLS-1$
		it.setEntityId(rs.getString(FixedColumns.ENTITY_ID.dbColName));
		it.setEntityStatus(Status.valueOf(rs.getString(FixedColumns.ENTITY_STATUS.dbColName))); 
		// build categories
		byte[] entityUuid = rs.getBytes("entity_uuid"); //$NON-NLS-1$
		it.setEntityUuid(UuidUtils.byteToUUID(entityUuid));
		
		
		for (EntityAttribute ea : query.getEntityType().getAttributes()){
			Object x = rs.getObject("ea_" + ea.getKeyId()); //$NON-NLS-1$
			if (x != null && ea.getDmAttribute().getType() == AttributeType.DATE){
				x = java.sql.Date.valueOf((String)x);
			}
			it.setEntityAttribute("entity:" + ea.getKeyId(), x); //$NON-NLS-1$
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
		
		
		try (ResultSet rs = c.createStatement().executeQuery(sql)){
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid == null)
					continue;
				String[] names = getCategoryLabels(UuidUtils.byteToUUID(uuid), session);
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
		}
	}
}
