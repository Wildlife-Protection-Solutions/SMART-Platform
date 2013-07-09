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
package org.wcs.smart.query.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

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
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointObservation;
import org.wcs.smart.patrol.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;

/**
 * Query engine for executing 
 * queries using derby.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class DerbyQueryEngine2 implements QueryEngine {
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(ConservationArea.class, "ca"); //$NON-NLS-1$
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(Waypoint.class, "wp"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservation.class, "wpo"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationAttribute.class, "wpoa"); //$NON-NLS-1$
		tablePrefix.put(Attribute.class, "a"); //$NON-NLS-1$
		tablePrefix.put(Category.class, "c"); //$NON-NLS-1$
		tablePrefix.put(AttributeTreeNode.class, "atn"); //$NON-NLS-1$
		tablePrefix.put(AttributeListItem.class, "ali"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Area.class, "ar"); //$NON-NLS-1$
	}

	
	/**
	 * Maps hibernate classes to database table names
	 */
	protected static HashMap<Class<?>, String> tableNames = new HashMap<Class<?>, String>();
	static {
		tableNames = new HashMap<Class<?>, String>();
		tableNames.put(ConservationArea.class, "smart.conservation_area"); //$NON-NLS-1$
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(Waypoint.class, "smart.waypoint"); //$NON-NLS-1$
		tableNames.put(WaypointObservation.class, "smart.wp_observation"); //$NON-NLS-1$
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes"); //$NON-NLS-1$
		tableNames.put(Attribute.class, "smart.dm_attribute"); //$NON-NLS-1$
		tableNames.put(Category.class, "smart.dm_category"); //$NON-NLS-1$
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree"); //$NON-NLS-1$
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Area.class, "smart.area_geometries"); //$NON-NLS-1$
	}
		
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	protected void dropTable(Connection c, String tableName)  {
		try {
			String sql = "DROP TABLE " + tableName; //$NON-NLS-1$
			QueryPlugIn.logSql(sql);
			c.createStatement().execute(sql);
		} catch (Exception ex) {
			// eatme
		}
	}

	protected String createTempTableName(){
		return "query_temp_" + System.nanoTime();
	}


	
	
	
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String[] getCategoryLabels(byte[] uuid, Session session){
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
	protected String getEmployeeName(byte[] uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.load(Employee.class, uuid);
			if (x != null) {
				return x.getFullLabel();
			}
		}
		return null;
	}
	
	protected String getName(byte[] uuid, byte[] cauuid, Session session){
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
	protected String getDataType(AttributeType type) {
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
		}
		return ""; //$NON-NLS-1$

	}
	
	public String namePrefix(Class<?> clazz){
		return tableNames.get(clazz) + " " + prefix(clazz);
	}
	
	public String prefix(Class<?> clazz){
		return tablePrefix.get(clazz);
	}
	
	protected abstract String getTemporaryTableSelectClause(boolean includeObservations);
	
	protected abstract String getTemporaryTableCreateClause(String tableName);
	
	protected String appendFromClause(HashSet<Class<?>> tables){
		return "";
	}
	
	
	/**
	 * By default creates an index on the ob_uuid field.  This method can be overwritten to 
	 * create additional indexes
	 * 
	 * @param c
	 * @param tableName
	 * @throws SQLException
	 */
	protected void buildTemporaryTableIndexes(Connection c, String tableName) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX " + tableName + "_ob_uuid_idx on " +  tableName + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
}
