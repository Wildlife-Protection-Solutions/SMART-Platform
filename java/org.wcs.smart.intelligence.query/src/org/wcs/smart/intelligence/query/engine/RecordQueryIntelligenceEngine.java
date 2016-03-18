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
package org.wcs.smart.intelligence.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilter;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordResultItem;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine;
import org.wcs.smart.query.common.engine.DerbyFilterToSqlGenerator;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.BracketFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.NotExpression;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.util.UuidUtils;

/**
 * Runs intelligence record queries, returning pages result set.
 * 
 * @author Emily
 *
 */
public class RecordQueryIntelligenceEngine extends AbstractQueryEngine {


	static {
		tablePrefix.put(Intelligence.class, "i"); //$NON-NLS-1$
		tablePrefix.put(Informant.class, "ii"); //$NON-NLS-1$
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(IntelligenceSource.class, "iis"); //$NON-NLS-1$
		tablePrefix.put(IntelligencePoint.class, "iip"); //$NON-NLS-1$
		tablePrefix.put(Label.class, "lbl"); //$NON-NLS-1$
		
		
		tableNames.put(Intelligence.class, "smart.intelligence"); //$NON-NLS-1$
		tableNames.put(Informant.class, "smart.informant"); //$NON-NLS-1$
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(IntelligenceSource.class, "smart.intelligence_source"); //$NON-NLS-1$
		tableNames.put(IntelligencePoint.class, "smart.intelligence_point"); //$NON-NLS-1$
		tableNames.put(Label.class, "smart.i18n_label"); //$NON-NLS-1$
	}
	
