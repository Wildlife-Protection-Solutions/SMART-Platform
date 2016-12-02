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
package org.wcs.smart.connect.query.engine.intelligence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilter;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.model.IntelligenceRecordQuery;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.BooleanExpression;
import org.wcs.smart.query.model.filter.BracketFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.NotExpression;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Runs intelligence record queries, returning pages result set.
 * 
 * @author Emily
 *
 */
public class PsqlRecordQueryIntelligenceEngine extends AbstractQueryEngine {
	
	private String queryDataTable;
	
	@Override
	public boolean canExecute(String querytype) {
		return IntelligenceRecordQuery.KEY.equals(querytype);
	}
	
	public String getQueryDataTable(){
		return this.queryDataTable;
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
		session = (Session) parameters.get(Session.class.getName());
		locale = (Locale)parameters.get(Locale.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		queryDataTable = createTempTableName();
		
		return session.doReturningWork(new ReturningWork<RecordIntelligenceQueryResult>() {
			@Override
			public RecordIntelligenceQueryResult execute(Connection c) throws SQLException {
				try{
					//create temp table for holding reuslts
					StringBuilder sql = new StringBuilder();
					sql.append("CREATE TABLE "); //$NON-NLS-1$
					sql.append(queryDataTable);
					sql.append("(");//$NON-NLS-1$
					sql.append(" ca_id varchar(8), ");//$NON-NLS-1$
					sql.append(" ca_name varchar(256), ");//$NON-NLS-1$
					sql.append(" intel_uuid UUID, ");//$NON-NLS-1$
					sql.append(" intel_name varchar(1024), ");//$NON-NLS-1$
					sql.append(" intel_datereceived date, ");//$NON-NLS-1$
					sql.append(" intel_fromdate date, ");//$NON-NLS-1$
					sql.append(" intel_todate date, ");//$NON-NLS-1$
					sql.append(" intel_sourceuuid UUID, ");//$NON-NLS-1$
					sql.append(" intel_source varchar(1024), ");//$NON-NLS-1$
					sql.append(" intel_patrolid varchar(32), ");//$NON-NLS-1$
					sql.append(" intel_informantid varchar(128), ");//$NON-NLS-1$
					sql.append(" intel_description varchar(32672), ");//$NON-NLS-1$
					sql.append(" intel_locations geometry ");//$NON-NLS-1$
					sql.append(")"); //$NON-NLS-1$
					
					logger.finest(sql.toString());
					c.createStatement().executeUpdate(sql.toString());
					
					//add results to table
					sql = new StringBuilder();
					sql.append("INSERT INTO "); //$NON-NLS-1$
					sql.append(queryDataTable);
					sql.append(" SELECT "); //$NON-NLS-1$
					sql.append(tablePrefix(ConservationArea.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(ConservationArea.class) + ".name,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".uuid,"); //$NON-NLS-1$
					sql.append("null,");	//name //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".received_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".from_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".to_date,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".source_uuid,"); //$NON-NLS-1$
					sql.append("null,");	//source //$NON-NLS-1$
					sql.append(tablePrefix(Patrol.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(Informant.class) + ".id,"); //$NON-NLS-1$
					sql.append(tablePrefix(Intelligence.class) + ".description,"); //$NON-NLS-1$
					sql.append("foo.geoms");	//locations //$NON-NLS-1$
					sql.append(" FROM ");//$NON-NLS-1$
					sql.append(tableNamePrefix(Intelligence.class));
					sql.append(" LEFT JOIN "); //$NON-NLS-1$
					sql.append(" (SELECT st_collect(st_makepoint(x, y)) as geoms, intelligence_uuid FROM " + tableName(IntelligencePoint.class) + " group by intelligence_uuid) foo ON foo.intelligence_uuid = " + tablePrefix(Intelligence.class) + ".uuid  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
					parseConservationAreaFilterInternal(query);
					
					sql.append(tablePrefix(ConservationArea.class) + ".uuid IN ("); //$NON-NLS-1$
					for (UUID ca : caFilter.getConservationAreaFilterIds()){
						sql.append("?,"); //$NON-NLS-1$
						parameterValues.add(ca);
					}
					sql.deleteCharAt(sql.length()-1);
					sql.append(")"); //$NON-NLS-1$
					
					//date filter
					Date[] d = query.getDateFilter().getDateFilterOption().getDates();
					if (d != null){
						sql.append(" AND "); //$NON-NLS-1$
						sql.append(tablePrefix(Intelligence.class) + ".received_date>= ? AND "); //$NON-NLS-1$ 
						sql.append(tablePrefix(Intelligence.class) + ".received_date <= ? "); //$NON-NLS-1$ 
						
						parameterValues.add(d[0]);
						parameterValues.add(d[1]);
					}
					
					//query filter
					if (query.getQueryFilter().length() > 0){
						sql.append("AND ( "); //$NON-NLS-1$
						filterToSql(query.getFilter().getFilter(), sql, parameterValues);
						sql.append(" )"); //$NON-NLS-1$
					}
					
					logger.finest(sql.toString());
					try(PreparedStatement psq = c.prepareStatement(sql.toString())){
						for (int i = 0; i < parameterValues.size(); i ++){
							if (parameterValues.get(i) instanceof UUID){
								psq.setObject(i+1, (UUID)parameterValues.get(i));
							}else{
								psq.setObject(i+1, parameterValues.get(i));
							}
						}
						psq.executeUpdate();
					}
					
					/* set the intelligence source name */
					String s= "SELECT distinct intel_sourceuuid FROM " + queryDataTable; //$NON-NLS-1$
					logger.finest(s);
					try( ResultSet rs = c.createStatement().executeQuery(s)){
						try(PreparedStatement ps = c.prepareStatement("UPDATE " + queryDataTable + " SET intel_source = ? where intel_sourceuuid = ?")){ //$NON-NLS-1$ //$NON-NLS-2$
							while(rs.next()){
								UUID uuid = (UUID) rs.getObject(1);
								String name = getSourceName(uuid, session);
						
								ps.setString(1, name);
								ps.setObject(2, uuid);
								ps.executeUpdate();
							}
						}
					}
					
					//match language and country
					String key1 = locale.toString().toUpperCase();
					String query = "UPDATE " + queryDataTable + " SET intel_name = a.value from smart.i18n_label a join smart.language b on a.language_uuid = b.uuid WHERE " + queryDataTable + ".intel_uuid = a.element_uuid AND upper(b.code) = '" + key1 + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					logger.finest(query);
					c.createStatement().executeUpdate(query);
					
					//match language
					key1 = locale.getLanguage().toUpperCase();
					query = "UPDATE " + queryDataTable + " SET intel_name = a.value from smart.i18n_label a join smart.language b on a.language_uuid = b.uuid WHERE " + queryDataTable + ".intel_uuid = a.element_uuid AND upper(b.code) = '" + key1 + "' AND intel_name is null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					logger.finest(query);
					c.createStatement().executeUpdate(query);

					//update name to default language 
					s = "UPDATE " + queryDataTable + " SET intel_name = (SELECT a.value from smart.i18n_label a join smart.language b on a.language_uuid = b.uuid where " + queryDataTable+ ".intel_uuid = a.element_uuid and b.isdefault) WHERE intel_name is null"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					logger.finest(s);
					c.createStatement().executeUpdate(s);
				
					c.commit();
					
					//item cnt
					int itemcnt = 0;
					try(ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM " + getQueryDataTable())){ //$NON-NLS-1$
						rs.next();
						itemcnt = rs.getInt(1);
					}
					return new RecordIntelligenceQueryResult(PsqlRecordQueryIntelligenceEngine.this, itemcnt);
				}catch (Exception ex){
					throw new SQLException (ex);
				}
			}
		});
		
	
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
					sql.append(PsqlFilterToSqlGenerator.asSql(Operator.STR_CONTAINS));
				}else{
					sql.append(PsqlFilterToSqlGenerator.asSql(f.getOperator()));
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
				sql.append(PsqlFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? "); //$NON-NLS-1$
				parameterValues.add(f.getValue().toLowerCase());
				
			}else if (f.getFilterOption() == IntelligenceFilterOption.PATROLID){
				sql.append("LOWER("); //$NON-NLS-1$
				sql.append(tablePrefix(Patrol.class) + ".id) "); //$NON-NLS-1$
				sql.append(PsqlFilterToSqlGenerator.asSql(f.getOperator()));
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
				
				sql.append(PsqlFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? )"); //$NON-NLS-1$
				String value = f.getValue().toLowerCase();
				if (f.getOperator().equals(Operator.STR_CONTAINS) || f.getOperator().equals(Operator.STR_NOTCONTAINS)){
					value = "%" + value + "%"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				parameterValues.add(value);
			}else if (f.getFilterOption() == IntelligenceFilterOption.SOURCE){
				//needs a join?
				sql.append(tablePrefix(IntelligenceSource.class) + ".keyId "); //$NON-NLS-1$
				sql.append(PsqlFilterToSqlGenerator.asSql(f.getOperator()));
				sql.append(" ? "); //$NON-NLS-1$
				
				parameterValues.add(f.getValue());
				
			}
		}else if (filter instanceof BracketFilter){
			sql.append("("); //$NON-NLS-1$
			filterToSql(((BracketFilter)filter).getFilter(), sql, parameterValues);
			sql.append(")"); //$NON-NLS-1$
				
		}else if (filter instanceof BooleanExpression){
			filterToSql(((BooleanExpression)filter).getFilter1(), sql, parameterValues);
			sql.append(PsqlFilterToSqlGenerator.asSql(((BooleanExpression)filter).getOperator()));
			filterToSql(((BooleanExpression)filter).getFilter2(), sql, parameterValues);
		}else if (filter instanceof NotExpression){
			sql.append(PsqlFilterToSqlGenerator.asSql(Operator.NOT));
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


	@Override
	public void cleanUp(Session session) throws SQLException {
		dropTable(session, queryDataTable);	
	}

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		return null;
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		return null;
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}
}

