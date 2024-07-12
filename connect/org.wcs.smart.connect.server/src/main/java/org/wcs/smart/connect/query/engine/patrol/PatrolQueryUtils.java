package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeTreeNode;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

public class PatrolQueryUtils {

	
	public static String getPatrolMembersAsString(Session session, UUID patrolLegUuid, AbstractQueryEngine engine) {
		List<Employee> members = session
				.createQuery("SELECT id.member FROM PatrolLegMember WHERE id.patrolLeg.uuid = :uuid", Employee.class) //$NON-NLS-1$
				.setParameter("uuid", patrolLegUuid) //$NON-NLS-1$
				.list();
		members.sort((a, b) -> engine.getEmployeeName(a).compareTo(engine.getEmployeeName(b)));

		StringJoiner joiner = new StringJoiner(", "); //$NON-NLS-1$
		members.forEach(e -> joiner.add(engine.getEmployeeName(e)));

		String value = joiner.toString();

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
	public static List<String> addPatrolAttributesToQueryResult(String queryDataTable, 
			Connection c, Session session, AbstractQueryEngine engine)
			
					throws SQLException {
		// patrol attributes
		// custom patrol attributes
		List<PatrolAttribute> pattributes = getPatrolAttributes(engine.getCaFilter(), session);
		
		List<String> columns = new ArrayList<>();
		for (PatrolAttribute pattribute : pattributes) {
			String cname = PatrolAttributeQueryColumn.PREFIX + "_" + pattribute.getKeyId(); //$NON-NLS-1$
			String type = "varchar(8200)"; //$NON-NLS-1$
			if (pattribute.getType() == AttributeType.BOOLEAN || pattribute.getType() == AttributeType.NUMERIC) {
				type = "double precision"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.DATE) {
				type = "varchar(10)"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.LIST) {
				type = "varchar(1024)"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.TREE) {
				type = "varchar(1024)"; //$NON-NLS-1$
			} else if (pattribute.getType() == AttributeType.TEXT) {

			} else {
				// attribute type not supported
				continue;
			}
			columns.add(cname);
			
			String query = "ALTER TABLE " + queryDataTable + " ADD " + cname + " " + type; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			c.createStatement().execute(query);

			if (pattribute.getType() == AttributeType.LIST) {
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT distinct "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".list_item_uuid, b.p_uuid"); //$NON-NLS-1$
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(queryDataTable + " b join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(PatrolAttributeValue.class));
				sql.append(" on b.p_uuid = "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_uuid"); //$NON-NLS-1$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(PatrolAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttribute.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_attribute_uuid"); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".list_item_uuid IS NOT NULL "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttribute.class) + ".keyid = ? "); //$NON-NLS-1$
				
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
								UUID liuuid = (UUID)rs.getObject(1);
								PatrolAttributeListItem li = session.get(PatrolAttributeListItem.class, liuuid);
	
								UUID puuid = (UUID)rs.getObject(2);
	
								psUpdate.setString(1, li.getName());
								psUpdate.setObject(2, puuid);
								psUpdate.addBatch();
							}
						}
					}
					psUpdate.executeBatch();
				}

			}else if (pattribute.getType() == AttributeType.TREE) {
					StringBuilder sql = new StringBuilder();
					sql.append("SELECT distinct "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".tree_node_uuid, b.p_uuid"); //$NON-NLS-1$
					sql.append(" FROM "); //$NON-NLS-1$
					sql.append(queryDataTable + " b join "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(PatrolAttributeValue.class));
					sql.append(" on b.p_uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_uuid"); //$NON-NLS-1$
					sql.append(" join "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(PatrolAttribute.class));
					sql.append(" on "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttribute.class) + ".uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_attribute_uuid"); //$NON-NLS-1$
					sql.append(" WHERE "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".tree_node_uuid IS NOT NULL "); //$NON-NLS-1$
					sql.append(" AND "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(PatrolAttribute.class) + ".keyid = ? "); //$NON-NLS-1$
					
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
									UUID liuuid = (UUID)rs.getObject(1);
									PatrolAttributeTreeNode li = session.get(PatrolAttributeTreeNode.class, liuuid);
		
									UUID puuid = (UUID)rs.getObject(2);
		
									psUpdate.setString(1, li.getName());
									psUpdate.setObject(2, puuid);
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
				sql.append(engine.tablePrefix(PatrolAttributeValue.class));
				if (pattribute.getType() == AttributeType.BOOLEAN || pattribute.getType() == AttributeType.NUMERIC) {
					sql.append(".number_value"); //$NON-NLS-1$
				} else if (pattribute.getType() == AttributeType.DATE || pattribute.getType() == AttributeType.TEXT) {
					sql.append(".string_value"); //$NON-NLS-1$
				} else {
					throw new UnsupportedOperationException();
				}
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(PatrolAttributeValue.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(PatrolAttribute.class));
				sql.append(" ON "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttribute.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_attribute_uuid "); //$NON-NLS-1$
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttribute.class) + ".keyid = ? "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(PatrolAttributeValue.class) + ".patrol_uuid = "); //$NON-NLS-1$
				sql.append(queryDataTable + ".p_uuid"); //$NON-NLS-1$
				sql.append(")"); //$NON-NLS-1$
				
				try(PreparedStatement ps = c.prepareStatement(sql.toString())){
					ps.setString(1, pattribute.getKeyId());
					ps.execute();
				}
			}
		}

		return columns;
	}
	
	/**
	 * Adds all the patrol min datetime and max datetime to the query results.
	 * 
	 * 
	 * @param queryDataTable
	 * @param c
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	public static void addPatrolMinMaxDateTime(String queryDataTable, 
			Connection c, Session session, AbstractQueryEngine engine) throws SQLException {
		
		for (String field : new String[]{"p_min_datetime", "p_max_datetime"}) { //$NON-NLS-1$ //$NON-NLS-2$
			StringBuilder sb = new StringBuilder();
			sb.append("ALTER TABLE " + queryDataTable + " ADD COLUMN " + field + " timestamp"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			try(Statement s = c.createStatement()){
				s.execute(sb.toString());
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("WITH mintimes AS ("); //$NON-NLS-1$
		sb.append("SELECT " + engine.tablePrefix(Patrol.class) + ".uuid as p_uuid, "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("min((" + engine.tablePrefix(PatrolLegDay.class) + ".patrol_day || ' ' || " + engine.tablePrefix(PatrolLegDay.class) + ".start_time)::timestamp) as p_min_datetime, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("max((" + engine.tablePrefix(PatrolLegDay.class) + ".patrol_day || ' ' || " + engine.tablePrefix(PatrolLegDay.class) + ".end_time)::timestamp) as p_max_datetime "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" FROM "); //$NON-NLS-1$
		sb.append(queryDataTable + " data "); //$NON-NLS-1$
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(engine.tableNamePrefix(Patrol.class));
		sb.append(" ON data.p_uuid = " + engine.tablePrefix(Patrol.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(engine.tableNamePrefix(PatrolLeg.class));
		sb.append(" ON " + engine.tablePrefix(PatrolLeg.class) + ".patrol_uuid = " + engine.tablePrefix(Patrol.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(engine.tableNamePrefix(PatrolLegDay.class));
		sb.append(" ON " + engine.tablePrefix(PatrolLeg.class) + ".uuid = " + engine.tablePrefix(PatrolLegDay.class) + ".patrol_leg_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append("GROUP BY "+ engine.tablePrefix(Patrol.class) +".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(")"); //$NON-NLS-1$
		sb.append(" UPDATE " + queryDataTable); //$NON-NLS-1$
		sb.append(" SET p_min_datetime = a.p_min_datetime, p_max_datetime = a.p_max_datetime "); //$NON-NLS-1$
		sb.append(" FROM mintimes a "); //$NON-NLS-1$
		sb.append(" WHERE a.p_uuid = " + queryDataTable + ".p_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		try(Statement s = c.createStatement()){
			s.execute(sb.toString());
		}
		
	}
	
	public static List<PatrolAttribute> getPatrolAttributes(ConservationAreaFilter caFilter,
			Session session){
		
		//no cas; no attributes
		if (caFilter.getConservationAreaFilterIds().isEmpty()) return Collections.emptyList();
		
		//single ca
		if (caFilter.getConservationAreaFilterIds().size() == 1) {
			List<PatrolAttribute> pas = QueryFactory.buildQuery(session, PatrolAttribute.class, 
					new Object[] {"conservationArea.uuid", caFilter.getConservationAreaFilterIds().get(0)}).list(); //$NON-NLS-1$
			return pas;
		}
		
		//multi ca - merge attributes
		List<PatrolAttribute> pas = session.createQuery("FROM PatrolAttribute a WHERE a.conservationArea.uuid in (:cas)", //$NON-NLS-1$
				PatrolAttribute.class)
				.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
				.list();
		
		HashMap<String, List<PatrolAttribute>> attributes = new HashMap<>();
		for (PatrolAttribute pa : pas) {
			if (!attributes.containsKey(pa.getKeyId())) {
				attributes.put(pa.getKeyId(), new ArrayList<>());
			}
			attributes.get(pa.getKeyId()).add(pa);			
		}
		
		List<PatrolAttribute> results = new ArrayList<>();
		for (List<PatrolAttribute> items : attributes.values()) {
			PatrolAttribute pa = PatrolUtils.mergeAttributes(items);
			if (pa != null) results.add(pa);
		}
		Collections.sort(results);
		return results;
	}
}
