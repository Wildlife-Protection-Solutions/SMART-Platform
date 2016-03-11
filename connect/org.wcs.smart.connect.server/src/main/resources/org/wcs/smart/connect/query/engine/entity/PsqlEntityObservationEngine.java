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
package org.wcs.smart.connect.query.engine.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.entity.query.parser.internal.EntityAttributeFilter;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.date.CachingDateFilter;

/**
 * Query engine for executing lazy queries using derby.
 * This engines create temporary tables that one to one correspond with the table
 * that user see. {@link DerbyPagedObservationResult} obtains the name of this table and is
 * responsible for all other operations (fetching/sorting/deleting tables)
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PsqlEntityObservationEngine extends AbstractQueryEngine {

	private final Logger logger = Logger.getLogger(PsqlEntityObservationEngine.class.getName());
	
	private String queryDataTable;
	private SimpleQuery query;
	private Collection<String> entityTypes;
	
	public PsqlEntityObservationEngine(){
	}
	
	public String getQueryDataTable(){
		return this.queryDataTable;
	}

	public Collection<String> getEntityTypes(){
		return this.entityTypes;
	}
	
	@Override
	public boolean canExecute(String querytype) {
		return EntityObservationQuery.KEY.equals(querytype);
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

		query = (SimpleQuery) lquery;
		locale = (Locale) parameters.get(Locale.class.getName());
		session = (Session) parameters.get(Session.class.getName());
	
		if (query.getDateFilter() == null){
			return null;
		}
		
		queryDataTable = createTempTableName();

		return session.doReturningWork(new ReturningWork<EntityObservationQueryResult>() {
			@Override
			public EntityObservationQueryResult execute(Connection c) throws SQLException {
				IFilterProcessor filterer = null;
				
				//create a date filter that caches the dates so the same
				//dates are used for all parts of the query;
				//otherwise different date filters will be computed
				//for different parts of the queries
				DateFilter dFilter = new DateFilter(query.getDateFilter().getDateFieldOption(), new CachingDateFilter(query.getDateFilter().getDateFilterOption()));				
				try {		
					entityTypes = new ArrayList<String>();
			        query.getFilter().getFilter().accept(new IFilterVisitor() {
			            @Override
			            public void visit(IFilter filter) {
			                if (filter instanceof EntityAttributeFilter){
			                    entityTypes.add(((EntityAttributeFilter) filter).getEntityKey());
			                }
			            }
			        });
			        
					filterer = PsqlEntityObservationEngine.this.getFilterProcessor(query.getFilter().getFilterType(), queryDataTable);
					ConservationAreaFilter caFilter = AbstractQueryEngine.parseConservationAreaFilter(query);
					filterer.processFilter(c, query.getFilter().getFilter(), dFilter, 
							caFilter, 
							true, true);
					
					populateTemporaryTableExtra(c, caFilter, session);

					//item cnt
					int itemcnt;
					try(ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM " + getQueryDataTable())){
						rs.next();
						itemcnt = rs.getInt(1);
					}
					c.commit();
					
					return new EntityObservationQueryResult(PsqlEntityObservationEngine.this, itemcnt);
				}catch (Exception ex){
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					throw new SQLException(ex);
				} finally {
					if (filterer != null) filterer.dropTemporaryTables(c);
					dropTemporaryTables(c, false);
				}
				
			}
		});
	}

	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	private void dropTemporaryTables(Connection c, boolean fullDrop) throws SQLException {
		if (!fullDrop)
			return;
		//original table
		dropTable(c, queryDataTable);
		dropTable(c, queryDataTable + "_LIST"); //$NON-NLS-1$
		dropTable(c, queryDataTable + "_TREE"); //$NON-NLS-1$
	}

	private void populateTemporaryTableExtra(Connection c, ConservationAreaFilter caFilter, Session session) throws Exception {
		//NOTE: does 50 worked for monitor in total
		String[][] columnsToAdd = new String[][]{
				{"ca_id","varchar(8)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ca_name","varchar(256)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{"ob_observer", "varchar(512)"} //$NON-NLS-1$ //$NON-NLS-2$
		};
		
		for (int i = 0; i < columnsToAdd.length; i ++){
			String sql = "ALTER TABLE " + queryDataTable + " ADD "+ columnsToAdd[i][0] + " " + columnsToAdd[i][1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}
		//ca information
		populateCaDetails(c, queryDataTable, "p_ca_uuid", query); //$NON-NLS-1$
		
		// add observers
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT ob_observer_uuid FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		logger.finest(sql.toString());

		
		String updateSql = "UPDATE " + queryDataTable + " SET "; //$NON-NLS-1$ //$NON-NLS-2$
		String q1 = updateSql + "ob_observer = ? where ob_observer_uuid = ?"; //$NON-NLS-1$
		logger.finest(q1);
		PreparedStatement observerSt = c.prepareStatement(q1);
		int cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())) {
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null) continue;
				String name = getEmployeeName(uuid, session);

				if (name != null) {

					observerSt.setString(1, name);
					observerSt.setObject(2, uuid);
					observerSt.addBatch();
					cnt++;
					if (cnt >= 100) {
						observerSt.executeBatch();
						cnt = 0;
					}
				}
			}
			observerSt.executeBatch();
		}

		populateTemporaryTableCategory(c, session, caFilter, queryDataTable);
		populateAdditionalWpoaTable(c, queryDataTable + "_list", "list_element_uuid", caFilter); //$NON-NLS-1$ //$NON-NLS-2$
		populateAdditionalWpoaTable(c, queryDataTable + "_tree", "tree_node_uuid", caFilter); //$NON-NLS-1$ //$NON-NLS-2$
		
		
	}

	private void populateAdditionalWpoaTable(Connection c, String tableName, String obsAttUuidColumn, ConservationAreaFilter caFilter) throws Exception {
		String sql = "CREATE TABLE " + tableName + " (uuid uuid, value varchar(1024))"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);
	
		sql = "INSERT INTO " + tableName + " (uuid) SELECT DISTINCT wpoa." + obsAttUuidColumn //$NON-NLS-1$ //$NON-NLS-2$
				+" FROM "  //$NON-NLS-1$
				+ tableNamePrefix(WaypointObservationAttribute.class) + " inner join " //$NON-NLS-1$
				+ queryDataTable + " r on " //$NON-NLS-1$
				+ tablePrefix(WaypointObservationAttribute.class) + ".OBSERVATION_UUID = r.OB_UUID"; //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql);
		
		//add entity attributes
		if (entityTypes.size() > 0){
    		StringBuilder s = new StringBuilder();
	        clearParameters();
	        s.append("INSERT INTO " + tableName + "(uuid) SELECT DISTINCT "); //$NON-NLS-1$ //$NON-NLS-2$
	        s.append(tablePrefix(EntityAttributeValue.class) + "." + obsAttUuidColumn); //$NON-NLS-1$
	        s.append(" FROM "); //$NON-NLS-1$
	        s.append(tableNamePrefix(EntityAttributeValue.class ));
	        s.append(" join "); //$NON-NLS-1$
	        s.append(tableNamePrefix(Entity.class ));
	        s.append(" on "); //$NON-NLS-1$
	        s.append(tablePrefix(EntityAttributeValue.class ) + ".entity_uuid = "); //$NON-NLS-1$
	        s.append(tablePrefix(Entity.class ) + ".uuid "); //$NON-NLS-1$
	        s.append(" join "); //$NON-NLS-1$
	        s.append(tableNamePrefix(EntityType.class ));
	        s.append(" on "); //$NON-NLS-1$
	        s.append(tablePrefix(EntityType.class ) + ".uuid = "); //$NON-NLS-1$
	        s.append(tablePrefix(Entity.class ) + ".entity_type_uuid "); //$NON-NLS-1$
	        s.append(" WHERE "); //$NON-NLS-1$
	        s.append(tablePrefix(EntityAttributeValue.class) + "." + obsAttUuidColumn + " is not null and "); //$NON-NLS-1$ //$NON-NLS-2$
	        s.append("keyid IN ("); //$NON-NLS-1$
	        for (String et : entityTypes){
	            String p1 = addParameterValue(et);
	            s.append(p1 + ","); //$NON-NLS-1$
	        }
	        s.deleteCharAt(s.length()-1);
	        s.append(")"); //$NON-NLS-1$
	        s.append(" AND ca_uuid IN ("); //$NON-NLS-1$
            for (UUID cauuid : caFilter.getConservationAreaFilterIds()){
                String p1 = addParameterValue(cauuid);
                s.append(p1 + ",");     //$NON-NLS-1$
            }
            s.deleteCharAt(s.length()-1);
            s.append(")"); //$NON-NLS-1$
        
            logger.finest(s.toString());
            parseQueryString(c, s.toString()).executeUpdate();
		}
		updateLabel(c, tableName, "uuid", "value"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	

	@Override
	public String getTemporaryTableSelectClause(boolean includeObservations) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".ca_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".source, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".id, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".x, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".y, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".direction, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".distance, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".datetime, "); //$NON-NLS-1$
		sql.append(tablePrefix(Waypoint.class) + ".wp_comment, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".employee_uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".uuid, "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class) + ".category_uuid "); //$NON-NLS-1$

		return sql.toString();
	}

	@Override
	public String getTemporaryTableCreateClause(String tableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + "("); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append("p_ca_uuid UUID,"); //$NON-NLS-1$
		sql.append("wp_uuid UUID,"); //$NON-NLS-1$ 
		sql.append("wp_source varchar(16),"); //$NON-NLS-1$
		sql.append("wp_id integer,"); //$NON-NLS-1$
		sql.append("wp_x double precision,"); //$NON-NLS-1$
		sql.append("wp_y double precision,"); //$NON-NLS-1$
		sql.append("wp_direction real,"); //$NON-NLS-1$
		sql.append("wp_distance real,"); //$NON-NLS-1$
		sql.append("wp_time timestamp,"); //$NON-NLS-1$
		sql.append("wp_comment varchar(4096),"); //$NON-NLS-1$
		sql.append("ob_observer_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_uuid UUID,"); //$NON-NLS-1$
		sql.append("ob_category_uuid UUID"); //$NON-NLS-1$
		sql.append(")"); //$NON-NLS-1$
		return sql.toString();
	}
	
	@Override
	public void buildTemporaryTableIndexes(Connection c, String tableName)
			throws SQLException {
		super.buildTemporaryTableIndexes(c, tableName);
		
		StringBuilder sql = new StringBuilder();
		sql.append("create index "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("_ob_category_uuid_idx on "); //$NON-NLS-1$
		sql.append(tableName);
		sql.append("(ob_category_uuid)"); //$NON-NLS-1$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
	}

	@Override
	public void cleanUp(Session session) {
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				dropTemporaryTables(c, true);		
			}});
	}

	@Override
	public String getSurveySamplingUnitJoinFieldName() {
		return null;
	}

	protected IFilterProcessor getFilterProcessor(FilterType filterType,
			String queryDataTable) {
		if (filterType == IFilter.FilterType.OBSERVATION){
			return new PsqlEntityFilterProcessor(queryDataTable, this);
		}else{
			return new PsqlEntityWaypointFilterProcessor(queryDataTable, this);
		}
	}
}
