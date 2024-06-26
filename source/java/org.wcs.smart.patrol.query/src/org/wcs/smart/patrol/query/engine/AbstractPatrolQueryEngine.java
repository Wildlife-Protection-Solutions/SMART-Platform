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
package org.wcs.smart.patrol.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.query.hibernate.PatrolQueryHibernateManager;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AbstractPatrolQueryEngine extends AbstractQueryEngine implements IPatrolQueryEngine{
	
	static {
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(PatrolWaypoint.class, "pwp"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Team.class, "smart.team"); //$NON-NLS-1$
		tablePrefix.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tablePrefix.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttribute.class, "spa"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttributeValue.class, "spav"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttributeListItem.class, "spali"); //$NON-NLS-1$

	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	static {
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(PatrolWaypoint.class, "smart.patrol_waypoint"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Team.class, "smart.team"); //$NON-NLS-1$
		tableNames.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tableNames.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		tableNames.put(PatrolAttribute.class, "smart.patrol_attribute"); //$NON-NLS-1$
		tableNames.put(PatrolAttributeValue.class, "smart.patrol_attribute_value"); //$NON-NLS-1$
		tableNames.put(PatrolAttributeListItem.class, "smart.patrol_attribute_list"); //$NON-NLS-1$
	}


	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	@Override
	public IFilterProcessor getFilterProcessor(FilterType filterType, 
			String queryDataTable, Query query){
		if (filterType == FilterType.OBSERVATION){
			return new FilterProcessor(queryDataTable, this, query);
		}else if (filterType == FilterType.GROUP) {
			return new WaypointGroupFilterProcessor(queryDataTable, this, query);
		}else{
			return new WaypointFilterProcessor(queryDataTable, this, query);
		}
	}
	
	//TODO: don't cache everything?
	private HashMap<UUID,String> membersCache = new HashMap<>();
	protected String getPatrolMembersAsString(Session session, UUID patrolLegUuid) {
		if (membersCache.containsKey(patrolLegUuid)) return membersCache.get(patrolLegUuid);
			
		List<Employee> members = session.createQuery("SELECT id.member FROM PatrolLegMember WHERE id.patrolLeg.uuid = :uuid", Employee.class) //$NON-NLS-1$
		.setParameter("uuid", patrolLegUuid) //$NON-NLS-1$
		.list();
		members.sort((a,b)->getEmployeeName(a).compareTo(getEmployeeName(b)));
			
		StringJoiner joiner = new StringJoiner(", "); //$NON-NLS-1$
		members.forEach(e->joiner.add(getEmployeeName(e)));
			
		String value = joiner.toString();
		membersCache.put(patrolLegUuid, value);
		return value;
			
	}
	
	/**
	 * Adds all the custom patrol query attributes to the results table and populates the values. Returns
	 * the list of added columns;
	 * @param queryDataTable
	 * @param c
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	protected List<String> addPatrolAttributesToQueryResult(String queryDataTable,			
			Connection c, Session session)
			throws SQLException {
		// patrol attributes
		// custom patrol attributes
		List<PatrolAttribute> pattributes = PatrolQueryHibernateManager.getInstance()
				.getCustomPatrolAttributes(session);
		List<String> columns = new ArrayList<>();
		for (PatrolAttribute pattribute : pattributes) {
			String cname = PatrolAttributeQueryColumn.PREFIX + "_" + pattribute.getKeyId(); //$NON-NLS-1$
			String type = "varchar(8200)"; //$NON-NLS-1$
			if (pattribute.getType() == AttributeType.BOOLEAN || pattribute.getType() == AttributeType.NUMERIC) {
				type = "double"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.DATE) {
				type = "varchar(10)"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.LIST) {
				type = "varchar(1024)"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.TEXT) {

			} else {
				// attribute type not supported
				continue;
			}
			columns.add(cname);
			
			String query = "ALTER TABLE " + queryDataTable + " ADD " + cname + " " + type; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			QueryPlugIn.logSql(query);
			c.createStatement().execute(query);

			if (pattribute.getType() == AttributeType.LIST) {
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT distinct "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".list_item_uuid, b.p_uuid"); //$NON-NLS-1$
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(queryDataTable + " b join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(PatrolAttributeValue.class));
				sql.append(" on b.p_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".patrol_uuid"); //$NON-NLS-1$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(PatrolAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttribute.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".patrol_attribute_uuid"); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".list_item_uuid IS NOT NULL "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttribute.class) + ".keyid = ? "); //$NON-NLS-1$
				
				StringBuilder sb = new StringBuilder();
				sb.append("UPDATE "); //$NON-NLS-1$
				sb.append(queryDataTable);
				sb.append(" SET "); //$NON-NLS-1$
				sb.append(cname);
				sb.append(" = ? "); //$NON-NLS-1$
				sb.append(" WHERE p_uuid = ?"); //$NON-NLS-1$

				try (PreparedStatement psUpdate = c.prepareStatement(sb.toString())) {
					try(PreparedStatement psQuery = c.prepareStatement(sql.toString())){
						psQuery.setString(1, pattribute.getKeyId());						
						try (ResultSet rs = psQuery.executeQuery()) {
							while (rs.next()) {
								UUID liuuid = UuidUtils.byteToUUID(rs.getBytes(1));
								PatrolAttributeListItem li = session.get(PatrolAttributeListItem.class, liuuid);
	
								UUID puuid = UuidUtils.byteToUUID(rs.getBytes(2));
	
								psUpdate.setString(1, li.getName());
								psUpdate.setBytes(2, UuidUtils.uuidToByte(puuid));
								psUpdate.addBatch();
							}
						}
					}
					psUpdate.executeBatch();
				}

			} else {
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE " + queryDataTable); //$NON-NLS-1$
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(cname);
				sql.append(" = ("); //$NON-NLS-1$
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class));
				if (pattribute.getType() == AttributeType.BOOLEAN || pattribute.getType() == AttributeType.NUMERIC) {
					sql.append(".number_value"); //$NON-NLS-1$
				} else if (pattribute.getType() == AttributeType.DATE || pattribute.getType() == AttributeType.TEXT) {
					sql.append(".string_value"); //$NON-NLS-1$
				} else {
					throw new UnsupportedOperationException();
				}
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(PatrolAttributeValue.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append(tableNamePrefix(PatrolAttribute.class));
				sql.append(" ON "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttribute.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".patrol_attribute_uuid "); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttribute.class) + ".keyid = ? "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(tablePrefix(PatrolAttributeValue.class) + ".patrol_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable + ".p_uuid"); //$NON-NLS-1$
				sql.append(")"); //$NON-NLS-1$
				
				QueryPlugIn.logSql(sql.toString());
				
				try(PreparedStatement ps = c.prepareStatement(sql.toString())){
					ps.setString(1, pattribute.getKeyId());
					ps.execute();
				}
			}
		}

		return columns;
	}
	
	protected void addPatrolMinMaxDateTime(String queryDataTable,			
			Connection c, Session session)
			throws SQLException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE " + queryDataTable + " ADD COLUMN p_min_datetime timestamp"); //$NON-NLS-1$ //$NON-NLS-2$
		
		QueryPlugIn.logSql(sb.toString());
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
		sb = new StringBuilder();
		sb.append("ALTER TABLE " + queryDataTable + " ADD COLUMN p_max_datetime timestamp"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sb.toString());
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
		String datetimetable = super.createTempTableName();
		
		sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(datetimetable);
		sb.append("(p_uuid char(16) for bit data, p_min_datetime timestamp, p_max_datetime timestamp)"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sb.toString());
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
		
		sb = new StringBuilder();
		sb.append("INSERT INTO " ); //$NON-NLS-1$
		sb.append(datetimetable);
		sb.append("(p_uuid, p_min_datetime, p_max_datetime) "); //$NON-NLS-1$
		sb.append("SELECT " + tablePrefix(Patrol.class) + ".uuid, "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("min(timestamp(" + tablePrefix(PatrolLegDay.class) + ".patrol_day || ' ' || " + tablePrefix(PatrolLegDay.class) + ".start_time)) as p_min_datetime, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("max(timestamp(" + tablePrefix(PatrolLegDay.class) + ".patrol_day || ' ' || " + tablePrefix(PatrolLegDay.class) + ".end_time)) as p_max_datetime "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryDataTable + " data "); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Patrol.class));
		sb.append(" ON data.p_uuid = " + tablePrefix(Patrol.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(PatrolLeg.class));
		sb.append(" ON " + tablePrefix(PatrolLeg.class) + ".patrol_uuid = " + tablePrefix(Patrol.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(PatrolLegDay.class));
		sb.append(" ON " + tablePrefix(PatrolLeg.class) + ".uuid = " + tablePrefix(PatrolLegDay.class) + ".patrol_leg_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("GROUP BY "+ tablePrefix(Patrol.class) +".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		

		QueryPlugIn.logSql(sb.toString());
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
		
		for(String field : new String[] {"p_min_datetime", "p_max_datetime"}){ //$NON-NLS-1$ //$NON-NLS-2$
			sb = new StringBuilder();
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(queryDataTable);
			sb.append(" SET " + field + " = (SELECT " + field ); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" FROM "); //$NON-NLS-1$
			sb.append(datetimetable + " a "); //$NON-NLS-1$
			sb.append("WHERE a.p_uuid = " + queryDataTable + ".p_uuid )"); //$NON-NLS-1$ //$NON-NLS-2$
		
			QueryPlugIn.logSql(sb.toString());
			try(Statement s = c.createStatement()){
				s.execute(sb.toString());
			}
		}
		
		sb = new StringBuilder();
		sb.append("DROP TABLE " + datetimetable); //$NON-NLS-1$
		QueryPlugIn.logSql(sb.toString());
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
				
	}

}