	private DerbyPagedIntellResults results;
	private String queryDataTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return IntelligenceRecordQuery.KEY.equals(querytype);
	}
	
	/**
	 * Runs the given patrol query and retrieves the results from the database.
	 * 
	 * @param query
	 * @param session
	 * @param monitor
	 * @return
	 * @throws SQLException
	 */
	@Override
	public IQueryResult executeQuery(
			Query lquery,
			HashMap<String, Object> parameters) throws SQLException{
	
		final IntelligenceRecordQuery query = (IntelligenceRecordQuery) lquery;
		final Session session = (Session) parameters.get(Session.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		
		session.doWork(new Work() {
			
			@Override
			public void execute(Connection c) throws SQLException {
				try{
					//create temp table for holding reuslts
					StringBuilder sql = new StringBuilder();
					sql.append("CREATE TABLE "); //$NON-NLS-1$
					sql.append(queryDataTable);
					sql.append("(");//$NON-NLS-1$
					sql.append(" ca_id varchar(8), ");//$NON-NLS-1$
					sql.append(" ca_name varchar(256), ");//$NON-NLS-1$
					sql.append(" intel_uuid char(16) for bit data, ");//$NON-NLS-1$
					sql.append(" intel_name varchar(1024), ");//$NON-NLS-1$
					sql.append(" intel_datereceived date, ");//$NON-NLS-1$
					sql.append(" intel_fromdate date, ");//$NON-NLS-1$
					sql.append(" intel_todate date, ");//$NON-NLS-1$
					sql.append(" intel_sourceuuid char(16) for bit data, ");//$NON-NLS-1$
					sql.append(" intel_source varchar(1024), ");//$NON-NLS-1$
					sql.append(" intel_patrolid varchar(32), ");//$NON-NLS-1$
					sql.append(" intel_informantid varchar(128), ");//$NON-NLS-1$
					sql.append(" intel_description varchar(32672), ");//$NON-NLS-1$
					sql.append(" intel_locations varchar(32672) ");//$NON-NLS-1$
					sql.append(")"); //$NON-NLS-1$
					
					QueryPlugIn.logSql(sql.toString());
					c.createStatement().executeUpdate(sql.toString());
					
					//add results to table
					sql = new StringBuilder();
					sql.append("INSERT INTO "); //$NON-NLS-1$
					sql.append(queryDataTable);
					sql.append(" SELECT "); //$NON-NLS-1$
					sql.append(tablePrefix(ConservationArea.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(ConservationArea.class) + ".name,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".uuid,"); //$NON-NLS-1$
					sql.append("'',");	//name //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".received_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".from_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".to_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".source_uuid,"); //$NON-NLS-1$
					sql.append("'',");	//source //$NON-NLS-1$
					sql.append(tablePrefix(Patrol.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(Informant.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".description,"); //$NON-NLS-1$
					sql.append("''");	//locations //$NON-NLS-1$
					sql.append(" FROM ");//$NON-NLS-1$
					sql.append(tableNamePrefix(Intelligence.class));
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(tableNamePrefix(ConservationArea.class));
					sql.append(" ON ");//$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".ca_uuid = " + tablePrefix(ConservationArea.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
					
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(tableNamePrefix(Informant.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".informant_uuid = " + tablePrefix(Informant.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(tableNamePrefix(IntelligenceSource.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".source_uuid = " + tablePrefix(IntelligenceSource.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
				
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(tableNamePrefix(Patrol.class));
					sql.append(" ON "); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".patrol_uuid = " + tablePrefix(Patrol.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$
	
					
					sql.append(" WHERE "); //$NON-NLS-1$
					
					List<Object> parameterValues = new ArrayList<Object>();
					
					// ca filter
					ArrayList<UUID> localFilters = new ArrayList<UUID>();
					ConservationAreaFilter caFilter = ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(), SmartDB.getConservationAreaConfiguration().getConservationAreas());
					if (caFilter.includeAll()){
						//include all current conservation areas
						if (SmartDB.getConservationAreaConfiguration() != null){
							for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
								localFilters.add(ca.getUuid());
							}
						}else{
							localFilters.add(SmartDB.getCurrentConservationArea().getUuid());
						}
					}else{
						//include only selected conservation areas
						localFilters.addAll(caFilter.getConservationAreaFilterIds());
					}
					if (localFilters.size() > 0){
						sql.append(tablePrefix(ConservationArea.class) + ".uuid IN ("); //$NON-NLS-1$
						for (UUID ca : localFilters){
							sql.append("?,"); //$NON-NLS-1$
							parameterValues.add(ca);
						}
						sql.deleteCharAt(sql.length()-1);
						sql.append(")"); //$NON-NLS-1$
					}
					
					//date filter
					Date[] d = query.getDateFilter().getDateFilterOption().getDates();
					if (d != null){
						sql.append(" AND "); //$NON-NLS-1$
						sql.append(tablePrefix(Intelligence.class) + ".received_date>= ? AND "); //$NON-NLS-1$ 
						sql.append(tablePrefix(Intelligence.class) + ".received_date <= ? "); //$NON-NLS-1$ 
						
						parameterValues.add(d[0].toString());
						parameterValues.add(d[1].toString());
					}
					
					//query filter
					if (query.getQueryFilter().length() > 0){
						sql.append("AND ( "); //$NON-NLS-1$
						filterToSql(query.getFilter().getFilter(), sql, parameterValues);
						sql.append(" )"); //$NON-NLS-1$
					}
					
					QueryPlugIn.logSql(sql.toString());
					try(PreparedStatement psq = c.prepareStatement(sql.toString())){
						for (int i = 0; i < parameterValues.size(); i ++){
							if (parameterValues.get(i) instanceof UUID){
								psq.setBytes(i+1, UuidUtils.uuidToByte(((UUID)parameterValues.get(i))));
							}else{
								psq.setObject(i+1, parameterValues.get(i));
							}
						}
						psq.executeUpdate();
					}
					
					/* set the intelligence source name */
					String s= "SELECT distinct intel_sourceuuid FROM " + queryDataTable; //$NON-NLS-1$
					QueryPlugIn.logSql(s);
					try( ResultSet rs = c.createStatement().executeQuery(s)){
						try(PreparedStatement ps = c.prepareStatement("UPDATE " + queryDataTable + " SET intel_source = ? where intel_sourceuuid = ?")){ //$NON-NLS-1$ //$NON-NLS-2$
							while(rs.next()){
								byte[] uuid = rs.getBytes(1);
								String name = getSourceName(UuidUtils.byteToUUID(uuid), session);
						
								ps.setString(1, name);
								ps.setBytes(2, uuid);
								ps.executeUpdate();
							}
						}
					}
					
					/* set the intelligence record name */
					s = "UPDATE " + queryDataTable + " SET intel_name = (SELECT a.value from smart.i18n_label a where " + queryDataTable+ ".intel_uuid = a.element_uuid and a.language_uuid = x'" + UuidUtils.uuidToString(SmartDB.getCurrentLanguage().getUuid()) + "')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					QueryPlugIn.logSql(s);
					c.createStatement().executeUpdate(s);
					s = "UPDATE " + queryDataTable + " SET intel_name = (SELECT a.value from smart.i18n_label a join smart.language b on a.language_uuid = b.uuid where " + queryDataTable+ ".intel_uuid = a.element_uuid and b.isdefault) WHERE intel_name is null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					QueryPlugIn.logSql(s);
					c.createStatement().executeUpdate(s);
					
					sql = new StringBuilder();
					sql.append("SELECT count(*) FROM " + queryDataTable); //$NON-NLS-1$
					
					QueryPlugIn.logSql(sql.toString());
					int rowCnt = 0;
					try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
						rs.next();
						rowCnt = rs.getInt(1);
					}
					results = new DerbyPagedIntellResults(queryDataTable, rowCnt, RecordQueryIntelligenceEngine.this, query);
					
					c.commit();
				}catch (Exception ex){
					throw new SQLException (ex);
				}
			}
		});
		return results;
	
	}
	
	public void dropTables(Connection c){
		dropTable(c, queryDataTable);
	}
	
	private void filterToSql(IFilter filter, StringBuilder sql, List<Object> parameterValues) throws SQLException{
		sql.append(" "); //$NON-NLS-1$
		if (filter instanceof IntelligenceFilter){
			IntelligenceFilter f = (IntelligenceFilter)filter;
			if (f.getFilterOption() == IntelligenceFilterOption.DESCRIPTION){
				sql.append("LOWER("); //$NON-NLS-1$
				sql.append(tablePrefix(Intelligence.class) + ".description) "); //$NON-NLS-1$
				if (f.getOperator().equals(Operator.STR_EQUALS)){
					//description is a long varchar and you cannot do = on longvarchar
					sql.append(DerbyFilterToSqlGenerator.asSql(Operator.STR_CONTAINS));
				}else{
					sql.append(DerbyFilterToSqlGenerator.asSql(f.getOperator()));
				}
				sql.append(" ? "); //$NON-NLS-1$
				
				String value = f.getValue().toLowerCase();
				if (f.getOperator().equals(Operator.STR_CONTAINS) || f.getOperator().equals(Operator.STR_NOTCONTAINS)){
					value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				parameterValues.add(value);
				
			}else if (f.getFilterOption() == IntelligenceFilterOption.INFORMANTID){
				sql.append("LOWER("); //$NON-NLS-1$
				sql.append(tablePrefix(Informant.class) + ".id) "); //$NON-NLS-1$
				sql.append(DerbyFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? "); //$NON-NLS-1$
				parameterValues.add(f.getValue().toLowerCase());
				
			}else if (f.getFilterOption() == IntelligenceFilterOption.PATROLID){
				sql.append("LOWER("); //$NON-NLS-1$
				sql.append(tablePrefix(Patrol.class) + ".id) "); //$NON-NLS-1$
				sql.append(DerbyFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? "); //$NON-NLS-1$
				
				String value = f.getValue().toLowerCase();
				if (f.getOperator().equals(Operator.STR_CONTAINS) || f.getOperator().equals(Operator.STR_NOTCONTAINS)){
					value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				parameterValues.add(value);
			}else if (f.getFilterOption() == IntelligenceFilterOption.NAME){
				//needs a join
				sql.append(" EXISTS ( SELECT * FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(Label.class));
				sql.append(" WHERE "); //$NON-NLS-1$
				sql.append(tablePrefix(Label.class));
				sql.append(".element_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(Intelligence.class));
				sql.append(".uuid and LOWER(value) "); //$NON-NLS-1$
				
				sql.append(DerbyFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? )"); //$NON-NLS-1$
				String value = f.getValue().toLowerCase();
				if (f.getOperator().equals(Operator.STR_CONTAINS) || f.getOperator().equals(Operator.STR_NOTCONTAINS)){
					value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				parameterValues.add(value);
			}else if (f.getFilterOption() == IntelligenceFilterOption.SOURCE){
				//needs a join?
				sql.append(tablePrefix(IntelligenceSource.class) + ".keyId "); //$NON-NLS-1$
				sql.append(DerbyFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? "); //$NON-NLS-1$
				
				parameterValues.add(f.getValue());
				
			}
		}else if (filter instanceof BracketFilter){
			sql.append("("); //$NON-NLS-1$
			filterToSql(((BracketFilter)filter).getFilter(), sql, parameterValues);
			sql.append(")"); //$NON-NLS-1$
				
		}else if (filter instanceof BooleanExpression){
			filterToSql(((BooleanExpression)filter).getFilter1(), sql, parameterValues);
			sql.append(DerbyFilterToSqlGenerator.asSql(((BooleanExpression)filter).getOperator()));
			filterToSql(((BooleanExpression)filter).getFilter2(), sql, parameterValues);
		}else if (filter instanceof NotExpression){
			sql.append(DerbyFilterToSqlGenerator.asSql(Operator.NOT));
			filterToSql(((NotExpression)filter).getFilter(), sql, parameterValues);
		}
		sql.append(" "); //$NON-NLS-1$
	}
	
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getSourceName(UUID uuid, Session session){
		if (uuid != null){
			IntelligenceSource x = (IntelligenceSource) session.load(IntelligenceSource.class, uuid);
			if (x != null) {
				return x.getName();
			}
		}
		return null;
	}

	public IntelligenceRecordResultItem asQueryResultItem(ResultSet rs) throws SQLException{
		IntelligenceRecordResultItem item = new IntelligenceRecordResultItem();
		item.setConservationAreaName(rs.getString("ca_name")); //$NON-NLS-1$
		item.setConservationAreaId(rs.getString("ca_id")); //$NON-NLS-1$
		item.setUuid(UuidUtils.byteToUUID(rs.getBytes("intel_uuid"))); //$NON-NLS-1$
		item.setName(rs.getString("intel_name")); //$NON-NLS-1$
		item.setReceivedDate(rs.getDate("intel_datereceived")); //$NON-NLS-1$
		item.setFromDate(rs.getDate("intel_fromdate")); //$NON-NLS-1$
		item.setToDate(rs.getDate("intel_todate")); //$NON-NLS-1$
		
		item.setSourceUuid(UuidUtils.byteToUUID(rs.getBytes("intel_sourceuuid"))); //$NON-NLS-1$
		item.setSource(rs.getString("intel_source")); //$NON-NLS-1$
		
		item.setPatrolId(rs.getString("intel_patrolid")); //$NON-NLS-1$
		item.setInformantId(rs.getString("intel_informantid")); //$NON-NLS-1$
		item.setDescription(rs.getString("intel_description")); //$NON-NLS-1$
		
		return item;
		
	}
	/**
	 * Converts conservation area filter ot list of
	 * conservation areas for query.
	 * 
	 * @param filter
	 * @return
	 * @throws SQLException
	 */
	public String asString(ConservationAreaFilter filter) throws SQLException{
		ArrayList<UUID> localFilters = new ArrayList<UUID>();
		if (filter.includeAll()){
			//include all current conservation areas
			if (SmartDB.getConservationAreaConfiguration() != null){
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					localFilters.add(ca.getUuid());
				}
			}else{
				localFilters.add(SmartDB.getCurrentConservationArea().getUuid());
			}
		}else{
			//include only selected conservation areas
			localFilters.addAll(filter.getConservationAreaFilterIds());
		}
		if (localFilters.size() == 0){
			return ""; //$NON-NLS-1$
		}
		
		StringBuilder sb = new StringBuilder();
		for (UUID b : localFilters){
			sb.append("x'" + UuidUtils.uuidToString(b) + "',"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.deleteCharAt(sb.length() - 1);
		
		return sb.toString();
	}
}
