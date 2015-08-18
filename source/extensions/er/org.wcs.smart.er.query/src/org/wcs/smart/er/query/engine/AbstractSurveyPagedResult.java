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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.MissionTrackResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyQueryResultItem;
import org.wcs.smart.er.query.ui.columns.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.ui.columns.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.er.query.ui.columns.SurveyAttributeQueryColumn;
import org.wcs.smart.er.query.ui.columns.SurveyCategoryQueryColumn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.AbstractPagedQueryResultSet;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Abstract class for shared paged result set survey object.
 * 
 * @author Emily
 *
 */
public abstract class AbstractSurveyPagedResult  extends AbstractPagedQueryResultSet{

	protected String queryTempTable;

	protected ResultSet lastResultSet;
	
	protected Envelope bounds = null;

	//next sort column
	protected QueryColumn sortColumn = null;
	protected QueryColumn lastSortColumn = null;
	protected int direction = SWT.UP;
	protected boolean hasSortColumns = false;

	protected DerbySurveyQueryEngine engine;

	/**
	 * Drops active result set
	 */
	protected abstract void dropResultSet();
	
	/**
	 * Loads the next set of data given the offset and page size from
	 * the current open result set.
	 * 
	 * @param session
	 * @param offset
	 * @param pageSize
	 * @return
	 */
	protected abstract List<IResultItem> getNextData(final Session session, final int offset, final int pageSize);
	
	/**
	 * Opens a new result set and loads the next 
	 * set of data given the offset and page size
	 * @param session
	 * @param offset
	 * @param pageSize
	 * @return
	 */
	protected abstract List<IResultItem> getData(final Session session, final int offset, final int pageSize);
	
