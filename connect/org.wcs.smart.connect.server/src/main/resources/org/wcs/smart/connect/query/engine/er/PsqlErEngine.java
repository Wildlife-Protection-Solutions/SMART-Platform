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
package org.wcs.smart.connect.query.engine.er;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.columns.SurveyQueryColumnProvider;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Survey query engine with shared functions for populating extra 
 * data columns.
 * 
 * @author Emily
 *
 */
public abstract class PsqlErEngine extends AbstractQueryEngine{
	
	protected void populateMissionLeader(Connection c, Session session, String queryDataTable) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".mission_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" a join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionMember.class));
		sql.append(" on a.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".mission_uuid"); //$NON-NLS-1$
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionMember.class));
		sql.append(".is_leader"); //$NON-NLS-1$

		
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		updateSql += "mission_leader = ? where mission_uuid = ?"; //$NON-NLS-1$
		logger.finest(updateSql);
		PreparedStatement leaderSt = c.prepareStatement(updateSql);

		int cnt = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				String name = getEmployeeName(uuid, session);

				if (name != null) {
					leaderSt.setString(1, name);
					leaderSt.setObject(2, (UUID)rs.getObject(2));
					leaderSt.addBatch();

					cnt++;
					if (cnt >= 100) {
						leaderSt.executeBatch();
						cnt = 0;
					}
				}
			}
			leaderSt.executeBatch();
		}
	}
	protected void populateCaDetails(Connection c, String queryDataTable, Query query) throws SQLException{
		if (query.getConservationArea().equals(ConservationArea.MULTIPLE_CA)){
			//ca id and names are only used for cross-ca analysis
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + ".p_ca_uuid)");  //$NON-NLS-1$//$NON-NLS-2$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}

	}
	protected void populateAdditionalMissionTable(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			String queryDataTable,
			WpoaLinkedData linkedData) throws SQLException {
		
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable + linkedData.getPostfix());
		sql.append(" (uuid UUID, value varchar(1024))"); //$NON-NLS-1$ 
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionPropertyValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$

		StringBuilder sql2 = new StringBuilder();
		sql2.append("INSERT INTO "); //$NON-NLS-1$
		sql2.append( queryDataTable + linkedData.getPostfix());
		sql2.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		logger.finest(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2.toString());
		int count = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				if (uuid != null) {
//					byte[] cauuid = rs.getBytes(2);
//					String value = linkedData.getLabel(session, UuidUtils.byteToUUID(cauuid), UuidUtils.byteToUUID(uuid));
					String value = Label.getDescription(uuid, session);
					statement.setObject(1, uuid);
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
		

		List<MissionAttribute> attributes = new ArrayList<MissionAttribute>();
		if (sdFilter == null || sdFilter.getKey() == null){
			//get all mission properties
			attributes = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in ("conservationArea.uuid" ,caFilter.getConservationAreaFilterIds()))
				.list();
			//TODO: this will not support ccaa queries (attributes will not be merged);
		}else{
			//get mission properties for survey design only
			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
			for (MissionProperty mp : sd.getMissionProperties()){
				attributes.add(mp.getAttribute());
			}
		}
		for (MissionAttribute ma : attributes){
			sql = new StringBuilder();
			sql.append("ALTER TABLE ");
			sql.append(queryDataTable);
			sql.append(" ADD ma_" + ma.getKeyId());
			if (ma.getType() == AttributeType.NUMERIC){
				sql.append(" double precision");
			}else{
				sql.append(" varchar ");
			}
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
			
			if (ma.getType() == AttributeType.TEXT ||
					ma.getType() == AttributeType.NUMERIC){
				StringBuilder attrSql = new StringBuilder();
				attrSql.append("UPDATE ");
				attrSql.append(queryDataTable);
				attrSql.append(" SET ma_" + ma.getKeyId() );
				attrSql.append(" = ");
				if (ma.getType() == AttributeType.TEXT){
					attrSql.append(" mpv.string_value ");	
				}else if (ma.getType() == AttributeType.NUMERIC){
					attrSql.append(" mpv.number_value");
				}
				attrSql.append(" FROM " + tableNamePrefix(MissionPropertyValue.class));
				attrSql.append(" WHERE ");
				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_attribute_uuid = '" + ma.getUuid().toString() + "'");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_uuid = ");
				attrSql.append(queryDataTable + ".mission_uuid");
				logger.finest(attrSql.toString());
				c.createStatement().execute(attrSql.toString());
				
			}else if (ma.getType() == AttributeType.LIST){
				//for each list item
				for (MissionAttributeListItem ai : ma.getAttributeList()){
					StringBuilder attrSql = new StringBuilder();
					attrSql.append("UPDATE ");
					attrSql.append(queryDataTable);
					attrSql.append(" SET ma_" + ma.getKeyId() );
					attrSql.append(" = ");
					attrSql.append("'" + ai.getName() + "'");
					attrSql.append(" FROM " + tableNamePrefix(MissionPropertyValue.class));
					attrSql.append(" WHERE ");
					attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_attribute_uuid = '" + ma.getUuid().toString() + "'");
					attrSql.append(" AND ");
					attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_uuid = ");
					attrSql.append(queryDataTable + ".mission_uuid");
					attrSql.append(" AND ");
					attrSql.append(tablePrefix(MissionPropertyValue.class) + ".list_element_uuid = '" + ai.getUuid().toString() +"'");
					
					logger.finest(attrSql.toString());
					c.createStatement().execute(attrSql.toString());
				}
			}
		}
	}

	protected void populateAdditionalSuTable(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			String queryDataTable,
			WpoaLinkedData linkedData) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(queryDataTable + linkedData.getPostfix());
		sql.append(" (uuid UUID, value varchar(1024))"); //$NON-NLS-1$ 
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SamplingUnitAttributeValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.samplingunit_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".su_attribute_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + linkedData.getUuidColumn()); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("INSERT INTO "); //$NON-NLS-1$
		sql2.append( queryDataTable + linkedData.getPostfix());
		sql2.append(" VALUES (?, ?)"); //$NON-NLS-1$ 
		logger.finest(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2.toString());
		int count = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				if (uuid != null) {
					String value = Label.getDescription(uuid, session);
					statement.setObject(1, uuid);
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
		
		List<SamplingUnitAttribute> attributes = new ArrayList<SamplingUnitAttribute>();
		if (sdFilter == null || sdFilter.getKey() == null){
			//get all mission properties
			attributes = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.in ("conservationArea.uuid" ,caFilter.getConservationAreaFilterIds()))
				.list();
			//TODO: this will not support ccaa queries (attributes will not be merged);
		}else{
			//get mission properties for survey design only
			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
			for (SurveyDesignSamplingUnitAttribute susd : sd.getSamplingUnitAttributes()){
				attributes.add(susd.getSamplingUnitAttribute());
			}
		}
		for (SamplingUnitAttribute su : attributes){
			sql = new StringBuilder();
			sql.append("ALTER TABLE ");
			sql.append(queryDataTable);
			sql.append(" ADD su_" + su.getKeyId());
			if (su.getType() == AttributeType.NUMERIC){
				sql.append(" double precision");
			}else{
				sql.append(" varchar ");
			}
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
			
			if (su.getType() == AttributeType.TEXT ||
					su.getType() == AttributeType.NUMERIC){
				StringBuilder attrSql = new StringBuilder();
				attrSql.append("UPDATE ");
				attrSql.append(queryDataTable);
				attrSql.append(" SET su_" + su.getKeyId() );
				attrSql.append(" = ");
				if (su.getType() == AttributeType.TEXT){
					attrSql.append(" suav.string_value ");	
				}else if (su.getType() == AttributeType.NUMERIC){
					attrSql.append(" suav.number_value");
				}
				attrSql.append(" FROM " + tableNamePrefix(SamplingUnitAttributeValue.class));
				attrSql.append(" WHERE ");
				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = '" + su.getUuid().toString() + "'");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_uuid = ");
				attrSql.append(queryDataTable + ".samplingunit_uuid");
				logger.finest(attrSql.toString());
				c.createStatement().execute(attrSql.toString());
				
			}else if (su.getType() == AttributeType.LIST){
				//for each list item
				for (SamplingUnitAttributeListItem ai : su.getAttributeList()){
					StringBuilder attrSql = new StringBuilder();
					attrSql.append("UPDATE ");
					attrSql.append(queryDataTable);
					attrSql.append(" SET su_" + su.getKeyId() );
					attrSql.append(" = ");
					attrSql.append("'" + ai.getName() + "'");
					attrSql.append(" FROM " + tableNamePrefix(SamplingUnitAttributeValue.class));
					attrSql.append(" WHERE ");
					attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = '" + su.getUuid().toString() + "'");
					attrSql.append(" AND ");
					attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_uuid = ");
					attrSql.append(queryDataTable + ".samplingunit_uuid");
					attrSql.append(" AND ");
					attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".list_element_uuid = '" + ai.getUuid().toString() +"'");
					
					logger.finest(attrSql.toString());
					c.createStatement().execute(attrSql.toString());
				}
			}
		}
	}
	
	protected void populateAdditionalWpoaTable(Connection c, Session session,
			String queryDataTable,
			WpoaLinkedData linkedData) throws SQLException {
		String sql = "CREATE TABLE " + queryDataTable + linkedData.getPostfix() + " (uuid UUID, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);

		sql = "SELECT DISTINCT wpoa."+linkedData.getUuidColumn()+", r.CA_UUID FROM smart.wp_observation_attributes wpoa inner join "+queryDataTable+" r on wpoa.OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		
		String sql2 = "INSERT INTO "+queryDataTable+linkedData.getPostfix()+" VALUES (?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql2.toString());
		PreparedStatement statement = c.prepareStatement(sql2);
		int count = 0;
		logger.finest(sql.toString());
		try(ResultSet rs = c.createStatement().executeQuery(sql)){
			while (rs.next()) {
				UUID uuid = (UUID)rs.getObject(1);
				if (uuid != null) {
					UUID cauuid = (UUID)rs.getObject(2);
					String value = linkedData.getLabel(session, cauuid, uuid);
					statement.setObject(1, uuid);
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
	
	protected void populateTemporaryTableNameObjExtra(String uuidColumn, 
			String nameColumn, String queryDataTable,
			Connection c, Session session) throws SQLException {
		String sql = "SELECT DISTINCT ca_uuid, "+uuidColumn+" FROM "+queryDataTable;  //$NON-NLS-1$//$NON-NLS-2$
		logger.finest(sql);
		
		try (ResultSet rs = c.createStatement().executeQuery(sql)){
			PreparedStatement statement = c.prepareStatement("UPDATE "+ queryDataTable +" SET "+nameColumn+" = ? where "+uuidColumn+" = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int count = 0;
			while (rs.next()) {
				UUID ca_uuid = (UUID)rs.getObject(1);
				UUID uuid = (UUID)rs.getObject(2);
				if (uuid == null || ca_uuid == null)
					continue;
				String name = getName(uuid, ca_uuid, session);
				statement.setString(1, name);
				statement.setObject(2, uuid);
				statement.addBatch();
				count ++;
				if (count > 100){
					statement.executeBatch();
					count = 0;
				}				
			}
			statement.executeBatch();
			
		}
	}
	
	/**
	 * Wrapper class for populating linked data (additional columns)
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	protected abstract class WpoaLinkedData {
		private String postfix;
		private String uuidColumn;

		public WpoaLinkedData(String postfix, String uuidColumn) {
			super();
			this.postfix = postfix;
			this.uuidColumn = uuidColumn;
		}

		public String getPostfix() {
			return postfix;
		}

		public String getUuidColumn() {
			return uuidColumn;
		}
		
		public abstract String getLabel(Session session, UUID cauuid, UUID keyuuid);
	}

}
