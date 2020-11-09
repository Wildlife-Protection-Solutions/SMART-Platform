package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.query.model.ISamplingUnitResultItem;
import org.wcs.smart.er.query.model.ISurveyQueryResultItem;

public class ErSurveyQueryResultSet  {


//	
//
//	protected HashMap<UUID, HashMap<String, Object>> getResultsAttributes(
//			ResultSet rs, Session s) throws SQLException {
//		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
//		/*
//		 * 1 OB_UUID 2 KEYID 3 NUMBER_VALUE 4 STRING_VALUE 5 LIST_VALUE 6
//		 * TREE_VALUE 7 P_CA_UUID
//		 */
//		while (rs.next()) {
//			UUID obUuid = (UUID) rs.getObject(1);
//
//			if (obUuid == null)
//				continue;
//			HashMap<String, Object> attributes = attrMap.get(obUuid);
//			if (attributes == null) {
//				attributes = new HashMap<String, Object>();
//				attrMap.put(obUuid, attributes);
//			}
//			String key = rs.getString(2);
//			if (key != null) {
//				Object value = getAttributeValue(rs, s);
//				attributes.put(key, value);
//			}
//		}
//		return attrMap;
//	}
//
//	protected Object getAttributeValue(ResultSet rs, Session session)
//			throws SQLException {
//		/*
//		 * 1 OB_UUID 2 KEYID 3 NUMBER_VALUE 4 STRING_VALUE 5 LIST_VALUE 6
//		 * TREE_VALUE 7 P_CA_UUID
//		 */
//		if (rs.getObject(3) != null) {
//			return rs.getDouble(3);
//		}
//		String result = rs.getString(4); // string
//		if (result != null) {
//			return result;
//		}
//		result = rs.getString(5); // list
//		if (result != null) {
//			return result;
//		}
//		result = rs.getString(6); // tree
//		if (result != null) {
//			return result;
//		}
//		return null;
//	}

	public static void attachMissionProperties(List<? extends ISurveyQueryResultItem> result, Connection c, Session session) throws SQLException {

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT mpv.mission_uuid, ma.keyid, mpv.number_value,  ");
		attrSql.append(" mpv.string_value, mpv.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.mission_attribute ma ");
		attrSql.append(" join smart.mission_property_value mpv on mpv.mission_attribute_uuid = ma.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE mpv.mission_uuid IN ("); //$NON-NLS-1$

		
		List<UUID> uuids = new ArrayList<UUID>();
		for (ISurveyQueryResultItem irt : result) {
			uuids.add( irt.getMissionUuid() );
			attrSql.append("?,"); //$NON-NLS-1$
		}

		if (uuids.isEmpty()) return; //no missions
		attrSql.deleteCharAt(attrSql.length() - 1);
		attrSql.append(')');
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		for (int i = 0; i < uuids.size(); i ++){
			ps.setObject(i+1, uuids.get(i));
		}
		try (ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				UUID muuid = (UUID)rs.getObject(1);
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);

				for (ISurveyQueryResultItem it : result) {
					if (muuid.equals(it.getMissionUuid())) {
						if (rs.getObject(3) != null) {
							it.addMissionPropertyValue(key, dvalue);
						} else if (svalue != null) {
							it.addMissionPropertyValue(key, svalue);
						} else if (rs.getObject(5) != null) {
							it.addMissionPropertyValue(key,((MissionAttributeListItem) session.load(MissionAttributeListItem.class,(UUID)rs.getObject(5))).getName());
					
						}
					}
				}

			}
		}
	}

	public static void attachSamplingUnitAttributes(List<? extends ISamplingUnitResultItem> result,
			Connection c, Session session) throws SQLException {

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT suav.su_uuid, sua.keyid, suav.number_value, suav.string_value, suav.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.sampling_unit_attribute sua join smart.sampling_unit_attribute_value suav"); //$NON-NLS-1$
		attrSql.append(" on suav.su_attribute_uuid = sua.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE suav.su_uuid IN ("); //$NON-NLS-1$

		List<UUID> uuids = new ArrayList<UUID>();
		for (ISamplingUnitResultItem irt : result) {
			uuids.add( irt.getSamplingUnitUuid() );
			attrSql.append("?,"); //$NON-NLS-1$
		}

		if (uuids.isEmpty()) return; //no missions
		attrSql.deleteCharAt(attrSql.length() - 1);
		attrSql.append(')');

		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		for (int i = 0; i < uuids.size(); i ++){
			ps.setObject(i+1, uuids.get(i));
		}
		
		try (ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				UUID muuid = (UUID)rs.getObject(1);
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);

				for (ISamplingUnitResultItem it : result) {
					if (muuid.equals(it.getSamplingUnitUuid())) {
						if (rs.getObject(3) != null) {
							it.addSamplingUnitAttributeValue(key, dvalue);
						} else if (svalue != null) {
							it.addSamplingUnitAttributeValue(key, svalue);
						} else if (rs.getObject(5) != null) {
							String value = ((SamplingUnitAttributeListItem) session.load(SamplingUnitAttributeListItem.class,(UUID)rs.getObject(5))).getName();
							it.addSamplingUnitAttributeValue(key, value);
						}
					}
				}
			}
		}
	}
	
	
