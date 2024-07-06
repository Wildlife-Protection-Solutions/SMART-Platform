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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.hibernate.Session;
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
import org.wcs.smart.er.query.model.ISamplingUnitResultItem;
import org.wcs.smart.er.query.model.ISurveyQueryResultItem;
import org.wcs.smart.er.query.model.SurveyObservationResultItem;
import org.wcs.smart.er.query.model.SurveyQueryColumn;
import org.wcs.smart.er.query.model.SurveyWaypointResultItem;
import org.wcs.smart.er.query.model.column.MissionPropertyQueryColumn;
import org.wcs.smart.er.query.model.column.SamplingUnitAttributeQueryColumn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.ObservationQueryResult;
import org.wcs.smart.query.model.AttributeQueryColumn;
import org.wcs.smart.query.model.CategoryQueryColumn;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.QueryColumn.ColumnType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Shared functions for survey query results
 * 
 * @author Emily
 *
 */
public class SurveyPagedResultUtils  {

	public static List<byte[]> getMissionUuids(String dataTable){
		try(final Session session = HibernateManager.openSession()){
			final List<byte[]> uuids = new ArrayList<byte[]>();
			session.doWork(new Work(){
				@Override
				public void execute(Connection c) throws SQLException {
					String sql = "SELECT distinct mission_uuid FROM " + dataTable; //$NON-NLS-1$
					try(ResultSet rs = c.createStatement().executeQuery(sql)){
						while(rs.next()){
							uuids.add(rs.getBytes(1));
						}
					}
				}});
			
			return uuids;
		}
	}
	
