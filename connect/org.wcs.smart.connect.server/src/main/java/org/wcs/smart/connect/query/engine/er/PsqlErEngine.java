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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;

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
	
	public abstract String getQueryDataTable();
	
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

	public String getMissionAttributeFromColumnName(String columnName){
		for (Entry<String,String> e : missionColumnMap.entrySet()){
			if (e.getValue().equalsIgnoreCase(columnName)) return e.getKey();
		}
		return null;
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
			String labelTable) throws SQLException {
		missionColumnMap = new HashMap<String, String>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + labelTable + " SELECT DISTINCT "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".list_element_uuid FROM "); //$NON-NLS-1$
		
		sql.append(tableNamePrefix(MissionPropertyValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(queryDataTable);
		sql.append(" r on r.mission_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".mission_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionPropertyValue.class));
		sql.append(".list_element_uuid is not null "); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		
//		//TODO: add support of CCAA queries
//		List<MissionAttribute> attributes = new ArrayList<MissionAttribute>();
//		if (sdFilter == null || sdFilter.getKey() == null){
//			//get all mission properties
//			CriteriaBuilder cb = session.getCriteriaBuilder();
//			CriteriaQuery<MissionAttribute> query = cb.createQuery(MissionAttribute.class);
//			Root<MissionAttribute> from = query.from(MissionAttribute.class);
//			query.where(from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$ //$NON-NLS-2$
//			attributes = session.createQuery(query).list();
//			
//		}else{
//			//get mission properties for survey design only
//			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
//			if (sd == null) throw new SQLException(MessageFormat.format(Messages.getString("PsqlErEngine.SdNotFound", locale), sdFilter.getKey())); //$NON-NLS-1$
//			for (MissionProperty mp : sd.getMissionProperties()){
//				attributes.add(mp.getAttribute());
//			}
//		}
//
//		int cnt = 0;
//		for (MissionAttribute ma : attributes){
//			cnt++;
//			String columnName = "ma_" + cnt; //$NON-NLS-1$
//			missionColumnMap.put(ma.getKeyId(), columnName);
//			
//			sql = new StringBuilder();
//			sql.append("ALTER TABLE "); //$NON-NLS-1$
//			sql.append(queryDataTable);
//			sql.append(" ADD " + columnName); //$NON-NLS-1$
//			if (ma.getType() == AttributeType.NUMERIC){
//				sql.append(" double precision"); //$NON-NLS-1$
//			}else{
//				sql.append(" varchar "); //$NON-NLS-1$
//			}
//			logger.finest(sql.toString());
//			c.createStatement().execute(sql.toString());
//			
//			if (ma.getType() == AttributeType.TEXT ||
//					ma.getType() == AttributeType.NUMERIC){
//				StringBuilder attrSql = new StringBuilder();
//				attrSql.append("UPDATE "); //$NON-NLS-1$
//				attrSql.append(queryDataTable);
//				attrSql.append(" SET " + columnName ); //$NON-NLS-1$
//				attrSql.append(" = "); //$NON-NLS-1$
//				if (ma.getType() == AttributeType.TEXT){
//					attrSql.append(" mpv.string_value ");	 //$NON-NLS-1$
//				}else if (ma.getType() == AttributeType.NUMERIC){
//					attrSql.append(" mpv.number_value"); //$NON-NLS-1$
//				}
//				attrSql.append(" FROM " + tableNamePrefix(MissionPropertyValue.class)); //$NON-NLS-1$
//				attrSql.append(" WHERE "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_attribute_uuid = '" + ma.getUuid().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_uuid = "); //$NON-NLS-1$
//				attrSql.append(queryDataTable + ".mission_uuid"); //$NON-NLS-1$
//				logger.finest(attrSql.toString());
//				c.createStatement().execute(attrSql.toString());
//				
//			}else if (ma.getType() == AttributeType.LIST){
//				StringBuilder attrSql = new StringBuilder();
//				attrSql.append("UPDATE "); //$NON-NLS-1$
//				attrSql.append(queryDataTable);
//				attrSql.append(" SET  " + columnName ); //$NON-NLS-1$
//				attrSql.append(" = tmp.value"); //$NON-NLS-1$
//				attrSql.append(" FROM " + tableNamePrefix(MissionPropertyValue.class)); //$NON-NLS-1$
//				attrSql.append(", " + tableName + " tmp "); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" WHERE "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_attribute_uuid = '" + ma.getUuid().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(MissionPropertyValue.class) + ".mission_uuid = "); //$NON-NLS-1$
//				attrSql.append(queryDataTable + ".mission_uuid"); //$NON-NLS-1$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(MissionPropertyValue.class) + "." + obsAttUuidColumn + " = "); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" tmp.uuid"); //$NON-NLS-1$
//				
//				logger.finest(attrSql.toString());
//				c.createStatement().execute(attrSql.toString());
//			}
//		}
//		
//		updateLabel(c, queryDataTable, "uuid", "value"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void populateAdditionalSuTable(Connection c, Session session,
			SurveyDesignFilter sdFilter,
			ConservationAreaFilter caFilter,
			String dataTable, String labelTable) throws SQLException {
		suColumnMap = new HashMap<String, String>();

		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO " + labelTable + "(uuid) SELECT DISTINCT "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".list_element_uuid FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SamplingUnitAttributeValue.class));
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(dataTable);
		sql.append(" r on r.samplingunit_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".su_uuid WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(SamplingUnitAttributeValue.class));
		sql.append(".list_element_uuid is not null "); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
