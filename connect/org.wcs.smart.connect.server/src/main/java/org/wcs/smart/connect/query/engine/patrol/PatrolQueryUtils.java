package org.wcs.smart.connect.query.engine.patrol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
import org.wcs.smart.patrol.query.model.observation.PatrolAttributeQueryColumn;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

public class PatrolQueryUtils {

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
	
	public static List<PatrolAttribute> getPatrolAttributes(ConservationAreaFilter caFilter,
			Session session){
		
		if (caFilter.getConservationAreaFilterIds().isEmpty()) return Collections.emptyList();
		
		if (caFilter.getConservationAreaFilterIds().size() == 1) {
			List<PatrolAttribute> pas = QueryFactory.buildQuery(session, PatrolAttribute.class, 
					new Object[] {"conservationArea.uuid", caFilter.getConservationAreaFilterIds().get(0)}).list(); //$NON-NLS-1$
			return pas;
		}else {
			
			List<PatrolAttribute> pas = session.createQuery("FROM PatrolAttribute a WHERE a.conservationArea.uuid in (:cas)", //$NON-NLS-1$
					PatrolAttribute.class)
					.setParameterList("cas", caFilter.getConservationAreaFilterIds()) //$NON-NLS-1$
					.list();
			
			HashMap<String, PatrolAttribute> attributes = new HashMap<>();
			HashSet<String> toExclude = new HashSet<>();
			
			for (PatrolAttribute pa : pas) {
				PatrolAttribute current = attributes.get(pa.getKeyId());
				if (current == null && !toExclude.contains(pa.getKeyId())) {
					PatrolAttribute temp = new PatrolAttribute();
					temp.setKeyId(pa.getKeyId());
					temp.setIsActive(true);
					temp.setName(pa.getName());
					temp.setType(pa.getType());
					if (pa.getAttributeList() != null) {
						temp.setAttributeList(new ArrayList<>());
						for (PatrolAttributeListItem li : pa.getAttributeList()) {
							PatrolAttributeListItem clone = new PatrolAttributeListItem();
							clone.setIsActive(true);
							clone.setKeyId(li.getKeyId());
							clone.setName(li.getName());
							clone.setListOrder(li.getListOrder());
							temp.getAttributeList().add(clone);
						}
					}
					attributes.put(pa.getKeyId(), temp);
				}else {
					if (pa.getType() != current.getType()) {
						toExclude.add(pa.getKeyId());
						attributes.remove(pa.getKeyId());
					}else {
						//merge list items
						if (pa.getAttributeList() != null) {
							HashMap<String, PatrolAttributeListItem> items = new HashMap<>();
							for (PatrolAttributeListItem i : current.getAttributeList()) items.put(i.getKeyId(), i);
							
							for (PatrolAttributeListItem i : pa.getAttributeList()) {
								if (!items.containsKey(i.getKeyId())) {
									PatrolAttributeListItem clone = new PatrolAttributeListItem();
									clone.setIsActive(true);
									clone.setKeyId(i.getKeyId());
									clone.setName(i.getName());
									clone.setListOrder(i.getListOrder());
									items.put(clone.getKeyId(), clone);
								}
							}
							
							current.getAttributeList().clear();
							current.getAttributeList().addAll(items.values());
						}
					}
				}
			}
			
			pas = new ArrayList<>(attributes.values());
			for (PatrolAttribute pa : pas) {
				if (pa.getAttributeList() != null) Collections.sort(pa.getAttributeList(), (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			}
			Collections.sort(pas, (a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			
			return pas;
		}
	}
}