//	@Override
//	public void updateSortColumnGeneral(Session session, String queryDataTable, ConservationAreaFilter caFilter, String value, String typePrefix, 
//			String tableListSuffix, String tableTreeSuffix, String uuidColumn, boolean hasExtraTables) throws SQLException {
//		if(!hasExtraTables){
//			//I don't know how to sort these query types, they don't use temp tables so we can't use the same method as the rest.
//			throw new UnsupportedOperationException("Sorting not suppported for this Query Type"); //$NON-NLS-1$
//		}
//		if (!hasSortColumns) {
//			// add the sort columns
//			session.createNativeQuery("ALTER TABLE " + queryDataTable + " add column sortKeyDbl float").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
//			session.createNativeQuery("ALTER TABLE " + queryDataTable + " add column sortKeyTxt varchar(1024)").executeUpdate(); //$NON-NLS-1$ //$NON-NLS-2$
//			hasSortColumns = true;
//		}
//
//		
//		String key = sortColumn; //This is a bit horrible since users have to send in the attribute key of the column they want to sort. But I don't have a better solution at this point.
//
//		//TODO: this will not work for CCAA
//		Attribute.AttributeType type = QueryManager.INSTANCE.getAttributeType(session, key, caFilter); // session will not be closed on purpose
//
//		
//		if(type == null){
//			//lets see if this is a mission attribute
//			String tableSortField = "sortKeyTxt"; //$NON-NLS-1$
//			if (sortColumn.toLowerCase(Locale.ROOT).startsWith("ma_")){ //$NON-NLS-1$
//				String missionAttributeKey = sortColumn.substring(3);
//				Attribute.AttributeType maType = getMissionAttributeType(session, missionAttributeKey, caFilter);
//				if (maType != null){
//				
//					sortColumn = engine.getMissionAttributeColumnName(missionAttributeKey);
//					if (maType != null && maType == Attribute.AttributeType.NUMERIC){
//						tableSortField = "sortKeyDbl"; //$NON-NLS-1$
//					}
//				}else{
//					//ma_0 etc. which would work but cannot be sure what column we are actually sorting on					
//				}
//
//			}
//			if (sortColumn.toLowerCase(Locale.ROOT).startsWith("su_")){ //$NON-NLS-1$
//				String samplingUnitAttributeKey = sortColumn.substring(3);
//				Attribute.AttributeType maType = getSamplingUnitAttributeType(session, samplingUnitAttributeKey, caFilter);
//				if (maType != null){
//				
//					sortColumn = engine.getSamplingUnitAttributeColumnName(samplingUnitAttributeKey);
//					if (maType != null && maType == Attribute.AttributeType.NUMERIC){
//						tableSortField = "sortKeyDbl"; //$NON-NLS-1$
//					}
//				}else{
//					//ma_0 etc. which would work but cannot be sure what column we are actually sorting on					
//				}
//
//			}
//			
//			//update to the sort column
//			StringBuilder sql = new StringBuilder();
//			sql.append("UPDATE "); //$NON-NLS-1$
//			sql.append(queryDataTable);
//			sql.append(" SET " + tableSortField + " = " + sortColumn); //$NON-NLS-1$ //$NON-NLS-2$
//			session.createNativeQuery(sql.toString()).executeUpdate();
//			
//		}else{
//			
//			switch (type) {
//			case BOOLEAN:
//			case NUMERIC:
//				StringBuilder sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(" SET sortKeyDbl = "); //$NON-NLS-1$
//				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
//				sql.append("and a.keyid = '"); //$NON-NLS-1$
//				sql.append(key);
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
//				session.createNativeQuery(sql.toString()).executeUpdate();
//				break;
//			case TEXT:
//			case DATE:
//				sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
//				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
//				sql.append("and a.keyid = '"); //$NON-NLS-1$
//				sql.append(key);
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
//				session.createNativeQuery(sql.toString()).executeUpdate();
//				break;
//			case LIST:
//				sql = new StringBuilder();
//				sql.append("UPDATE "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
//				sql.append("(SELECT rl." + value + " FROM "); //$NON-NLS-1$ //$NON-NLS-2$
//				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(tableListSuffix + " rl on rl." + uuidColumn + " = wpoa.list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
//				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
//				sql.append(key);
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
//				session.createNativeQuery(sql.toString()).executeUpdate();
//				break;
//			case TREE:
//				sql = new StringBuilder();
//				sql.append("UPDATE ");//$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
//				sql.append("(SELECT rl." + value + " FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$ //$NON-NLS-2$
//				sql.append(queryDataTable);
//				sql.append(tableTreeSuffix + " rl on rl." + uuidColumn + " = wpoa.tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
//				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
//				sql.append(key);
//				sql.append("'"); //$NON-NLS-1$
//				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
//				sql.append(queryDataTable);
//				sql.append(typePrefix + "uuid)"); //$NON-NLS-1$
//				session.createNativeQuery(sql.toString()).executeUpdate();
//				break;
//			}
//		}
//		
//	}

