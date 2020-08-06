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
package org.wcs.smart.asset.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.query.model.AssetQueryResultItem;
import org.wcs.smart.asset.query.model.observation.AssetAttributeQueryColumn;
import org.wcs.smart.asset.query.model.observation.AssetCategoryQueryColumn;
import org.wcs.smart.asset.query.model.observation.FixedQueryColumn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.observation.query.model.columns.ObservationCategoryQueryColumn;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AttachmentResultSetIterator;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IPagedImageResultSet;
import org.wcs.smart.query.common.engine.IQueryResultSetIterator;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.common.ui.image.PagedImageQueryResults;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Wrapper for resulted temporary table which was build for particular query.
 * Provides ability to lazy load items from this table and sorting  functionality.
 *  
 */
public class AssetPagedObservationResult extends AssetPagedWaypointResult implements IObservationPagedQueryResultSet, IWaypointUpdateableResultSet, IPagedImageResultSet {
	
	private int wpCount = 0;

	private boolean hasSortColumns = false;
	private Set<String> dataColumns = null;

	//image results
	private PagedImageQueryResults imageResults = new PagedImageQueryResults() {
		@Override
		protected void initImageData() {
			AssetPagedObservationResult.this.initImageData();
		}
	};
	
	public AssetPagedObservationResult(String queryTempTable, AssetQueryEngine engine) {
		super(queryTempTable, engine);
	}
	
	@Override
	public String getResultsTable() {
		return queryTempTable;
	}
	