	public static void processSortColumn(String dataTable, String labelTable, DerbySurveyQueryEngine engine,
			boolean hasSortColumns, QueryColumn sortColumn, Connection c, Session session) throws SQLException {
		if (!hasSortColumns){
			//add the sort columns
			c.createStatement().execute("ALTER TABLE " + dataTable + " add column " + ObservationQueryResult.NUMBER_SORT +" double"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			c.createStatement().execute("ALTER TABLE " + dataTable + " add column " + ObservationQueryResult.TXT_SORT + " varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			c.commit();
			hasSortColumns = true;
		}
		
		String key = sortColumn.getKey();
		key = key.split(":")[1]; //$NON-NLS-1$
		Attribute.AttributeType type = null;
		
		
		session.beginTransaction();
		try{
			if (sortColumn instanceof MissionPropertyQueryColumn){
				MissionAttribute prop = QueryFactory.buildQuery(session, MissionAttribute.class,
							new Object[] {"keyId", key}, //$NON-NLS-1$
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$

				type = prop.getType();
			}else if (sortColumn instanceof SamplingUnitAttributeQueryColumn){
				SamplingUnitAttribute suA = QueryFactory.buildQuery(session, SamplingUnitAttribute.class,
						new Object[] {"keyId", key}, //$NON-NLS-1$
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).uniqueResult(); //$NON-NLS-1$
				type = suA.getType();
			}
		}finally{
			session.getTransaction().rollback();
		}
		if (type == null) return;
		
		String col = ObservationQueryResult.TXT_SORT;
		if (type == AttributeType.BOOLEAN || type == AttributeType.NUMERIC) {
			col = ObservationQueryResult.NUMBER_SORT;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE "); //$NON-NLS-1$
		sql.append(dataTable);
		sql.append(" SET ");//$NON-NLS-1$
		sql.append(col);
		sql.append(" = null "); //$NON-NLS-1$
		c.createStatement().execute(sql.toString());

		if (sortColumn instanceof MissionPropertyQueryColumn){
			//mission attributes
			switch (type) {
			case NUMERIC:
			case TEXT:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(dataTable);
				sql.append(" SET "); //$NON-NLS-1$
				sql.append( col );
				sql.append( " = "); //$NON-NLS-1$
				sql.append("(SELECT "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(MissionPropertyValue.class));
				sql.append("."); //$NON-NLS-1$
				if (type == AttributeType.NUMERIC) {
					sql.append("number_value"); //$NON-NLS-1$
				}else if (type == AttributeType.TEXT) {
					sql.append("string_value"); //$NON-NLS-1$
				}
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
				sql.append( dataTable );
				sql.append(".mission_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			
			case LIST:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(dataTable);
				sql.append(" SET "); //$NON-NLS-1$
				sql.append( col);
				sql.append( " = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(MissionPropertyValue.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append( labelTable + " rl on rl.uuid = "); //$NON-NLS-1$
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
				sql.append( dataTable); 
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
				sql.append(dataTable);
				sql.append(" SET "); //$NON-NLS-1$
				sql.append(col);
				sql.append( " = "); //$NON-NLS-1$
				sql.append(" (SELECT "); //$NON-NLS-1$
				sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
				sql.append("."); //$NON-NLS-1$
				if (type == AttributeType.NUMERIC) {
					sql.append("number_value"); //$NON-NLS-1$
				}else if (type == AttributeType.TEXT) {
					sql.append("string_value"); //$NON-NLS-1$
				}
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
				sql.append( dataTable );
				sql.append(".samplingunit_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				break;
			case LIST:
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(dataTable);
				sql.append(" SET "); //$NON-NLS-1$
				sql.append( col );
				sql.append( " = "); //$NON-NLS-1$
				sql.append("(SELECT rl.value FROM "); //$NON-NLS-1$
				sql.append(engine.tableNamePrefix(SamplingUnitAttributeValue.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append( labelTable + " rl on rl.uuid = "); //$NON-NLS-1$
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
				sql.append( dataTable); 
				sql.append(".samplingunit_uuid)"); //$NON-NLS-1$
				c.createStatement().execute(sql.toString());
				
				break;
				default: throw new SQLException(MessageFormat.format(Messages.AbstractSurveyPagedResult_suAttributeTypeNotSupportedSort, type.name()));
			}
		} //end mission attributes
	}
	public static void populateAdditionalAttributeTable(boolean create, 
			boolean mission, boolean samplingunits, String datatable, 
			String labeltable, DerbySurveyQueryEngine engine, Connection c, Session session) throws SQLException {
		
		
		StringBuilder sql = new StringBuilder();
		if (create) {
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(labeltable); 
			sql.append(" (uuid char(16) for bit data, value varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + "))"); //$NON-NLS-1$ //$NON-NLS-2$ 
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
		}

		sql = new StringBuilder();
		//mission attributes
		if (mission) {
			sql.append("SELECT DISTINCT "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(MissionPropertyValue.class));
			sql.append(".list_element_uuid"); //$NON-NLS-1$
			sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(MissionPropertyValue.class));
			sql.append(" inner join "); //$NON-NLS-1$
			sql.append(datatable);
			sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(MissionPropertyValue.class));
			sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(MissionPropertyValue.class));
			sql.append(".list_element_uuid"); //$NON-NLS-1$
			sql.append(" is not null "); //$NON-NLS-1$
			
		}
		if (mission && samplingunits) {
			sql.append(" UNION "); //$NON-NLS-1$
		}
		if (samplingunits) {
			//su attributes
			sql.append("SELECT DISTINCT "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
			sql.append(".list_element_uuid"); //$NON-NLS-1$
			sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
			sql.append(engine.tableNamePrefix(SamplingUnitAttributeValue.class));
			sql.append(" inner join "); //$NON-NLS-1$
			sql.append(datatable);
			sql.append(" r on r.samplingunit_uuid = "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
			sql.append(".su_attribute_uuid WHERE "); //$NON-NLS-1$
			sql.append(engine.tablePrefix(SamplingUnitAttributeValue.class));
			sql.append(".list_element_uuid"); //$NON-NLS-1$
			sql.append(" is not null "); //$NON-NLS-1$
		}
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("INSERT INTO "); //$NON-NLS-1$
		sql2.append(labeltable ); 
		sql2.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		QueryPlugIn.logSql(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2.toString());
		QueryPlugIn.logSql(sql.toString());
		
		int count = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				byte[] uuid = rs.getBytes(1);
				if (uuid != null) {
					byte[] cauuid = rs.getBytes(2);
					String value = SmartLabelProvider.getDescription(UuidUtils.byteToUUID(uuid), UuidUtils.byteToUUID(cauuid), session);
					statement.setBytes(1, uuid);
					statement.setString(2, value);
					statement.addBatch();
					count++;
					if (count >= 100){
						statement.executeBatch();
						count = 0;
					}
				}
			}
			statement.executeBatch();
		} 
	}
	
	public static String buildSortSql(QueryColumn sortColumn, int direction) {
		if (sortColumn == null || direction == SWT.NONE) return "";			 //$NON-NLS-1$
		
		String result = ""; //$NON-NLS-1$
		if (sortColumn instanceof SurveyQueryColumn) {
			String key = sortColumn.getKey();
			key = SurveyQueryColumn.getDbColumnName(key);
			if (sortColumn.getKey().equals(SurveyQueryColumn.FixedColumns.WAYPOINT_TIME.getKey())){
				result = "order by CAST(r." + key + " as TIME)"; //$NON-NLS-1$ //$NON-NLS-2$
			}else if (((SurveyQueryColumn)sortColumn).getKey().equals(SurveyQueryColumn.FixedColumns.OBS_GROUP_ID.getKey())) {
				result = "order by r."+key; //$NON-NLS-1$
			}else if (sortColumn.getType() == ColumnType.STRING){
				result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$	
			}else{
				result = "order by r."+key; //$NON-NLS-1$
			}
		}else if (sortColumn instanceof CategoryQueryColumn) {
			String key = sortColumn.getKey();
			key = CategoryQueryColumn.getDbColumnName(key);
			result = "order by UPPER(r."+key + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}else if (sortColumn instanceof AttributeQueryColumn ||
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
				case TIME:
					result = "order by TIME(sortKeyTxt)"; //$NON-NLS-1$
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
	
	
	
		
	
//	protected String getSortColumn(AttributeType type) {
//		switch (type) {
//		case BOOLEAN:
//		case NUMERIC:
//			return "sortKeyDbl"; //$NON-NLS-1$
//
//		case TEXT:
//		case LIST:
//		case TREE:
//		case DATE:
//			return "sortKeyTxt"; //$NON-NLS-1$
//		}
//		return null;
//	}
//	
//	protected String getSortValueField(AttributeType type) {
//		if (type == AttributeType.NUMERIC){
//			return "number_value"; //$NON-NLS-1$
//		}else if (type == AttributeType.TEXT ||
//				type == AttributeType.DATE){
//			return "string_value"; //$NON-NLS-1$
//		}
//		return null;
//	}
//	
//	protected void attachObservations(List<IResultItem> result, Connection c, Session session) throws SQLException {
//		boolean hasObservations = false;
//		StringBuilder attrSql = new StringBuilder();
//		attrSql.append("SELECT r.ob_uuid, a.keyid, wpoa.number_value, wpoa.string_value, rl.value as list_value, rt.value as tree_value, r.ca_uuid FROM "); //$NON-NLS-1$
//		attrSql.append(queryTempTable);
//		attrSql.append(" r left join smart.wp_observation_attributes wpoa on r.ob_uuid = wpoa.observation_uuid left join smart.dm_attribute a on a.uuid = wpoa.attribute_uuid left join "); //$NON-NLS-1$
//		attrSql.append(queryTempTable).append("_list rl on wpoa.list_element_uuid = rl.uuid left join "); //$NON-NLS-1$
//		attrSql.append(queryTempTable).append("_tree rt on wpoa.tree_node_uuid = rt.UUID WHERE r.ob_uuid in ("); //$NON-NLS-1$
//		for (IResultItem irt : result) {
//			SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
//			if (it.getObservationUuid() != null) {
//				if (hasObservations) {
//					attrSql.append(',');
//				}
//				hasObservations = true;
//				attrSql.append("x'").append(UuidUtils.uuidToString(it.getObservationUuid())).append('\''); //$NON-NLS-1$
//			}
//		}
//		
//		
//		if (!hasObservations) {
//			//no observations in current data fragment, so no need to select attributes as they will be empty
//			return;
//		}
//		attrSql.append(')');
//
//		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
//			HashMap<UUID, HashMap<String, Object>> attrMap = getResultsAttributes(rs, session);
//			for (IResultItem irt : result) {
//				SurveyQueryResultItem it = (SurveyQueryResultItem)irt;
//				if (it.getObservationUuid() != null) {
//					HashMap<String, Object> attributes = attrMap.get(it.getObservationUuid());
//					if (attributes != null) {
//						it.setAttributes(attributes);
//					}
//				}
//			}
//		}	
//	}
//	
	public static void attachMissionProperties(List<? extends IResultItem> result, Connection c, Session session) throws SQLException {
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT mpv.mission_uuid, ma.keyid, mpv.number_value,  mpv.string_value, mpv.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.mission_attribute ma join smart.mission_property_value mpv on mpv.mission_attribute_uuid = ma.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE mpv.mission_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		for (IResultItem irt : result) {
			UUID muuid = null;
			if (irt instanceof ISurveyQueryResultItem){
				muuid = ((ISurveyQueryResultItem) irt).getMissionUuid();
			}
			if (muuid != null){
				if (hasItem) attrSql.append(","); //$NON-NLS-1$
				attrSql.append("x'").append(UuidUtils.uuidToString(muuid)).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				hasItem = true;
			}
		}
		
		
		if (!hasItem) return; //no missions
		
		attrSql.append(')');
		
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			while(rs.next()){
				UUID muuid = UuidUtils.byteToUUID(rs.getBytes(1));
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);
				
				for (IResultItem irt : result) {
					if (irt instanceof ISurveyQueryResultItem){
						ISurveyQueryResultItem it = (ISurveyQueryResultItem)irt;
						if (muuid.equals(it.getMissionUuid())){
							if (rs.getObject(3) != null){
								it.addMissionPropertyValue(key, dvalue);
							}else if (svalue != null){
								it.addMissionPropertyValue(key,  svalue);
							}else if (rs.getObject(5) != null){
								it.addMissionPropertyValue(key, 
									((MissionAttributeListItem)session.get(MissionAttributeListItem.class, UuidUtils.byteToUUID(rs.getBytes(5)))).getName());
							}
						}
					}
				}
				
			}
		}		
	}
	
	
	public static void attachSamplingUnitAttributes(List<? extends IResultItem> result, Connection c, Session session) throws SQLException {
		
		StringBuilder attrSql = new StringBuilder();
		attrSql.append("SELECT suav.su_uuid, sua.keyid, suav.number_value, suav.string_value, suav.list_element_uuid FROM "); //$NON-NLS-1$
		attrSql.append("smart.sampling_unit_attribute sua join smart.sampling_unit_attribute_value suav"); //$NON-NLS-1$
		attrSql.append(" on suav.su_attribute_uuid = sua.uuid "); //$NON-NLS-1$
		attrSql.append(" WHERE suav.su_uuid IN ("); //$NON-NLS-1$

		boolean hasItem = false;
		for (IResultItem irt : result) {
			UUID muuid = null;
			if (irt instanceof SurveyObservationResultItem){
				muuid = ((SurveyObservationResultItem) irt).getSamplingUnitUuid();
			}else if (irt instanceof SurveyWaypointResultItem){
				muuid = ((SurveyWaypointResultItem) irt).getSamplingUnitUuid();
			}
			if (muuid != null){
				if (hasItem) attrSql.append(","); //$NON-NLS-1$
				attrSql.append("x'").append(UuidUtils.uuidToString(muuid)).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
				hasItem = true;
			}
		}		
		
		if (!hasItem) return;
		attrSql.append(")"); //$NON-NLS-1$
		
		try(ResultSet rs = c.createStatement().executeQuery(attrSql.toString())) {
			while(rs.next()){
				UUID muuid = UuidUtils.byteToUUID(rs.getBytes(1));
				String key = rs.getString(2);
				Double dvalue = rs.getDouble(3);
				String svalue = rs.getString(4);
				
				for (IResultItem irt : result) {
					if (irt instanceof ISamplingUnitResultItem){
						ISamplingUnitResultItem it = (ISamplingUnitResultItem)irt;
						if (muuid.equals(it.getSamplingUnitUuid())){
							if (rs.getObject(3) != null){
								it.addSamplingUnitAttributeValue(key, dvalue);
							}else if (svalue != null){
								it.addSamplingUnitAttributeValue(key,  svalue);
							}else if (rs.getObject(5) != null){
								String value = ((SamplingUnitAttributeListItem)session.get(SamplingUnitAttributeListItem.class,  UuidUtils.byteToUUID(rs.getBytes(5)))).getName();
								it.addSamplingUnitAttributeValue(key, value);
							}
						}
					}
				}				
			}
		}		
	}
	
//	protected HashMap<UUID, HashMap<String, Object>> getResultsAttributes(ResultSet rs, Session s) throws SQLException {
//		HashMap<UUID, HashMap<String, Object>> attrMap = new HashMap<UUID, HashMap<String, Object>>();
//		/*
//		1	OB_UUID
//		2	KEYID
//		3	NUMBER_VALUE
//		4	STRING_VALUE
//		5	LIST_VALUE
//		6	TREE_VALUE
//		7	P_CA_UUID
//		*/
//		while (rs.next()) {
//			byte[] obUuid = rs.getBytes(1);
//			if (obUuid == null)
//				continue;
//			UUID keyObj = UuidUtils.byteToUUID(obUuid);
//			HashMap<String, Object> attributes = attrMap.get(keyObj);
//			if (attributes == null) {
//				attributes = new HashMap<String, Object>();
//				attrMap.put(keyObj, attributes);
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
//	/**
//	 * Gets the attribute value from the result set for the given attribute.
//	 * 
//	 * @param att
//	 * @param rs
//	 * @param session
//	 * @return
//	 * @throws SQLException
//	 */
//	protected Object getAttributeValue(ResultSet rs, Session session) throws SQLException {
//		/*
//		1	OB_UUID
//		2	KEYID
//		3	NUMBER_VALUE
//		4	STRING_VALUE
//		5	LIST_VALUE
//		6	TREE_VALUE
//		7	P_CA_UUID
//		*/
//		if (rs.getObject(3) != null) {
//			return rs.getDouble(3);
//		}
//		String result = rs.getString(4); //string
//		if (result != null) {
//			return result;
//		}
//		result = rs.getString(5); //list
//		if (result != null) {
//			return result;
//		}
//		result = rs.getString(6); //tree
//		if (result != null) {
//			return result;
//		}
//		return null;
//	}
//
//	public List<byte[]> getMissionUuids() {
//		try(final Session session = HibernateManager.openSession()){
//			final List<byte[]> uuids = new ArrayList<byte[]>();
//			session.doWork(new Work(){
//				@Override
//				public void execute(Connection c) throws SQLException {
//					String sql = "SELECT distinct mission_uuid FROM " + queryTempTable; //$NON-NLS-1$
//					try(ResultSet rs = c.createStatement().executeQuery(sql)){
//						while(rs.next()){
//							uuids.add(rs.getBytes(1));
//						}
//					}
//				}});
//			
//			return uuids;
//		}
//	}
//	
//	@Override
//	public void dispose(Session session) throws SQLException{
//		super.dispose(session);
//		session.doWork(new Work(){
//			@Override
//			public void execute(Connection c) throws SQLException {
//				engine.dropTables(c);
//			}});
//	}
	
}
