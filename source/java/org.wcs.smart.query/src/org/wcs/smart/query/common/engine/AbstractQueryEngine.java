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
package org.wcs.smart.query.common.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.FilterType;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine shared functionality.  Also intitalizes common
 * database tables.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryEngine implements IQueryEngine {

	public HashMap<IFilter, FilterTable> filterTables = new HashMap<IFilter, FilterTable>();

	protected Map<String, Object> currentParameters = new HashMap<String, Object>();

	private static AtomicLong tableCnter = new AtomicLong();
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(ConservationArea.class, "ca"); //$NON-NLS-1$
		tablePrefix.put(Waypoint.class, "wp"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservation.class, "wpo"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationGroup.class, "wpg"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationAttributeList.class, "wpoal"); //$NON-NLS-1$
		tablePrefix.put(Attribute.class, "a"); //$NON-NLS-1$
		tablePrefix.put(Category.class, "c"); //$NON-NLS-1$
		tablePrefix.put(AttributeTreeNode.class, "atn"); //$NON-NLS-1$
		tablePrefix.put(AttributeListItem.class, "ali"); //$NON-NLS-1$
		tablePrefix.put(Area.class, "ar"); //$NON-NLS-1$
		tablePrefix.put(Employee.class, "e"); //$NON-NLS-1$
		tablePrefix.put(Agency.class, "aa"); //$NON-NLS-1$
		tablePrefix.put(Rank.class, "ear"); //$NON-NLS-1$
		tablePrefix.put(WaypointAttachment.class, "wpa"); //$NON-NLS-1$
		tablePrefix.put(ObservationAttachment.class, "wooa"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	protected static HashMap<Class<?>, String> tableNames = new HashMap<Class<?>, String>();
	static {
		tableNames = new HashMap<Class<?>, String>();
		tableNames.put(ConservationArea.class, "smart.conservation_area"); //$NON-NLS-1$
		tableNames.put(Waypoint.class, "smart.waypoint"); //$NON-NLS-1$
		tableNames.put(WaypointObservation.class, "smart.wp_observation"); //$NON-NLS-1$
		tableNames.put(WaypointObservationGroup.class, "smart.wp_observation_group"); //$NON-NLS-1$
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes"); //$NON-NLS-1$
		tableNames.put(WaypointObservationAttributeList.class, "smart.wp_observation_attributes_list"); //$NON-NLS-1$
		tableNames.put(Attribute.class, "smart.dm_attribute"); //$NON-NLS-1$
		tableNames.put(Category.class, "smart.dm_category"); //$NON-NLS-1$
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree"); //$NON-NLS-1$
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list"); //$NON-NLS-1$
		tableNames.put(Area.class, "smart.area_geometries"); //$NON-NLS-1$
		tableNames.put(Employee.class, "smart.employee"); //$NON-NLS-1$
		tableNames.put(Agency.class, "smart.agency"); //$NON-NLS-1$
		tableNames.put(Rank.class, "smart.rank"); //$NON-NLS-1$
		tableNames.put(WaypointAttachment.class, "smart.wp_attachments"); //$NON-NLS-1$
		tableNames.put(ObservationAttachment.class, "smart.observation_attachment"); //$NON-NLS-1$
	}
		
	
	@Override
	public abstract IQueryResult executeQuery(Query query, HashMap<String, Object> parameters) throws SQLException;

	@Override
	public abstract boolean canExecute(String querytype);
	
	
	/**
	 * Create the select statement to populate the temporary table
	 * containing observation data for the query engine.
	 * 
	 * @param includeObservations if observation information should be included
	 * in the output table (ob_uuid).
	 * 
	 * @return
	 */
	public abstract String getTemporaryTableSelectClause(boolean includeObservations);
	
	
	/**
	 * Create the temporary table for hold observation data
	 * for querying
	 * 
	 * @param tableName temporary table name
	 * @return 
	 */
	public abstract String getTemporaryTableCreateClause(String tableName);
	
	/**
	 * Create index on the temporary data table
	 * @param tableName
	 * @return
	 */
	public abstract void createTemporaryTableIndexes(Connection c, String tableName) throws SQLException;

	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	public abstract IFilterProcessor getFilterProcessor(FilterType filterType, String queryDataTable, Query query);
	
	
	
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	public void dropTable(Connection c, String tableName)  {
		try {
			String sql = "DROP TABLE " + tableName; //$NON-NLS-1$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
	}

	/**
	 * Drop all tables associated with the query results.
	 * @param c
	 * @throws SQLException
	 */
	public abstract void dropTables(Connection c) throws SQLException ;
	
	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	public synchronized String createTempTableName(){
		return "query_temp_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}

	
	
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String[] getCategoryLabels(UUID uuid, Session session){
		if (uuid != null){
			return QueryDataModelManager.getInstance().getFullCategoryLabel(session, uuid);
		}
		return null;
	}
	
	/**
	 * Loads the team object from the session
	 * and returns the associated name.
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String getEmployeeName(UUID uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.load(Employee.class, uuid);
			if (x != null) {
				return SmartLabelProvider.getShortLabel(x);
			}
		}
		return null;
	}
	
	protected String getName(UUID uuid, UUID cauuid, Session session){
		if (SmartDB.isMultipleAnalysis()){
			//need find label for the given conservation area
			return SmartLabelProvider.getDescription(uuid, cauuid, session);	
		}else{
			return Label.getDescription(uuid, session);
		}
	}
	
	/**
	 * Returns the database data type for a given 
	 * attribute type.
	 * @param type the attribute type
	 * @return the database datatype for the observation
	 * temporary table
	 */
	public String getDataType(AttributeType type) {
		switch (type) {
		case LIST:
			return "varchar(128)"; //keyid //$NON-NLS-1$
		case TREE:
			return "varchar(32672)"; ///hkey //$NON-NLS-1$
		case NUMERIC:
			return "double"; //$NON-NLS-1$
		case BOOLEAN:
			return "double"; //$NON-NLS-1$
		case TEXT:
			return "varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		case DATE:
			return "varchar(10)"; //$NON-NLS-1$
		case MLIST: throw new UnsupportedOperationException("Multi-List attribute type not support for column type"); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$

	}
	/**
	 * Patrol.class = "smart.patrol p"
	 * @param clazz
	 * @return the table name with associated short form
	 */
	public String tableNamePrefix(Class<?> clazz){
		return tableName(clazz) + " " + tablePrefix(clazz); //$NON-NLS-1$
	}
	
	/**
	 * 
	 * @param clazz
	 * @return the table query short form
	 */
	public String tablePrefix(Class<?> clazz){
		return tablePrefix.get(clazz);
	}
	
	/**
	 * 
	 * @param clazz
	 * @return the table query short form
	 */
	public String tableName(Class<?> clazz){
		return tableNames.get(clazz);
	}
	
	
	/**
	 * A string to append to the from clause of the select
	 * statement to create the temporary table.
	 * <p>Depending on the select clause additional tables may
	 * be required.  See {@link DerbyQueryEngine2#getTemporaryTableCreateClause(String)}. </p> 
	 * @param tables List of tables already included in the from clause
	 * @return
	 */
	public String appendFromClause(HashSet<Class<?>> tables){
		return ""; //$NON-NLS-1$
	}
	
	
	/**
	 * Creates an index on the ob_uuid in the given table.
	 * 
	 * @param c database connection
	 * @param tableName temporary table to create indexes on
	 * @throws SQLException
	 */
	public void createObsIndex(Connection c, String tableName) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_ob_uuid_idx on " +  tableName + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	/**
	 * Creates an index on the wp_uuid in the given table
	 * @param c
	 * @param tableName
	 * @throws SQLException
	 */
	public void createWpIndex(Connection c, String tableName) throws SQLException {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_wp_uuid_idx on " +  tableName + "(wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}

	
	/**
	 * @see org.wcs.smart.query.common.engine.IQueryEngine#addParameterValue(java.lang.Object)
	 */
	public String addParameterValue(Object parameter){
		String name = ":param_" + currentParameters.size(); //$NON-NLS-1$
		currentParameters.put(name, parameter);
		return name;
	}
	
	public void clearParameters(){
		currentParameters.clear();
	}
	
	/**
	 * Users must close the prepared statement when they are done with it.
	 * @param connection
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public PreparedStatement parseQueryString(Connection connection, String query) throws SQLException{
		NamedPreparedStatement pp = new NamedPreparedStatement(connection, query);
		StringBuilder log = new StringBuilder();
		for (Entry<String, Object> entry : currentParameters.entrySet()){
			if (entry.getValue() instanceof UUID){
				pp.setObject(entry.getKey(), (UUID)entry.getValue());
				log.append("x'"+ UuidUtils.uuidToString((UUID)entry.getValue()) + ", "); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				pp.setObject(entry.getKey(), entry.getValue());
				log.append(entry.getValue().toString() + ", "); //$NON-NLS-1$
			}
		}
		QueryPlugIn.logSql(log.toString());
		return pp.getStatement();
	}

	
	/**
	 * Determine if the given column for the given table exists and if
	 * it exists returns true if there are values in the color otherwise false.
	 *  
	 * @param c
	 * @param tableName
	 * @param columnName
	 * @return
	 */
	protected boolean checkColumnHasValues(Connection c, String tableName, String columnName) {
		
		//see if column exists
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT count(*) "); //$NON-NLS-1$
		sb.append("FROM sys.SYSSCHEMAS s, sys.systables t, sys.syscolumns c "); //$NON-NLS-1$
		sb.append(" WHERE s.schemaid = t.schemaid and c.referenceid = t.tableid "); //$NON-NLS-1$
		sb.append(" and t.tablename = UPPER('" + tableName + "') and c.columnname = UPPER('" + columnName + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		try(ResultSet rs = c.createStatement().executeQuery(sb.toString())){
			if (rs.next()) {
				if (rs.getInt(1) <= 0) return true; //no column in table
			}else {
				return true;
			}
		} catch (SQLException e) {
			SmartPlugIn.log(MessageFormat.format("Unexpected error while checking column with name ''{1}'' for values in table ''{0}''.", tableName, columnName), e); //$NON-NLS-1$
		}
		try(ResultSet rs = c.createStatement().executeQuery("select count (*) from "+tableName+" where "+columnName+" is not null")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (rs.next()) {
				int count = rs.getInt(1);
				return count > 0;
			}
		} catch (SQLException e) {
			if (e.getSQLState().equalsIgnoreCase("42X04") && e.getErrorCode() == 20000) { //$NON-NLS-1$
				//usually this is ok behavior which happens when specific column is not present in query output; add logging just in case
				SmartPlugIn.logInfo(MessageFormat.format("No column with name ''{1}'' found in table ''{0}''.", tableName, columnName)); //$NON-NLS-1$
			} else {
				//this is something unexpected
				SmartPlugIn.log(MessageFormat.format("Unexpected error while checking column with name ''{1}'' for values in table ''{0}''.", tableName, columnName), e); //$NON-NLS-1$
			}
		}
		return true; //it is safer to assume that column that we were unable to find may have values and display it to user
	}
	
	public void checkForOutOfMemory(Exception ex) throws SQLException{
		Throwable temp = ex;
		while(temp != null) {
			if (temp instanceof SQLException) {
				if ("XJ001".equals(((SQLException)temp).getSQLState())) { //$NON-NLS-1$
					throw new SQLException(Messages.AbstractQueryEngine_NotEnoughMemory);
				}
			}
			temp = temp.getCause();
		}
	}
	
	

	/**
	 * Simple class for tracking temporary filter tables 
	 * and columns
	 * 
	 * @author Emily
	 *
	 */
	//NOTE: the secondary key is for waypoint observation group filters
	//these filters require both wp_id and wp_group_id in order to include
	//waypoints with no observations when the query filter is filtering on 
	//a waypoint (or higher ex. patrol id; waypoint type) level attribute 
	//and not a data model filter
	public static class FilterTable{
		public String tablename;
		public String primarykey;
		public String secondarykey;
		
		public FilterTable(String tablename, String primarykey) {
			this.tablename = tablename;
			this.primarykey = primarykey;
		}
		
		public FilterTable(String tablename, String primarykey, String secondarykey) {
			this.tablename = tablename;
			this.primarykey = primarykey;
			this.secondarykey = secondarykey;
		}
	}
	
}