//		//TODO: add support of CCAA queries
//		List<SamplingUnitAttribute> attributes = new ArrayList<SamplingUnitAttribute>();
//		if (sdFilter == null || sdFilter.getKey() == null){
//			CriteriaBuilder cb = session.getCriteriaBuilder();
//			CriteriaQuery<SamplingUnitAttribute> query = cb.createQuery(SamplingUnitAttribute.class);
//			Root<SamplingUnitAttribute> from = query.from(SamplingUnitAttribute.class);
//			query.where(from.get("conservationArea").get("uuid").in(caFilter.getConservationAreaFilterIds())); //$NON-NLS-1$ //$NON-NLS-2$
//			attributes = session.createQuery(query).list();
//		}else{
//			SurveyDesign sd = SurveyQueryColumnProvider.getSurveyDesign(sdFilter.getKey(), session, caFilter);
//			for (SurveyDesignSamplingUnitAttribute susd : sd.getSamplingUnitAttributes()){
//				attributes.add(susd.getSamplingUnitAttribute());
//			}
//		}
//		int cnt = 0;
//		for (SamplingUnitAttribute su : attributes){
//			cnt++;
//			String columnName = "su_" + cnt; //$NON-NLS-1$
//			suColumnMap.put(su.getKeyId(), columnName);
//			
//			sql = new StringBuilder();
//			sql.append("ALTER TABLE "); //$NON-NLS-1$
//			sql.append(queryDataTable);
//			sql.append(" ADD " + columnName ); //$NON-NLS-1$
//			if (su.getType() == AttributeType.NUMERIC){
//				sql.append(" double precision"); //$NON-NLS-1$
//			}else{
//				sql.append(" varchar "); //$NON-NLS-1$
//			}
//			logger.finest(sql.toString());
//			c.createStatement().execute(sql.toString());
//			
//			if (su.getType() == AttributeType.TEXT ||
//					su.getType() == AttributeType.NUMERIC){
//				StringBuilder attrSql = new StringBuilder();
//				attrSql.append("UPDATE "); //$NON-NLS-1$
//				attrSql.append(queryDataTable);
//				attrSql.append(" SET " + columnName ); //$NON-NLS-1$
//				attrSql.append(" = "); //$NON-NLS-1$
//				if (su.getType() == AttributeType.TEXT){
//					attrSql.append(" suav.string_value ");	 //$NON-NLS-1$
//				}else if (su.getType() == AttributeType.NUMERIC){
//					attrSql.append(" suav.number_value"); //$NON-NLS-1$
//				}
//				attrSql.append(" FROM " + tableNamePrefix(SamplingUnitAttributeValue.class)); //$NON-NLS-1$
//				attrSql.append(" WHERE "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = '" + su.getUuid().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_uuid = "); //$NON-NLS-1$
//				attrSql.append(queryDataTable + ".samplingunit_uuid"); //$NON-NLS-1$
//				logger.finest(attrSql.toString());
//				c.createStatement().execute(attrSql.toString());
//				
//			}else if (su.getType() == AttributeType.LIST){
//				StringBuilder attrSql = new StringBuilder();
//				attrSql.append("UPDATE "); //$NON-NLS-1$
//				attrSql.append(queryDataTable);
//				attrSql.append(" SET " + columnName ); //$NON-NLS-1$
//				attrSql.append(" = tmp.value"); //$NON-NLS-1$
//				attrSql.append(" FROM " + tableNamePrefix(SamplingUnitAttributeValue.class)); //$NON-NLS-1$
//				attrSql.append(", " + tableName + " tmp "); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" WHERE "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = '" + su.getUuid().toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + ".su_uuid = "); //$NON-NLS-1$
//				attrSql.append(queryDataTable + ".samplingunit_uuid"); //$NON-NLS-1$
//				attrSql.append(" AND "); //$NON-NLS-1$
//				attrSql.append(tablePrefix(SamplingUnitAttributeValue.class) + "." + obsAttUuidColumn + " = "); //$NON-NLS-1$ //$NON-NLS-2$
//				attrSql.append(" tmp.uuid"); //$NON-NLS-1$
//				
//				logger.finest(attrSql.toString());
//				c.createStatement().execute(attrSql.toString());
//			}
//		}
//		
		

	}
	
	@Override
	public String getDateFilterTable() throws SQLException{
		return tablePrefix(MissionDay.class);
	}
	
	@Override
	public String getDateFilterField() throws SQLException{
		return "mission_day"; //$NON-NLS-1$
	}
	
	@Override
	public void createWaypointGroupTable(Connection c, String waypointTable, Collection<IWaypointSource> sources, ConservationAreaFilter caFilter, DateFilter dateFilter) throws SQLException {
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid uuid, wp_group_uuid uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + getIndexName(waypointTable) + "_wpuuid_idx on " + waypointTable + " (wp_group_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		clearParameters();
		sql = new StringBuilder();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid, wp_group_uuid) SELECT DISTINCT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class));
		sql.append(".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationGroup.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$

		sql.append(tableNamePrefix(Waypoint.class));

		sql.append(" left join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservationGroup.class));
		sql.append(" on " + tablePrefix(Waypoint.class) + ".uuid = " + tablePrefix(WaypointObservationGroup.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SurveyWaypoint.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyWaypoint.class));
		sql.append(".wp_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		
		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionDay.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyWaypoint.class));
		sql.append(".mission_day_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class));
		sql.append(".uuid "); //$NON-NLS-1$
		
		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Mission.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class));
		sql.append(".mission_uuid "); //$NON-NLS-1$

		sql.append(" WHERE "); //$NON-NLS-1$

		sql.append("source in ("); //$NON-NLS-1$
		for (IWaypointSource src : sources) {
			sql.append("'" + src.getKey() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(")"); //$NON-NLS-1$
		if (caFilter != null) {
			String cfilter = PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, tablePrefix(Waypoint.class), this);
			if (cfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(cfilter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}

		if (dateFilter != null) {
			String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, this);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(dfilter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}

		logger.finest(sql.toString());
		try (NamedPreparedStatement ps = parseQueryString(c, sql.toString())) {
			ps.executeUpdate();
		}
	}
	
	@Override
	public void createWaypointTable(Connection c, String waypointTable, Collection<IWaypointSource> sources, ConservationAreaFilter caFilter, DateFilter dateFilter) throws SQLException {
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + getIndexName(waypointTable) + "_wpuuid_idx on " + waypointTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid) SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$
		sql.append(tableName(Waypoint.class));
		sql.append(" as ");//$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class));

		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(SurveyWaypoint.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyWaypoint.class));
		sql.append(".wp_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		
		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(MissionDay.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(SurveyWaypoint.class));
		sql.append(".mission_day_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class));
		sql.append(".uuid "); //$NON-NLS-1$
		
		sql.append (" JOIN "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Mission.class));
		sql.append(" ON "); //$NON-NLS-1$
		sql.append(tablePrefix(Mission.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(MissionDay.class));
		sql.append(".mission_uuid "); //$NON-NLS-1$
		
		sql.append(" WHERE "); //$NON-NLS-1$

		sql.append("source in ("); //$NON-NLS-1$
		for (IWaypointSource src : sources) {
			sql.append("'" + src.getKey() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(") "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String cfilter = PsqlFilterToSqlGenerator.INSTANCE.asSql(caFilter, tablePrefix(Waypoint.class), this);
			if (cfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(cfilter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}

		if (dateFilter != null) {
			String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, this);
			if (dfilter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(dfilter);
				sql.append(" ) "); //$NON-NLS-1$
			}
		}

		logger.finest(sql.toString());
		try (NamedPreparedStatement ps = parseQueryString(c, sql.toString())) {
			ps.executeUpdate();
		}
	}
}
