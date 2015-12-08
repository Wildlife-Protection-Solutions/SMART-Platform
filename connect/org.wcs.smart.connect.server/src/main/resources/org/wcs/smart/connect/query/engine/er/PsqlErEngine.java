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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.connect.query.columns.SurveyQueryColumnProvider;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Survey query engine with shared functions for populating extra 
 * data columns.
 * 
 * @author Emily
 *
 */
public abstract class PsqlErEngine extends AbstractQueryEngine{
	
	//maps a mission attribute key to a database table column
	//postgresql does not support column names that are 
	//as long as our maximum key length so we cannot use
	//the keyid as the column id
	private HashMap<String, String> missionColumnMap = null;
	private HashMap<String, String> suColumnMap = null;
	
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

	public String getMissionAttributeColumnName(String missionAttributeKey){
		return missionColumnMap.get(missionAttributeKey);
	}
	
	public String getSamplingUnitAttributeColumnName(String missionAttributeKey){
		return suColumnMap.get(missionAttributeKey);
	}
	
	protected void populateAdditionalMissionTable(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			String queryDataTable,
			String tableName, String obsAttUuidColumn) throws SQLException {
		missionColumnMap = new HashMap<String, String>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append(" (uuid UUID, value varchar(1024))"); //$NON-NLS-1$ 
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("INSERT INTO " + tableName + " SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + obsAttUuidColumn); //$NON-NLS-1$
		sql.append(", r.ca_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionPropertyValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append("." + obsAttUuidColumn); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$

		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		updateLabel(c, tableName, "uuid", "value");
		
		//TODO: add support of CCAA queries
		List<MissionAttribute> attributes = new ArrayList<MissionAttribute>();
		if (sdFilter == null || sdFilter.getKey() == null){
			//get all mission properties
			attributes = session.createCriteria(MissionAttribute.class)
				.add(Restrictions.in ("conservationArea.uuid" ,caFilter.getConservationAreaFilterIds()))
				.list();
		}else{
			//get mission properties for survey design only
			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
			for (MissionProperty mp : sd.getMissionProperties()){
				attributes.add(mp.getAttribute());
			}
		}

		int cnt = 0;
		for (MissionAttribute ma : attributes){
			cnt++;
			String columnName = "ma_" + cnt;
			missionColumnMap.put(ma.getKeyId(), columnName);
			
			sql = new StringBuilder();
			sql.append("ALTER TABLE ");
			sql.append(queryDataTable);
			sql.append(" ADD " + columnName);
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
				attrSql.append(" SET " + columnName );
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
				StringBuilder attrSql = new StringBuilder();
				attrSql.append("UPDATE ");
				attrSql.append(queryDataTable);
				attrSql.append(" SET  " + columnName );
				attrSql.append(" = tmp.value");
				attrSql.append(" FROM " + tableNamePrefix(MissionPropertyValue.class));
				attrSql.append(", " + tableName + " tmp ");
				attrSql.append(" WHERE ");
				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_attribute_uuid = '" + ma.getUuid().toString() + "'");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_uuid = ");
				attrSql.append(queryDataTable + ".mission_uuid");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(MissionPropertyValue.class) + "." + obsAttUuidColumn + " = ");
				attrSql.append(" tmp.uuid");
				
				logger.finest(attrSql.toString());
				c.createStatement().execute(attrSql.toString());
			}
		}
	}

	protected void populateAdditionalSuTable(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			String queryDataTable,
			String tableName, String obsAttUuidColumn) throws SQLException {
		suColumnMap = new HashMap<String, String>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append(" (uuid UUID, value varchar(1024))"); //$NON-NLS-1$ 
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		sql = new StringBuilder();
		sql.append("INSERT INTO " + tableName + "(uuid) SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + obsAttUuidColumn); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SamplingUnitAttributeValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.samplingunit_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".su_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append("." + obsAttUuidColumn); //$NON-NLS-1$
		sql.append(" is not null "); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		updateLabel(c, tableName, "uuid", "value");
		
		//TODO: add support of CCAA queries
		List<SamplingUnitAttribute> attributes = new ArrayList<SamplingUnitAttribute>();
		if (sdFilter == null || sdFilter.getKey() == null){
			attributes = session.createCriteria(SamplingUnitAttribute.class)
				.add(Restrictions.in ("conservationArea.uuid" ,caFilter.getConservationAreaFilterIds()))
				.list();
		}else{
			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
			for (SurveyDesignSamplingUnitAttribute susd : sd.getSamplingUnitAttributes()){
				attributes.add(susd.getSamplingUnitAttribute());
			}
		}
		int cnt = 0;
		for (SamplingUnitAttribute su : attributes){
			cnt++;
			String columnName = "su_" + cnt;
			suColumnMap.put(su.getKeyId(), columnName);
			
			sql = new StringBuilder();
			sql.append("ALTER TABLE ");
			sql.append(queryDataTable);
			sql.append(" ADD " + columnName );
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
				attrSql.append(" SET " + columnName );
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
				StringBuilder attrSql = new StringBuilder();
				attrSql.append("UPDATE ");
				attrSql.append(queryDataTable);
				attrSql.append(" SET " + columnName );
				attrSql.append(" = tmp.value");
				attrSql.append(" FROM " + tableNamePrefix(SamplingUnitAttributeValue.class));
				attrSql.append(", " + tableName + " tmp ");
				attrSql.append(" WHERE ");
				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = '" + su.getUuid().toString() + "'");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_uuid = ");
				attrSql.append(queryDataTable + ".samplingunit_uuid");
				attrSql.append(" AND ");
				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "." + obsAttUuidColumn + " = ");
				attrSql.append(" tmp.uuid");
				
				logger.finest(attrSql.toString());
				c.createStatement().execute(attrSql.toString());
			}
		}
	}
	
	protected void populateAdditionalWpoaTable(Connection c, Session session,
			String queryDataTable,
			String tableName, String obsAttUuidColumn) throws SQLException {
	
		String sql = "CREATE TABLE " + tableName + " (uuid UUID, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);

		sql = "INSERT INTO " + tableName + "(uuid) SELECT DISTINCT wpoa."+obsAttUuidColumn
				+" FROM smart.wp_observation_attributes wpoa inner join "
				+queryDataTable+" r on wpoa.OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql);
		updateLabel(c, tableName, "uuid", "value");
		
	}
}