	@Override
	public void destroy() {
		//simply closing result set and deleting temporary table
		dropResultSet();
		super.destroy();
	}
	
	
	@Override
	public List<IResultItem> getData(final int offset, final int pageSize) {
		final Session session = HibernateManager.openSession();
		//NOTE: session will not be closed on purpose!!!!
		//as we want related ResultSet to remain opened for performance reasons
		List<IResultItem> result = getNextData(session, offset, pageSize);
		if (result == null) {
			result = getData(session, offset, pageSize);
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.model.IPagedQueryResultSet#setSorting(org.wcs.smart.query.model.observation.QueryColumn, int)
	 */
	public void setSorting(final QueryColumn sortColumn, int direction) {
		this.lastSortColumn = this.sortColumn;
		this.sortColumn = sortColumn;
		this.direction = direction;
		dropResultSet();
	}
	
	private static String[][] FIXED_COLUMN_KEY_TO_ROW  = {
		{"waypoint", "wp"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"su_id", "samplingunit_id"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"su_buffer", "samplingunit_buffer"}, //$NON-NLS-1$ //$NON-NLS-2$
		{"wp_time", "wp_date"} //$NON-NLS-1$ //$NON-NLS-2$
	};
	
	
	protected String buildSortSql() {
		if (sortColumn == null || direction == SWT.NONE)
			return ""; //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof SurveyQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			for (String[] data : FIXED_COLUMN_KEY_TO_ROW) {
				key = key.replace(data[0], data[1]);
			}
			if (sortColumn.getKey().equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}else if (sortColumn instanceof SurveyCategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = key.replace(":", "_"); //$NON-NLS-1$ //$NON-NLS-2$ 
			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}else if (sortColumn instanceof SurveyAttributeQueryColumn ||
				sortColumn instanceof MissionPropertyQueryColumn ||
				sortColumn instanceof SamplingUnitAttributeQueryColumn) {
			
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
	 * A default implementation that assumes wp_x and wp_y fields in the temporary
	 * data table.  Users can overwrite as required.
	 */
	@Override
	public Envelope getEnvelope(){
		if (this.bounds == null){
			Session s = HibernateManager.openSession();
			final String sql = "SELECT min(wp_x), max(wp_x), min(wp_y), max(wp_y) FROM " + queryTempTable; //$NON-NLS-1$
			s.doWork(new Work(){

				@Override
				public void execute(Connection c) throws SQLException {
					try(ResultSet q = c.createStatement().executeQuery(sql)){
						q.next();
						double minx = q.getDouble(1);
						double maxx = q.getDouble(2);
						double miny = q.getDouble(3);
						double maxy = q.getDouble(4);
				
						bounds = new Envelope(minx, maxx, miny, maxy);
					}
				}	
			});
		}
		return bounds;	
	}
	
	protected void updateSortColumn(QueryColumn sortColumn, Session session, Connection c) throws SQLException{
		if (sortColumn instanceof SurveyAttributeQueryColumn ||
			sortColumn instanceof MissionPropertyQueryColumn ||
			sortColumn instanceof SamplingUnitAttributeQueryColumn) {
			if (!hasSortColumns){
				//add the sort columns
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyDbl double"); //$NON-NLS-1$ //$NON-NLS-2$
				c.createStatement().execute("ALTER TABLE " + queryTempTable + " add column sortKeyTxt varchar(1024)"); //$NON-NLS-1$ //$NON-NLS-2$
				c.commit();
				hasSortColumns = true;
			}
			String key = sortColumn.getKey();
			key = key.split(":")[1]; //$NON-NLS-1$
			Attribute.AttributeType type = null;
			
			
			session.beginTransaction();
			try{
				if (sortColumn instanceof SurveyAttributeQueryColumn){
					Attribute attribute = QueryDataModelManager.getInstance().getAttribute(session, key); //session will not be closed on purpose
					type = attribute.getType();
				}else if (sortColumn instanceof MissionPropertyQueryColumn){
					MissionAttribute prop = (MissionAttribute) session.createCriteria(MissionAttribute.class)
							.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list().get(0); //$NON-NLS-1$
					type = prop.getType();
				}else if (sortColumn instanceof SamplingUnitAttributeQueryColumn){
					SamplingUnitAttribute suA = (SamplingUnitAttribute)session.createCriteria(SamplingUnitAttribute.class)
							.add(Restrictions.eq("keyId", key)) //$NON-NLS-1$
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())).list().get(0); //$NON-NLS-1$
					type = suA.getType();
				}
			}finally{
				session.getTransaction().rollback();
			}
			if (type == null) return;
			
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryTempTable);
			sql.append(" SET ");//$NON-NLS-1$
			sql.append(getSortColumn(type));
			sql.append(" = null "); //$NON-NLS-1$
			c.createStatement().execute(sql.toString());

			if (sortColumn instanceof SurveyAttributeQueryColumn){
				switch (type) {
					case BOOLEAN:
					case NUMERIC:
					case TEXT:
					case DATE:
						sql = new StringBuilder();
						sql.append("UPDATE "); //$NON-NLS-1$
						sql.append(queryTempTable);
						sql.append(" SET "); //$NON-NLS-1$
						sql.append(getSortColumn(type));
						sql.append(" = "); //$NON-NLS-1$
						sql.append("(SELECT wpoa."); //$NON-NLS-1$
						sql.append(getSortValueField(type));
						sql.append(" FROM "); //$NON-NLS-1$
						sql.append("smart.WP_OBSERVATION_ATTRIBUTES wpoa join smart.DM_ATTRIBUTE a on a.uuid = wpoa.attribute_uuid "); //$NON-NLS-1$
						sql.append("and a.keyid = '"); //$NON-NLS-1$
						sql.append(key);
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
			}else if (sortColumn instanceof MissionPropertyQueryColumn){
				//mission attributes
				switch (type) {
				case NUMERIC:
				case TEXT:
					sql = new StringBuilder();
					sql.append("UPDATE "); //$NON-NLS-1$
					sql.append(queryTempTable);
					sql.append(" SET "); //$NON-NLS-1$
					sql.append(getSortColumn(type));
					sql.append( " = "); //$NON-NLS-1$
					sql.append("(SELECT "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append("." + getSortValueField(type)); //$NON-NLS-1$
					sql.append(" FROM "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(MissionPropertyValue.class));
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(MissionAttribute.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionAttribute.class));
					sql.append(".uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append(".mission_attribute_uuid AND "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionAttribute.class));
					sql.append(".keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(" WHERE "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append(".mission_uuid = "); //$NON-NLS-1$
					sql.append( queryTempTable );
					sql.append(".mission_uuid)"); //$NON-NLS-1$
					c.createStatement().execute(sql.toString());
					break;
				
				case LIST:
					sql = new StringBuilder();
					sql.append("UPDATE "); //$NON-NLS-1$
					sql.append(queryTempTable);
					sql.append(" SET "); //$NON-NLS-1$
					sql.append(getSortColumn(type));
					sql.append( " = "); //$NON-NLS-1$
					sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(MissionPropertyValue.class));
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append( queryTempTable + "_MLIST rl on rl.uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append(".list_element_uuid "); //$NON-NLS-1$
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(MissionAttribute.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionAttribute.class));
					sql.append(".uuid =  "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append(".mission_attribute_uuid "); //$NON-NLS-1$
					sql.append("and "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionAttribute.class));
					sql.append(".keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(" WHERE "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(MissionPropertyValue.class));
					sql.append(".mission_uuid = "); //$NON-NLS-1$
					sql.append( queryTempTable); 
					sql.append(".mission_uuid)"); //$NON-NLS-1$
					c.createStatement().execute(sql.toString());
					
					break;
					default: throw new SQLException(MessageFormat.format(Messages.AbstractSurveyPagedResult_missionAttributeTypeNotSupportedSort, type.name()));
				}
			}else if (sortColumn instanceof SamplingUnitAttributeQueryColumn){
				switch (type) {
				case NUMERIC:
				case TEXT:
					sql = new StringBuilder();
					sql.append("UPDATE "); //$NON-NLS-1$
					sql.append(queryTempTable);
					sql.append(" SET "); //$NON-NLS-1$
					sql.append(getSortColumn(type));
					sql.append( " = "); //$NON-NLS-1$
					sql.append(" (SELECT "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append("." + getSortValueField(type)); //$NON-NLS-1$
					sql.append(" FROM "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(SamplingUnitAttributeValue.class));
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(SamplingUnitAttribute.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttribute.class));
					sql.append(".uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append(".su_attribute_uuid AND "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttribute.class));
					sql.append(".keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(" WHERE "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append(".su_uuid = "); //$NON-NLS-1$
					sql.append( queryTempTable );
					sql.append(".samplingunit_uuid)"); //$NON-NLS-1$
					c.createStatement().execute(sql.toString());
					break;
				case LIST:
					sql = new StringBuilder();
					sql.append("UPDATE "); //$NON-NLS-1$
					sql.append(queryTempTable);
					sql.append(" SET "); //$NON-NLS-1$
					sql.append(getSortColumn(type));
					sql.append( " = "); //$NON-NLS-1$
					sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(SamplingUnitAttributeValue.class));
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append( queryTempTable + "_suLIST rl on rl.uuid = "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append(".list_element_uuid "); //$NON-NLS-1$
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(engine.tableNamePrefix(SamplingUnitAttribute.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttribute.class));
					sql.append(".uuid =  "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append(".su_attribute_uuid "); //$NON-NLS-1$
					sql.append("and "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttribute.class));
					sql.append(".keyid = '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(" WHERE "); //$NON-NLS-1$
					sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
					sql.append(".su_uuid = "); //$NON-NLS-1$
					sql.append( queryTempTable); 
					sql.append(".samplingunit_uuid)"); //$NON-NLS-1$
					c.createStatement().execute(sql.toString());
					
					break;
					default: throw new SQLException(MessageFormat.format(Messages.AbstractSurveyPagedResult_suAttributeTypeNotSupportedSort, type.name()));
				}
			} //end mission attributes
		}//end sort column
		c.commit();
	}
		
	
	protected String getSortColumn(AttributeType type) {
		switch (type) {
		case BOOLEAN:
		case NUMERIC:
			return "sortKeyDbl"; //$NON-NLS-1$

		case TEXT:
		case LIST:
		case TREE:
		case DATE:
			return "sortKeyTxt"; //$NON-NLS-1$
		}
		return null;
	}
	
	protected String getSortValueField(AttributeType type) {
		if (type == AttributeType.NUMERIC){
			return "number_value"; //$NON-NLS-1$
		}else if (type == AttributeType.TEXT ||
				type == AttributeType.DATE){
			return "string_value"; //$NON-NLS-1$
		}
		return null;
	}
	
	protected void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {
		boolean hasObservations = false;
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.ca_uuid FROM "); //$NON-NLS-1$
		attrSql.append(queryTempTable);
		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
		for (IResultItem irt : result) {
			SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
			if (it.getObservationUuid() != null) {
				if (hasObservations) {
					attrSql.append(',');
				}
				hasObservations = true;
				attrSql.append("x'").append(UuidUtils.uuidToString(it.getObservationUuid())).append('\''); //$NON-NLS-1$
			}
		}
		
		
		if (!hasObservations) {
			//no observations in current data fragment, so no need to select attributes as they will be empty
			return;
		}
		attrSql.append(')');

		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
			for (IResultItem irt : result) {
				SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
				if (it.getObservationUuid() != null) {
					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
					if (attributes != null) {
						it.setAttributes(attributes);
					}
				}
			}
		}	
	}
	
	protected void attachMissionProperties(List<IResultItem> result, Connection c, Session session) throws SQLException {
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT mpv.mission_uuid, ma.keyid, mpv.number_value,  mpv.string_value, mpv.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.mission_attribute ma join smart.mission_property_value mpv on mpv.mission_attribute_uuid = ma.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE mpv.mission_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		for (IResultItem irt : result) {
			UUID muuid = null;
			if (irt instanceof SurveyQueryResultItem){
				muuid = ((SurveyQueryResultItem) irt).getMissionUuid();
			}else if (irt instanceof MissionTrackResultItem){
				muuid = ((MissionTrackResultItem) irt).getMissionUuid();
			}
			if (muuid != null){
				if (hasItem) attrSql.append(","); //$NON-NLS-1$
				attrSql.append("x'").append(UuidUtils.uuidToString(muuid)).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				hasItem = true;
			}
		}
		
		
		if (!hasItem) {
			//no missions
			return;
		}
		attrSql.append(')');
		
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			while(rs.next()){
				byte[] muuid = rs.getBytes(1);
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);
				
				for (IResultItem irt : result) {
					if (irt instanceof SurveyQueryResultItem){
						SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
						if (muuid.equals(it.getMissionUuid())){
							if (rs.getObject(3) != null){
								it.addMissionPropertyValue(key, dvalue);
							}else if (svalue != null){
								it.addMissionPropertyValue(key,  svalue);
							}else if (rs.getObject(5) != null){
								it.addMissionPropertyValue(key, 
									((MissionAttributeListItem)session.load(MissionAttributeListItem.class, rs.getBytes(5))).getName());
							}
						}
					}else if (irt instanceof MissionTrackResultItem){
						MissionTrackResultItem it = (MissionTrackResultItem)irt;
						if (muuid.equals(it.getMissionUuid())){
							if (rs.getObject(3) != null){
								it.addMissionPropertyValue(key, dvalue);
							}else if (svalue != null){
								it.addMissionPropertyValue(key,  svalue);
							}else if (rs.getObject(5) != null){
								it.addMissionPropertyValue(key, 
									((MissionAttributeListItem)session.load(MissionAttributeListItem.class, rs.getBytes(5))).getName());
							}
						}
					}
				}
				
			}
		}		
	}
	
	
	protected void attachSamplingUnitAttributes(List<IResultItem> result, Connection c, Session session) throws SQLException {
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT suav.su_uuid, sua.keyid, suav.number_value, suav.string_value, suav.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.sampling_unit_attribute sua join smart.sampling_unit_attribute_value suav"); //$NON-NLS-1$
		attrSql.append(" on suav.su_attribute_uuid = sua.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE suav.su_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		for (IResultItem irt : result) {
			SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
			if (it.getSamplingUnitUuid() != null){
				if (hasItem) attrSql.append(","); //$NON-NLS-1$
				attrSql.append("x'").append(UuidUtils.uuidToString(it.getSamplingUnitUuid())).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				hasItem = true;
			}
		}
		
		
		if (!hasItem) {
			//no missions
			return;
		}
		attrSql.append(')');

		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			while(rs.next()){
				byte[] muuid = rs.getBytes(1);
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);
				
				for (IResultItem irt : result) {
				
					SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
					if (muuid.equals(it.getSamplingUnitUuid())){
						if (rs.getObject(3) != null){
							it.addSamplingUnitAttributeValue(key, dvalue);
						}else if (svalue != null){
							it.addSamplingUnitAttributeValue(key,  svalue);
						}else if (rs.getObject(5) != null){
							String value = ((SamplingUnitAttributeListItem)session.load(SamplingUnitAttributeListItem.class, rs.getBytes(5))).getName();
							it.addSamplingUnitAttributeValue(key, value);
						}
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
	
	protected List<IResultItem> getResults(ResultSet rs, int from, int pageSize, Session session) throws SQLException {
		List<IResultItem> items = new ArrayList<IResultItem>();
		rs.absolute(from);
		int to = from + pageSize;
		if (to >= itemCount) {
			to = itemCount;
		}
		for(int x = from; x < to; x++) {
			rs.next();
			IResultItem it = engine.asQueryResultItem(rs, session);
			items.add(it);
		}
		return items;
	}
	
	public List<byte[]> getMissionUuids() {
		final Session session = HibernateManager.openSession();
		final List<byte[]> uuids = new ArrayList<byte[]>();
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				String sql = "SELECT distinct mission_uuid FROM " + queryTempTable; //$NON-NLS-1$
				try(ResultSet rs = c.createStatement().executeQuery(sql)){
					while(rs.next()){
						uuids.add(rs.getBytes(1));
					}
				}
			}});
		
		return uuids;
	}
	
}