//	private Attribute.AttributeType getMissionAttributeType(Session session, String missionAttributeKey, ConservationAreaFilter caFilter){
//		if (caFilter.getConservationAreaFilterIds().size() == 1){
//			org.hibernate.query.Query<MissionAttribute> q = session.createQuery("From MissionAttribute where conservationArea.uuid = :ca and keyid = :key", MissionAttribute.class); //$NON-NLS-1$
//			q.setParameter("ca", caFilter.getConservationAreaFilterIds().get(0)); //$NON-NLS-1$
//			q.setParameter("key", missionAttributeKey); //$NON-NLS-1$
//			q.setCacheable(true);
//			
//			List<MissionAttribute> results = q.list();
//			if (results.size() != 1 ){
//				return null;
//			}else{
//				return results.get(0).getType();
//			}
//		}else if (caFilter.getConservationAreaFilterIds().size() == 0){
//			//no conservation areas in filter; this should not be valid
//			return null;
//			
//		}else{
//			org.hibernate.query.Query<MissionAttribute> q = session.createQuery("From MissionAttribute where conservationArea.uuid in (:cas) and keyid = :key", MissionAttribute.class); //$NON-NLS-1$
//			q.setParameterList("cas", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
//			q.setParameter("key", missionAttributeKey); //$NON-NLS-1$
//			
//			List<MissionAttribute> allAttributes = q.list();
//			if (allAttributes.size() == 0) return null;
//			
//			Set<AttributeType> types = allAttributes.stream().map(a->a.getType()).distinct().collect(Collectors.toSet());
//			if (types.size() == 1) return types.iterator().next();
//			return null;	//not a valid column as the key has different types in different cas (or is not valid in any cas)
//		}
//	}
//	
//	private Attribute.AttributeType getSamplingUnitAttributeType(Session session, String smaplingUnitAttributeKey, ConservationAreaFilter caFilter){
//		if (caFilter.getConservationAreaFilterIds().size() == 1){
//			org.hibernate.query.Query<SamplingUnitAttribute> q = session.createQuery("From SamplingUnitAttribute where conservationArea.uuid = :ca and keyid = :key", SamplingUnitAttribute.class); //$NON-NLS-1$
//			q.setParameter("ca", caFilter.getConservationAreaFilterIds().get(0)); //$NON-NLS-1$
//			q.setParameter("key", smaplingUnitAttributeKey); //$NON-NLS-1$
//			q.setCacheable(true);
//
//			List<SamplingUnitAttribute> results = q.list();
//			if (results.size() != 1 ){
//				return null;
//			}else{
//				return results.get(0).getType();
//			}
//		}else if (caFilter.getConservationAreaFilterIds().size() == 0){
//			//no conservation areas in filter; this should not be valid
//			return null;
//			
//		}else{
//			org.hibernate.query.Query<SamplingUnitAttribute> q = session.createQuery("From SamplingUnitAttribute where conservationArea.uuid in (:cas) and keyid = :key", SamplingUnitAttribute.class); //$NON-NLS-1$
//			q.setParameterList("cas", caFilter.getConservationAreaFilterIds()); //$NON-NLS-1$
//			q.setParameter("key", smaplingUnitAttributeKey); //$NON-NLS-1$
//			
//			List<SamplingUnitAttribute> allAttributes = q.list();
//			if (allAttributes.size() == 0) return null;
//			
//			Set<AttributeType> types = allAttributes.stream().map(a->a.getType()).distinct().collect(Collectors.toSet());
//			if (types.size() == 1) return types.iterator().next();
//			return null;	//not a valid column as the key has different types in different cas (or is not valid in any cas)
//		}
//	}
}
