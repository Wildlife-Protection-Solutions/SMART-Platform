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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.query.engine.ObservationQueryResult;
import org.wcs.smart.entity.query.model.EntityObservationResultItem;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResultItem;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityObservationQueryResult extends ObservationQueryResult<EntityObservationResultItem> implements IPagedImageResultSet {

	
	public EntityObservationQueryResult(PsqlEntityObservationEngine engine, int itemCnt, boolean includeUuids){
		super(engine, itemCnt, includeUuids);
	}
	
	@Override
	protected void attachObservations(List<EntityObservationResultItem> result, Connection c, Session session) throws SQLException {
		super.attachObservations(result, c, session);
		attachEntityAttributes(result, c, session);
		
	}
	
	private void attachEntityAttributes(List<EntityObservationResultItem> result, Connection c, Session session) throws SQLException {
		if (((PsqlEntityObservationEngine)engine).getEntityTypes().isEmpty()) return;
		
		// attach entities
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT r.ob_uuid, a.keyid as entitykey, ea.keyid as entityattributekey, "); //$NON-NLS-1$
		sql.append("eav.number_value, eav.string_value, rl.value as list_value, rt.value as tree_value "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(engine.getQueryDataTable());
		sql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid "); //$NON-NLS-1$
		sql.append(" join smart.entity_type a on a.dm_attribute_uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
		sql.append(" join smart.entity e on e.attribute_list_item_uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
		sql.append(" join smart.entity_attribute_value eav on eav.entity_uuid = e.uuid "); //$NON-NLS-1$
		sql.append(" join smart.entity_attribute ea on ea.uuid = eav.entity_attribute_uuid left join "); //$NON-NLS-1$
		sql.append(engine.getObservationLabelTable());
		sql.append(" rl on eav.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		sql.append(engine.getObservationLabelTable());
		sql.append(" rt on eav.tree_node_uuid = rt.UUID "); //$NON-NLS-1$
		sql.append("WHERE r.ob_uuid IN (  "); //$NON-NLS-1$

		List<UUID> uuids = new ArrayList<UUID>();
		for (EntityObservationResultItem it : result) {
			if (it.getObservationUuid() != null) {
				uuids.add(it.getObservationUuid());
				sql.append("?,"); //$NON-NLS-1$
			}
		}
		sql.deleteCharAt(sql.length() - 1);

		sql.append(") AND "); //$NON-NLS-1$
		sql.append("a.keyid in ("); //$NON-NLS-1$
		for (String et : ((PsqlEntityObservationEngine)engine).getEntityTypes()) {
			sql.append("'" + et + "',"); //$NON-NLS-1$//$NON-NLS-2$
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(")"); //$NON-NLS-1$

		PreparedStatement ps = c.prepareStatement(sql.toString());
		for (int i = 0; i < uuids.size(); i++) {
			ps.setObject(i + 1, uuids.get(i));
		}

		try (ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				UUID obuuid = (UUID) rs.getObject(1);
				String entityKey = rs.getString(2);
				String entityAttributeKey = rs.getString(3);
				Object value = null;
				if (rs.getObject(4) != null) {
					// number value
					value = rs.getDouble(4);
				} else if (rs.getObject(5) != null) {
					// string
					value = rs.getString(5);
				} else if (rs.getObject(6) != null) {
					// list string
					value = rs.getString(6);
				} else if (rs.getObject(7) != null) {
					// tree string
					value = rs.getString(7);
				}

				for (IResultItem rii : result) {
					EntityObservationResultItem it = (EntityObservationResultItem) rii;
					if (it.getObservationUuid() != null && it.getObservationUuid().equals(obuuid)) {
						it.addEntityAttribute(entityKey, entityAttributeKey, value);
					}
				}
			}
		}

	}
	
	@Override
	protected EntityObservationResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		EntityObservationResultItem it = new EntityObservationResultItem();
		super.setFields(it, rs);
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		return it;
	}



	@Override
	protected String getDistinctWaypointQuery(String prefix, boolean includeObservation) {
		throw new UnsupportedOperationException("Attachment queries not supported for entity query types."); //$NON-NLS-1$
	}



	@Override
	protected IAttachmentResultItem asAttachmentQueryResultItem(ResultSet rs, Session session) throws SQLException {
		throw new UnsupportedOperationException("Attachment queries not supported for entity query types."); //$NON-NLS-1$
	}

}
