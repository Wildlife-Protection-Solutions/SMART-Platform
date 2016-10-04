package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.api.QueryApi;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;

public abstract class ErSurveyQueryResultSet extends AbstractDbFeatureResultSet {

	protected PsqlErEngine engine;
	
	protected ErSurveyQueryResultSet(PsqlErEngine engine){
		this.engine = engine;
	}
	
	protected void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append(
				"_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable())
				.append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid IN ( "); //$NON-NLS-1$
		boolean hasObservations = false;
		List<UUID> uuids = new ArrayList<UUID>();
		for (IResultItem iri : result) {
			SurveyQueryResultItem it = (SurveyQueryResultItem) iri;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				uuids.add(it.getObservationUuid());
				attrSql.append("?"); //$NON-NLS-1$
			}
		}
		if (!hasObservations)
			return;
		attrSql.append(')');

		String dir;
		if(direction == QueryApi.Direction.DOWN.value ){
			dir = "DESC";
		}else{
			dir ="ASC";
		}
		if(sortColumn != null){
			attrSql.append(" ORDER BY sortkeydbl " +dir+ ", sortkeytxt " + dir); //$NON-NLS-1$
		}
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		for (int i = 0; i < uuids.size(); i++) {
			ps.setObject(i + 1, uuids.get(i));
		}
		try (ResultSet rs = ps.executeQuery()) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(
					rs, session);
			for (IResultItem iri : result) {
				SurveyQueryResultItem it = (SurveyQueryResultItem) iri;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it
							.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}
	}

