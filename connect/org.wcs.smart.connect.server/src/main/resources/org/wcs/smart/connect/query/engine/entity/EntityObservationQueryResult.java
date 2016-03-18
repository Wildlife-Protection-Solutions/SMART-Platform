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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
/**
 * Result set of observation (all data) queries.
 * 
 * @author Emily
 *
 */
public class EntityObservationQueryResult extends AbstractDbFeatureResultSet {

	private PsqlEntityObservationEngine engine;
	
	public EntityObservationQueryResult(PsqlEntityObservationEngine engine, int itemCnt){
		this.engine = engine;
		setItemCount(itemCnt);
	}
	
	/**
	 * Gets results from the given result set.
	 * 
	 * @param rs
	 * @param from
	 * @param pageSize
	 * @return
	 * @throws SQLException
	 */
	@Override
	public List<IResultItem> getResults(Session session, ResultSet rs, int from, int pageSize) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			EntityQueryResultItem it = asQueryResultItem(rs);
			items.add(it);
		}
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(items, c, session);
				attachEntityAttributes(items, c, session);
			}
			
		});
		return items;
	}
	
	
	private void attachEntityAttributes(List<IResultItem> result, Connection c, Session session) throws SQLException {
		if (engine.getEntityTypes().size() > 0){
			//attach entities
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT r.ob_uuid, a.keyid as entitykey, ea.keyid as entityattributekey, eav.number_value, eav.string_value, rl.value as list_value, rt.value as tree_value "); //$NON-NLS-1$
				sql.append(" FROM "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append(" r join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid join smart.entity_type a on a.dm_attribute_uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity e on e.attribute_list_item_uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity_attribute_value eav on eav.entity_uuid = e.uuid "); //$NON-NLS-1$
				sql.append(" join smart.entity_attribute ea on ea.uuid = eav.entity_attribute_uuid left join "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append("_list rl on eav.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
				sql.append(engine.getQueryDataTable());
				sql.append("_tree rt on eav.tree_node_uuid = rt.UUID "); //$NON-NLS-1$	
			
				sql.append("WHERE r.ob_uuid IN (  "); //$NON-NLS-1$
				
				List<UUID> uuids = new ArrayList<UUID>();
				for (IResultItem rii : result) {
					EntityQueryResultItem it = (EntityQueryResultItem) rii;
					if (it.getObservationUuid() != null) {
						uuids.add(it.getObservationUuid());
						sql.append("?,");
					}
				}
				sql.deleteCharAt(sql.length() - 1);
				
				sql.append(") AND "); //$NON-NLS-1$
				sql.append("a.keyid in ("); //$NON-NLS-1$
				for (String et : engine.getEntityTypes()){
					sql.append("'" + et + "',");  //$NON-NLS-1$//$NON-NLS-2$
				}
				sql.deleteCharAt(sql.length() - 1);
				sql.append(")"); //$NON-NLS-1$
				
				PreparedStatement ps = c.prepareStatement(sql.toString());
				for (int i = 0; i < uuids.size(); i ++){
					ps.setObject(i+1, uuids.get(i));
				}
				
				try(ResultSet rs = ps.executeQuery()){
					while(rs.next()){
						UUID obuuid = (UUID) rs.getObject(1);
						String entityKey = rs.getString(2);
						String entityAttributeKey = rs.getString(3);
						Object value = null;
						if (rs.getObject(4) != null){
							//number value
							value = rs.getDouble(4);
						}else if (rs.getObject(5) != null){
							//string
							value = rs.getString(5);
						}else if (rs.getObject(6) != null){
							//list string
							value = rs.getString(6);
						}else if (rs.getObject(7) != null){
							//tree string
							value = rs.getString(7);
						}
						
						for (IResultItem rii : result) {
							EntityQueryResultItem it = (EntityQueryResultItem) rii;
							if (it.getObservationUuid() != null
									&& it.getObservationUuid().equals(obuuid)) {
								it.addEntityAttribute(entityKey,
										entityAttributeKey, value);
							}
						}
					}
				}
				
			}
	}
	
	private void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.p_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable());
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(engine.getQueryDataTable()).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid IN ( "); //$NON-NLS-1$
		boolean hasObservations = false;
		List<UUID> uuids = new ArrayList<UUID>();
		for (IResultItem iri : result){
			EntityQueryResultItem it  = (EntityQueryResultItem) iri;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				uuids.add(it.getObservationUuid());
				attrSql.append("?"); //$NON-NLS-1$
			}
		}
		if (!hasObservations) return;
		attrSql.append(')');
		
		PreparedStatement ps = c.prepareStatement(attrSql.toString());
		for (int i = 0; i < uuids.size(); i ++){
			ps.setObject(i+1, uuids.get(i));
		}
		try(ResultSet rs = ps.executeQuery()) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (IResultItem iri : result){
				EntityQueryResultItem it  = (EntityQueryResultItem) iri;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}
	}
	
	protected HashMap<UUID, HashMap<String, Object>> getResultsAttributes(ResultSet rs, Session s) throws SQLException {
		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
		/*
		1	OB_UUID
		2	KEYID
		3	NUMBER_VALUE
		4	STRING_VALUE
		5	LIST_VALUE
		6	TREE_VALUE
		7	P_CA_UUID
		*/
		while (rs.next()) {
			UUID obUuid = (UUID)rs.getObject(1);
			
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
	protected Object getAttributeValue(ResultSet rs, Session session) throws SQLException {
		/*
		1	OB_UUID
		2	KEYID
		3	NUMBER_VALUE
		4	STRING_VALUE
		5	LIST_VALUE
		6	TREE_VALUE
		7	P_CA_UUID
		*/
		if (rs.getObject(3) != null) {
			return rs.getDouble(3);
		}
		String result = rs.getString(4); //string
		if (result != null) {
			return result;
		}
		result = rs.getString(5); //list
		if (result != null) {
			return result;
		}
		result = rs.getString(6); //tree
		if (result != null) {
			return result;
		}
		return null;
	}

	@Override
	public String getGeometryType() {
		return POINT_GEOM_TYPE;
	}

	@Override
	public Geometry createGeometry(IResultItem rs) throws Exception {
		EntityQueryResultItem i = ((EntityQueryResultItem)rs);
		return gf.createPoint(new Coordinate(i.getWaypointX(), i.getWaypointY())); 
	}

	@Override
	public String createId(IResultItem rs) throws Exception {
		return ((EntityQueryResultItem)rs).getWaypointId() + "." + System.nanoTime(); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Override
	public ResultSet getResultSet(final Session session) {
		return session.doReturningWork(new ReturningWork<ResultSet>() {
			@Override
			public ResultSet execute(Connection c) throws SQLException {
				return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * FROM " + engine.getQueryDataTable()); //$NON-NLS-1$
			}
		});
	}
	
	protected EntityQueryResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		EntityQueryResultItem it = new EntityQueryResultItem();
		it.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		it.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		it.setSourceId(rs.getString("wp_source")); //$NON-NLS-1$
		it.setWaypointUuid((UUID)rs.getObject("wp_uuid")); //$NON-NLS-1$
		it.setWaypointId(rs.getInt("wp_id")); //$NON-NLS-1$
		it.setWaypointX(rs.getDouble("wp_x")); //$NON-NLS-1$
		it.setWaypointY(rs.getDouble("wp_y")); //$NON-NLS-1$
		it.setWpDateTime(rs.getTimestamp("wp_time")); //$NON-NLS-1$
		it.setWaypointDirection(rs.getObject("wp_direction") == null ? null : rs.getFloat("wp_direction")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointDistance(rs.getObject("wp_distance") == null ? null : rs.getFloat("wp_distance")); //$NON-NLS-1$ //$NON-NLS-2$
		it.setWaypointComment(rs.getString("wp_comment")); //$NON-NLS-1$
		it.setWaypointObserver(rs.getString("ob_observer")); //$NON-NLS-1$
		it.setObservationUuid((UUID)rs.getObject("ob_uuid")); //$NON-NLS-1$
		
		//build categories
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < engine.getCategoryCnt(); i ++){
			String category = rs.getString("category_"+i); //$NON-NLS-1$
			if (category == null){
				break;
			}
			categories.add(category);
		}
		
		it.setCategory(categories.toArray(new String[categories.size()]));
		return it;
	}

	@Override
	public void dispose(Session session) throws SQLException{
		engine.cleanUp(session);		
	}

}