	@Override
	protected void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		if (sortColumn instanceof AssetAttributeQueryColumn){
			if (!hasSortColumns){
				//add the sort columns
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyDbl double"); //$NON-NLS-1$ //$NON-NLS-2$
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyTxt varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
				c.commit();
				hasSortColumns = true;
			}
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			Attribute attribute = null;
			
			session.beginTransaction();
			try{
				attribute = QueryDataModelManager.getInstance().getAttribute(session, key); //session will not be closed on purpose
			}finally{
				session.getTransaction().rollback();
			}
			
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				// nullify first
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyDbl = null "); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case LIST:
			case TREE:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = null"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			}
			
			switch (attribute.getType()) {
			case BOOLEAN:
			case NUMERIC:
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyDbl = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.NUMBER_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append(key);
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case TEXT:
			case DATE:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT wpoa.STRING_VALUE FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
				sql.append("and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case LIST:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
				sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append( "_LIST rl on rl.uuid = wpoa.list_element_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable); 
				sql.append(".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
			case TREE:
				sql = new StringBuilder();
				sql.append("UPDATE ");//$NON-NLS-1$
				sql.append(queryTempTable);
				sql.append(" SET sortKeyTxt = ");//$NON-NLS-1$
				sql.append("(SELECT rl.value FROM smart.WP_OBSERVATION_ATTRIBUTES wpoa join "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append("_TREE rl on rl.uuid = wpoa.tree_node_uuid "); //$NON-NLS-1$
				sql.append("join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid and a.keyid = '"); //$NON-NLS-1$
				sql.append( key );
				sql.append("'"); //$NON-NLS-1$
				sql.append(" WHERE wpoa.observation_uuid = "); //$NON-NLS-1$
				sql.append( queryTempTable );
				sql.append( ".ob_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
			}
		}
		c.commit();
	}
		

	private void attachObservations(List<? extends IResultItem> result, Connection c, Session session) throws SQLException {
		boolean hasObservations = false;
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.wp_ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (IResultItem pit : result){
			AssetQueryResultItem it  = (AssetQueryResultItem) pit;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				attrSql.append("x'").append(UuidUtils.uuidToString(it.getObservationUuid())).append('\''); //$NON-NLS-1$
			}
		}
		attrSql.append(')');
		
		//no observations in current data fragment, so no need to select attributes as they will be empty
		if (!hasObservations) return;
				
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (IResultItem pit : result){
				AssetQueryResultItem it  = (AssetQueryResultItem) pit;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}
	}
	
	@Override
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof FixedQueryColumn) {
			String key = sortColumn.getKey();
			key = FixedQueryColumn.getDbColumnName(key);			
			if (sortColumn.getKey().equals(FixedQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (((FixedQueryColumn)sortColumn).getKey().equals(FixedQueryColumn.FixedColumns.OBS_GROUP_ID.getKey())){
				result = "order by r.wp_group_uuid"; //$NON-NLS-1$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r." + key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}
		if (sortColumn instanceof AssetCategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = ObservationCategoryQueryColumn.getDbColumnName(key);
			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (sortColumn instanceof AssetAttributeQueryColumn) {
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			switch (sortColumn.getType()) {
				case BOOLEAN:
				case NUMBER:
				case INTEGER:
					result = "order by sortKeyDbl"; //$NON-NLS-1$
					break;
				case DATE:
					result = "order by DATE(sortKeyTxt)"; //$NON-NLS-1$
					break;
				default:
					result = "order by UPPER(sortKeyTxt)"; //$NON-NLS-1$
					break;
			}
		}
		if (!result.isEmpty()) {
			result += direction == SWT.UP ? " asc" : " desc"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
	/**
	 * Gets results from the given result get.
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
			AssetQueryResultItem it = engine.asQueryResultItem(rs, null);
			items.add(it);
		}
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				attachObservations(items, c, session);		
			}
			
		});
		
		return items;
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
			byte[] obUuid = rs.getBytes(1);
			if (obUuid == null)
				continue;
			UUID keyObj = UuidUtils.byteToUUID(obUuid);
			HashMap<String, Object> attributes = attrMap.get(keyObj);
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
				attrMap.put(keyObj, attributes);
			}
			String key = rs.getString(2);
			if (key != null) {
				Object value = getAttributeValue(rs, s);
				attributes.put(key, value);
			}
		}
		return attrMap;
	}

	/**
	 * Gets the attribute value from the result set for the given attribute.
	 * 
	 * @param att
	 * @param rs
	 * @param session
	 * @return
	 * @throws SQLException
	 */
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

	public int getWpCount() {
		return wpCount;
	}

	protected void setWpCount(int wpCount) {
		this.wpCount = wpCount;
	}

	@Override
	public boolean isDataColumn(QueryColumn column) {
		return dataColumns != null && dataColumns.contains(column.getKey());
	}
	
	public void setDataColumns(Set<String> dataColumns) {
		this.dataColumns = dataColumns;
	}
	
	private boolean updateAttribute(WaypointObservation wo, String attributeKey, Object newValue, Session session){
		WaypointObservationAttribute toUpdate = null;
		for (WaypointObservationAttribute woa : wo.getAttributes()){
			if (woa.getAttribute().getKeyId().equals(attributeKey)){
				toUpdate = woa;
			}
		}
		if (toUpdate == null) return false;
		
		boolean updated = false;
		switch(toUpdate.getAttribute().getType()){
			case BOOLEAN:
				if (newValue instanceof Boolean){
					Boolean newBoolean = (Boolean)newValue;
					if ((toUpdate.getNumberValue() > 0.5 && !newBoolean) || (toUpdate.getNumberValue() < 0.5 && newBoolean)){
						if (newBoolean){
							toUpdate.setNumberValue(1.0);
						}else{
							toUpdate.setNumberValue(0.0);
						}
						updated = true;
					}
				}
				break;
			case DATE:
				if (newValue instanceof Date){
					Date newDate = (Date)newValue;
					if (!SharedUtils.isSameDate(newDate,  toUpdate.getDateValue())){
						toUpdate.setDateValue(newDate);
						updated = true;
					}
				}
				break;
			case LIST:
				if (newValue instanceof AttributeListItem){
					AttributeListItem newItem = (AttributeListItem)newValue;
					if (!newItem.equals(toUpdate.getAttributeListItem())){
						toUpdate.setAttributeListItem(newItem);
						updated = true;
						
						//add label to query results if necessary
						if (engine instanceof AssetObservationEngine){
							((AssetObservationEngine)engine).addListLabel(session, newItem);
						}
					}
				}
				break;
			case NUMERIC:
				if (newValue == null && toUpdate.getNumberValue() == null) break;
				if (newValue == null) {
					toUpdate.setNumberValue(null);
					updated = true;
				}else if (newValue instanceof Double){
					Double newDouble = (Double)newValue;
					if (toUpdate.getNumberValue().doubleValue() != newDouble.doubleValue()){
						toUpdate.setNumberValue(newDouble);
						updated = true;
					}
				}
				break;
			case TEXT:
				if (newValue instanceof String){
					String newString = (String)newValue;
					if (!toUpdate.getStringValue().equals(newString)){
						toUpdate.setStringValue(newString);
						updated = true;
					}
				}
				break;
			case TREE:
				if (newValue instanceof AttributeTreeNode){
					AttributeTreeNode newItem = (AttributeTreeNode)newValue;
					if (!newItem.equals(toUpdate.getAttributeTreeNode())){
						toUpdate.setAttributeTreeNode(newItem);
						updated = true;
						
						//add label to query results if necessary
						if (engine instanceof AssetObservationEngine){
							((AssetObservationEngine)engine).addTreeLabel(session, newItem);
						}
					}
				}
				break;
		}
		return updated;
	}

	@Override
	public boolean update(QueryColumn column, IResultItem item, Object newValue) throws Exception{
		if (super.update(column, item, newValue)) return true;
		if (column instanceof AttributeQueryColumn){
			return updateAttributeColumn((AttributeQueryColumn)column, (AssetQueryResultItem)item, newValue);
		}else if (column instanceof CategoryQueryColumn){
			return updateObservation((AssetQueryResultItem)item, newValue);
		}
		return false;
	}
	
	public boolean deleteObservation(UUID observationUuid) throws Exception{
		Waypoint wp = null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			s.getTransaction().begin();
			try{		
				WaypointObservation wo = (WaypointObservation) s.get(WaypointObservation.class, observationUuid);
				if (wo == null) return false;
				wp = wo.getWaypoint();
				s.delete(wo);

				//delete empty observation groups
				wo.getObservationGroup().getObservations().remove(wo);
				if (wo.getObservationGroup().getObservations().isEmpty()) {
					s.delete(wo.getObservationGroup());
					wo.getObservationGroup().getWaypoint().getObservationGroups().remove(wo.getObservationGroup());
				}
				
				//update category names in query results table
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT count(*) FROM " + queryTempTable + " WHERE wp_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				queryUpdate.setParameter("uuid", wp.getUuid()); //$NON-NLS-1$
				Integer cnt = (Integer)queryUpdate.uniqueResult();
				if (cnt > 1){
					sql = new StringBuilder();
					sql.append(" DELETE FROM " + queryTempTable + " WHERE ob_uuid = :uuid "); //$NON-NLS-1$ //$NON-NLS-2$
					queryUpdate = s.createNativeQuery(sql.toString());
					queryUpdate.setParameter("uuid", observationUuid); //$NON-NLS-1$
					queryUpdate.executeUpdate();
					
					((AssetObservationEngine) engine).updateResultCount(s, this);
				}else{
					sql = new StringBuilder();
					sql.append(" UPDATE " + queryTempTable + " SET ob_uuid = null, wp_group_uuid = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					for (int j = 0; j < ((AssetObservationEngine)engine).getCategoryCount(); j++) {
						sql.append("category_" + j + " = null, "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sql.deleteCharAt(sql.length() - 1);
					sql.deleteCharAt(sql.length() - 1);
					sql.append(" WHERE ob_uuid = :uuid "); //$NON-NLS-1$
					queryUpdate = s.createNativeQuery(sql.toString());
					queryUpdate.setParameter("uuid", observationUuid); //$NON-NLS-1$
					queryUpdate.executeUpdate();
				}
				s.flush();
				updateLastModified(wp, s);
				s.getTransaction().commit();
			}catch(Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		WaypointEventManager.getInstance().waypointModified(wp);
		getEventBroker().post(AssetEvents.ASSETDATA, null);
		return true;
	}
	
	public boolean updateObservation(AssetQueryResultItem item, Object newValue) throws Exception{
		if (!(newValue instanceof WaypointObservation)) return false;
		WaypointObservation newOb = (WaypointObservation)newValue;
		
		if (newOb.getUuid() == null && item.getObservationUuid() != null) return false;	//cannot add a new feature to a row that already has an observation
		if (newOb.getUuid() == null && newOb.getCategory() == null) return false;//nothing to do
		
		try(Session s = HibernateManager.openSession()){
			try{
				s.getTransaction().begin();
				
				WaypointObservation wo;
				if (newOb.getUuid() == null){
					//new
					Waypoint wp = (Waypoint) s.get(Waypoint.class, item.getWaypointUuid());
					if (wp.getObservationGroups() == null)  wp.setObservationGroups(new ArrayList<>());
					if (wp.getObservationGroups().isEmpty()) {
						WaypointObservationGroup g = new WaypointObservationGroup();
						g.setObservations(new ArrayList<>());
						g.setWaypoint(wp);
						wp.getObservationGroups().add(g);
						s.save(g);
						item.setObservationGroupUuid(g.getUuid());
					}
					//add to first group
					WaypointObservationGroup first = wp.getObservationGroups().get(0);
					wo = newOb;
					wo.setObservationGroup(first);
					first.getObservations().add(wo);
					s.save(wo);
					
				}else{
					wo = (WaypointObservation) s.get(WaypointObservation.class, newOb.getUuid());
					if (wo == null) return false;
					if (wo.getAttributes() != null){
						for (WaypointObservationAttribute a : wo.getAttributes()){
							s.delete(a);
						}
						wo.getAttributes().clear();
						s.flush();
					}
					wo.setCategory(newOb.getCategory());
					if (wo.getAttributes() == null) wo.setAttributes(new ArrayList<>());
					for (WaypointObservationAttribute newA : newOb.getAttributes()){
						wo.getAttributes().add(newA);
						newA.setObservation(wo);
					}
				}
				
				//update category names in query results table
				List<String> names = new ArrayList<>();
				Category c = wo.getCategory();
				while(c != null){
					names.add(0, c.getName());
					c = c.getParent();
				}
				HashMap<String,Object> params = new HashMap<>();
				StringBuilder sql = new StringBuilder();
				sql.append("UPDATE " + queryTempTable + " SET "); //$NON-NLS-1$ //$NON-NLS-2$
				if (item.getObservationUuid()==null){
					sql.append("ob_uuid = :obuuid, "); //$NON-NLS-1$
					params.put("obuuid", wo.getUuid()); //$NON-NLS-1$
					
					sql.append("wp_group_uuid = :grpuuid, "); //$NON-NLS-1$
					params.put("grpuuid", wo.getObservationGroup().getUuid()); //$NON-NLS-1$
				}
				
				for (int j = 0; j < ((AssetObservationEngine)engine).getCategoryCount(); j++) {
					if (j > 0){
						sql.append(", "); //$NON-NLS-1$
					}
					sql.append("category_"); //$NON-NLS-1$
					sql.append(j);
					if (j < names.size()){
						sql.append("= :cat"); //$NON-NLS-1$
						sql.append(j);
						params.put("cat" + j, names.get(j)); //$NON-NLS-1$
					}else{
						sql.append(" = null"); //$NON-NLS-1$
					}
				}
				sql.append(" WHERE "); //$NON-NLS-1$
				if (item.getObservationUuid() == null){
					sql.append("wp_uuid = :uuid"); //$NON-NLS-1$
					params.put("uuid", wo.getWaypoint().getUuid()); //$NON-NLS-1$
				}else{
					sql.append("ob_uuid = :uuid"); //$NON-NLS-1$
					params.put("uuid", wo.getUuid()); //$NON-NLS-1$
				}
				
				NativeQuery<?> queryUpdate = s.createNativeQuery(sql.toString());
				for (Entry<String,Object> e : params.entrySet()){
					queryUpdate.setParameter(e.getKey(), e.getValue());
				}
				queryUpdate.executeUpdate();
				
				//add label to query results if necessary
				if (engine instanceof AssetObservationEngine){
					for (WaypointObservationAttribute a : wo.getAttributes()){
						if (a.getAttribute().getType() == AttributeType.LIST){
							if (a.getAttributeListItem() != null){
								((AssetObservationEngine)engine).addListLabel(s, a.getAttributeListItem());
							}
						}else if (a.getAttribute().getType() == AttributeType.TREE){
							((AssetObservationEngine)engine).addTreeLabel(s, a.getAttributeTreeNode());
						}
					}
				}
				s.flush();
				updateLastModified(wo.getWaypoint(), s);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}
		}
		return true;
	}
	
	private boolean updateAttributeColumn(AttributeQueryColumn column, AssetQueryResultItem item, Object value) throws Exception{
		boolean change = false;
		WaypointObservation wpo = null;
		try(Session s = HibernateManager.openSession()){
			s.getTransaction().begin();
			try {
				wpo = (WaypointObservation) s.get(WaypointObservation.class, item.getObservationUuid());
				if (wpo != null) {
					change = updateAttribute(wpo, column.getAttributeId(), value, s);
					s.flush();
					updateLastModified(wpo.getWaypoint(), s);
				}
				s.getTransaction().commit();
			} catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}

		if (change) {
			WaypointEventManager.getInstance().waypointModified(wpo.getWaypoint());
			getEventBroker().post(AssetEvents.ASSETDATA, null);
			return true;
		}
		return false;
	}
	
	
	@Override
	public void dispose(Session session)throws SQLException {
		super.dispose(session);
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				if (imageResults.getResultsTable() != null) engine.dropTable(c, imageResults.getResultsTable());
			}
		});
	}
	
	@Override
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize) {
		return imageResults.getImageData(offset, pageSize);
	}

	@Override
	public void createTooltip(IAttachmentResultItem data, final Composite parent) {
		AssetAttachmentTooltipProvider job = new AssetAttachmentTooltipProvider(data, parent);
		job.schedule();
	}

	@Override
	public int getImageCount() {
		return imageResults.getImageCount();
	}
	
	@Override
	public IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		initImageData();

		StringBuilder sb = new StringBuilder();
		String part = ((AssetObservationEngine)engine).getDistinctWaypointQuery("r.", true); //$NON-NLS-1$
		
		//join together attachments specifically associated with an observation
		//or attachments associated with a waypoint that has an observation
		//in the result set
		sb.append("SELECT " + part + ", b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(queryTempTable + " r "); //$NON-NLS-1$
		sb.append(" join " + imageResults.getResultsTable() + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on ( b.ob_uuid is not null and r.ob_uuid = b.ob_uuid ) "); //$NON-NLS-1$
		
		sb.append(" UNION "); //$NON-NLS-1$
		
		part = ((AssetObservationEngine)engine).getDistinctWaypointQuery("r.", false); //$NON-NLS-1$
		
		sb.append("SELECT foo.*, b.attach_uuid as attach_uuid FROM "); //$NON-NLS-1$
		sb.append("(SELECT DISTINCT " + part + " FROM " + queryTempTable + " r ) foo"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sb.append(" join " + imageResults.getResultsTable() + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" ON b.wp_uuid = foo.wp_uuid and b.ob_uuid is null"); //$NON-NLS-1$
		
		return new AttachmentResultSetIterator(session, 
				e->engine.asQueryAttachmentResultItem(e, session),
				()->sb.toString());
	}
	
	private synchronized void initImageData() {
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				String imageTempTable = engine.createTempTableName();
				
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE "); //$NON-NLS-1$
				sb.append(imageTempTable);
				sb.append("(attach_uuid char(16) for bit data, wp_uuid char(16) for bit data, ob_uuid char(16) for bit data, seq_order integer GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1))"); //$NON-NLS-1$
				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append(imageTempTable + "(attach_uuid, wp_uuid, ob_uuid) "); //$NON-NLS-1$
				
				sb.append(" SELECT z.attach_uuid, z.wp_uuid, z.ob_uuid FROM ( "); //$NON-NLS-1$
				sb.append("SELECT c.uuid as attach_uuid, a.wp_date, a.wp_id, a.wp_uuid, a.ob_uuid " ); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( queryTempTable + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(" smart.observation_attachment c on a.ob_uuid = c.obs_uuid "); //$NON-NLS-1$
				sb.append( " UNION "); //$NON-NLS-1$
				sb.append("SELECT c.uuid as attach_uuid, a.wp_date, a.wp_id, a.wp_uuid, cast(null as char(16) for bit data) as ob_uuid " ); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( queryTempTable + " a "); //$NON-NLS-1$
				sb.append(" JOIN "); //$NON-NLS-1$
				sb.append(" smart.wp_attachments c on c.wp_uuid = a.wp_uuid "); //$NON-NLS-1$
				sb.append(" ) z ORDER BY z.wp_date desc, z.wp_id "); //$NON-NLS-1$

				s.createNativeQuery(sb.toString()).executeUpdate();
				
				sb = new StringBuilder();
				sb.append("SELECT count(*) FROM "); //$NON-NLS-1$
				sb.append(imageTempTable);
				
				int imageDataCnt = (int) s.createNativeQuery(sb.toString()).uniqueResult();
				
				imageResults.setResults(imageTempTable, imageDataCnt);
				s.getTransaction().commit();
			}catch (Exception ex) {
				imageResults.setResults(null, -1);
				s.getTransaction().rollback();
				QueryPlugIn.log("Error computing attachment details: " + ex.getMessage(), ex); //$NON-NLS-1$
			}
		}
	}
}