	protected HashMap<UUID, HashMap<String, Object>> getResultsAttributes(
			ResultSet rs, Session s) throws SQLException {
		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
		/*
		 * 1 OB_UUID 2 KEYID 3 NUMBER_VALUE 4 STRING_VALUE 5 LIST_VALUE 6
		 * TREE_VALUE 7 P_CA_UUID
		 */
		while (rs.next()) {
			UUID obUuid = (UUID) rs.getObject(1);

			if (obUuid == null)
				continue;
			HashMap<String, Object> attributes = attrMap.get(obUuid);
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
				attrMap.put(obUuid, attributes);
			}
			String key = rs.getString(2);
			if (key != null) {
				Object value = getAttributeValue(rs, s);
				attributes.put(key, value);
			}
		}
		return attrMap;
	}

	protected Object getAttributeValue(ResultSet rs, Session session)
			throws SQLException {
		/*
		 * 1 OB_UUID 2 KEYID 3 NUMBER_VALUE 4 STRING_VALUE 5 LIST_VALUE 6
		 * TREE_VALUE 7 P_CA_UUID
		 */
		if (rs.getObject(3) != null) {
			return rs.getDouble(3);
		}
		String result = rs.getString(4); // string
		if (result != null) {
			return result;
		}
		result = rs.getString(5); // list
		if (result != null) {
			return result;
		}
		result = rs.getString(6); // tree
		if (result != null) {
			return result;
		}
		return null;
	}

	protected void attachMissionProperties(List<IResultItem> result,
			Connection c, Session session) throws SQLException {

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT mpv.mission_uuid, ma.keyid, mpv.number_value,  mpv.string_value, mpv.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.mission_attribute ma join smart.mission_property_value mpv on mpv.mission_attribute_uuid = ma.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE mpv.mission_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		List<UUID> uuids = new ArrayList<UUID>();
		for (IResultItem irt : result) {
			UUID muuid = null;
			if (irt instanceof SurveyQueryResultItem) {
				muuid = ((SurveyQueryResultItem) irt).getMissionUuid();
			} else if (irt instanceof MissionTrackResultItem) {
				muuid = ((MissionTrackResultItem) irt).getMissionUuid();
			}
			if (muuid != null) {
				if (hasItem)
					attrSql.append(","); //$NON-NLS-1$
				attrSql.append("?"); //$NON-NLS-1$ 
				uuids.add(muuid);
				hasItem = true;
			}
		}

		if (!hasItem) {
			// no missions
			return;
		}
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

				for (IResultItem irt : result) {
					if (irt instanceof SurveyQueryResultItem) {
						SurveyQueryResultItem it = (SurveyQueryResultItem) irt;
						if (muuid.equals(it.getMissionUuid())) {
							if (rs.getObject(3) != null) {
								it.addMissionPropertyValue(key, dvalue);
							} else if (svalue != null) {
								it.addMissionPropertyValue(key, svalue);
							} else if (rs.getObject(5) != null) {
								it.addMissionPropertyValue(
										key,
										((MissionAttributeListItem) session
												.load(MissionAttributeListItem.class,
														(UUID)rs.getObject(5)))
												.getName());
							}
						}
					} else if (irt instanceof MissionTrackResultItem) {
						MissionTrackResultItem it = (MissionTrackResultItem) irt;
						if (muuid.equals(it.getMissionUuid())) {
							if (rs.getObject(3) != null) {
								it.addMissionPropertyValue(key, dvalue);
							} else if (svalue != null) {
								it.addMissionPropertyValue(key, svalue);
							} else if (rs.getObject(5) != null) {
								it.addMissionPropertyValue(
										key,
										((MissionAttributeListItem) session
												.load(MissionAttributeListItem.class,
														(UUID)rs.getObject(5)))
												.getName());
							}
						}
					}
				}

			}
		}
	}

	protected void attachSamplingUnitAttributes(List<IResultItem> result,
			Connection c, Session session) throws SQLException {

		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT suav.su_uuid, sua.keyid, suav.number_value, suav.string_value, suav.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.sampling_unit_attribute sua join smart.sampling_unit_attribute_value suav"); //$NON-NLS-1$
		attrSql.append(" on suav.su_attribute_uuid = sua.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE suav.su_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		List<UUID> uuids = new ArrayList<UUID>();
		for (IResultItem irt : result) {
			UUID uuid = null;
			if (irt instanceof SurveyQueryResultItem){
				uuid = ((SurveyQueryResultItem) irt).getSamplingUnitUuid();
			}else if (irt instanceof MissionTrackResultItem){
				uuid = ((MissionTrackResultItem) irt).getSamplingUnitUuid();
			}
			if (uuid != null){
				if (hasItem)
					attrSql.append(","); //$NON-NLS-1$
				uuids.add(uuid);
				attrSql.append("?"); //$NON-NLS-1$
				hasItem = true;
			}
		}

		if (!hasItem) {
			// no missions
			return;
		}
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

				for (IResultItem irt : result) {
					if (irt instanceof SurveyQueryResultItem){
						SurveyQueryResultItem it = (SurveyQueryResultItem) irt;
						if (muuid.equals(it.getSamplingUnitUuid())) {
							if (rs.getObject(3) != null) {
								it.addSamplingUnitAttributeValue(key, dvalue);
							} else if (svalue != null) {
								it.addSamplingUnitAttributeValue(key, svalue);
							} else if (rs.getObject(5) != null) {
								String value = ((SamplingUnitAttributeListItem) session
										.load(SamplingUnitAttributeListItem.class,
												(UUID)rs.getObject(5)))
										.getName();
								it.addSamplingUnitAttributeValue(key, value);
							}
						}
					}else if (irt instanceof MissionTrackResultItem){
						MissionTrackResultItem it = (MissionTrackResultItem) irt;
						if (muuid.equals(it.getSamplingUnitUuid())) {
							if (rs.getObject(3) != null) {
								it.addSamplingUnitAttributeValue(key, dvalue);
							} else if (svalue != null) {
								it.addSamplingUnitAttributeValue(key, svalue);
							} else if (rs.getObject(5) != null) {
								String value = ((SamplingUnitAttributeListItem) session
										.load(SamplingUnitAttributeListItem.class,
												(UUID)rs.getObject(5)))
										.getName();
								it.addSamplingUnitAttributeValue(key, value);
							}
						}
					}
				}
			}
		}
	}

}
