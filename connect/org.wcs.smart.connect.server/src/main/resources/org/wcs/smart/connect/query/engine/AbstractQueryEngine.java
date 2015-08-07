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
package org.wcs.smart.connect.query.engine;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.NamedPreparedStatement;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine shared functionality.  Also intitalizes common
 * database tables.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryEngine implements IQueryEngine {

	protected Map<String, Object> currentParameters = new HashMap<String, Object>();
	public HashMap<IFilter, String> filterTables = new HashMap<IFilter, String>();
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(ConservationArea.class, "ca"); //$NON-NLS-1$
		tablePrefix.put(Waypoint.class, "wp"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservation.class, "wpo"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa"); //$NON-NLS-1$
		tablePrefix.put(Attribute.class, "a"); //$NON-NLS-1$
		tablePrefix.put(Category.class, "c"); //$NON-NLS-1$
		tablePrefix.put(AttributeTreeNode.class, "atn"); //$NON-NLS-1$
		tablePrefix.put(AttributeListItem.class, "ali"); //$NON-NLS-1$
		tablePrefix.put(Area.class, "ar"); //$NON-NLS-1$
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
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes"); //$NON-NLS-1$
		tableNames.put(Attribute.class, "smart.dm_attribute"); //$NON-NLS-1$
		tableNames.put(Category.class, "smart.dm_category"); //$NON-NLS-1$
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree"); //$NON-NLS-1$
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list"); //$NON-NLS-1$
		tableNames.put(Area.class, "smart.area_geometries"); //$NON-NLS-1$
	}
		
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	public void dropTable(Connection c, String tableName)  {
		try {
			String sql = "DROP TABLE " + tableName; //$NON-NLS-1$
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
	}

	/**
	 * Creates a temporary query table 
	 * 
	 * @return
	 */
	public String createTempTableName(){
		return "query_temp_" + System.nanoTime(); //$NON-NLS-1$
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
				return x.getFullLabel();
			}
		}
		return null;
	}
	
	protected String getName(UUID uuid, UUID cauuid, Session session){
		if (SmartDB.isMultipleAnalysis()){
			//need find label for the given conservation area
			return Label.getDescription(uuid, cauuid);	
		}else{
			return Label.getDescription(uuid);
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
			return "varchar(1024)"; //$NON-NLS-1$
		case DATE:
			return "varchar(10)"; //$NON-NLS-1$
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
	 * By default creates an index on the ob_uuid field.  This method can be overwritten to 
	 * create additional indexes.
	 * 
	 * @param c database connection
	 * @param tableName temporary table to create indexes on
	 * @throws SQLException
	 */
	public void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_ob_uuid_idx on " +  tableName + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
	public PreparedStatement parseQueryString(Connection connection, String query) throws SQLException{
		NamedPreparedStatement pp = new NamedPreparedStatement(connection, query);
		StringBuilder log = new StringBuilder();
		for (Entry<String, Object> entry : currentParameters.entrySet()){
			if (entry.getValue() instanceof UUID){
				pp.setObject(entry.getKey(), (UUID)entry.getValue());
				log.append("x'"+ UuidUtils.uuidToString((UUID)entry.getValue()) + ", "); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				pp.setObject(entry.getKey(), entry.getValue());
				log.append(entry.getValue().toString().toString() + ", "); //$NON-NLS-1$
			}
		}
		QueryPlugIn.logSql(log.toString());
		return pp.getStatement();
	}
	
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
	
	
	public int getCategoryDepth(Connection c, UUID caUuid) throws SQLException{
		PreparedStatement ps = c.prepareStatement("SELECT max(length(hkey) - length(replace(hkey, '.', ''))) FROM " + tablePrefix(Category.class) + " WHERE ca_uuid = ?");
		ps.setObject(1, caUuid);
		ResultSet rs = ps.executeQuery();
		if(rs.next()){
			return rs.getInt(1);
		}
		return 0;
	}
}
