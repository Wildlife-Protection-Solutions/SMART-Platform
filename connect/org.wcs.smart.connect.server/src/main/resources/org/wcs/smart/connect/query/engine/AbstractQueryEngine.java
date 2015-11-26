/*
 * Copyright (C) 2015 Wildlife Conservation Society
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

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
import org.wcs.smart.connect.query.engine.patrol.PatrolFilterProcessor;
import org.wcs.smart.connect.query.engine.patrol.PatrolWaypointFilterProcessor;
import org.wcs.smart.entity.model.Entity;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityAttributeValue;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligencePoint;
import org.wcs.smart.intelligence.model.IntelligenceSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine shared functionality.
 * 
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryEngine implements IQueryEngine {
	private final Logger logger = Logger.getLogger(AbstractQueryEngine.class.getName());
	
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
		
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(PatrolWaypoint.class, "pwp"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Team.class, "smart.team"); //$NON-NLS-1$
		tablePrefix.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tablePrefix.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		
		tablePrefix.put(Entity.class, "e"); //$NON-NLS-1$
		tablePrefix.put(EntityType.class, "et"); //$NON-NLS-1$
		tablePrefix.put(EntityAttribute.class, "ea"); //$NON-NLS-1$
		tablePrefix.put(EntityAttributeValue.class, "eav"); //$NON-NLS-1$
		
		tablePrefix.put(SurveyDesign.class, "sd"); //$NON-NLS-1$
		tablePrefix.put(Survey.class, "s"); //$NON-NLS-1$
		tablePrefix.put(Mission.class, "m"); //$NON-NLS-1$
		tablePrefix.put(MissionDay.class, "md"); //$NON-NLS-1$
		tablePrefix.put(MissionAttribute.class, "ma"); //$NON-NLS-1$
		tablePrefix.put(MissionAttributeListItem.class, "mali"); //$NON-NLS-1$
		tablePrefix.put(MissionProperty.class, "mp"); //$NON-NLS-1$
		tablePrefix.put(MissionTrack.class, "t"); //$NON-NLS-1$
		tablePrefix.put(MissionMember.class, "mm"); //$NON-NLS-1$
		tablePrefix.put(MissionPropertyValue.class, "mpv"); //$NON-NLS-1$
		tablePrefix.put(SurveyWaypoint.class, "sw"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnit.class, "su"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttribute.class, "sua"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttributeValue.class, "suav"); //$NON-NLS-1$
		tablePrefix.put(SamplingUnitAttributeListItem.class, "suli"); //$NON-NLS-1$
		
		tablePrefix.put(Intelligence.class, "i"); //$NON-NLS-1$
		tablePrefix.put(Informant.class, "ii"); //$NON-NLS-1$
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(IntelligenceSource.class, "iis"); //$NON-NLS-1$
		tablePrefix.put(IntelligencePoint.class, "iip"); //$NON-NLS-1$
		tablePrefix.put(Label.class, "lbl"); //$NON-NLS-1$
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
		
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(PatrolWaypoint.class, "smart.patrol_waypoint"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Team.class, "smart.team"); //$NON-NLS-1$
		tableNames.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tableNames.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		
		tableNames.put(Entity.class, "smart.entity"); //$NON-NLS-1$
		tableNames.put(EntityType.class, "smart.entity_type"); //$NON-NLS-1$
		tableNames.put(EntityAttribute.class, "smart.entity_attribute"); //$NON-NLS-1$
		tableNames.put(EntityAttributeValue.class, "smart.entity_attribute_value"); //$NON-NLS-1$
		
		tableNames.put(SurveyDesign.class, "smart.survey_design"); //$NON-NLS-1$
		tableNames.put(Survey.class, "smart.survey"); //$NON-NLS-1$
		tableNames.put(Mission.class, "smart.mission"); //$NON-NLS-1$
		tableNames.put(MissionDay.class, "smart.mission_day"); //$NON-NLS-1$
		tableNames.put(MissionAttribute.class, "smart.mission_attribute"); //$NON-NLS-1$
		tableNames.put(MissionAttributeListItem.class, "smart.mission_attribute_list"); //$NON-NLS-1$
		tableNames.put(MissionProperty.class, "smart.mission_property"); //$NON-NLS-1$
		tableNames.put(MissionTrack.class, "smart.mission_track"); //$NON-NLS-1$
		tableNames.put(MissionMember.class, "smart.mission_member"); //$NON-NLS-1$
		tableNames.put(MissionPropertyValue.class, "smart.mission_property_value"); //$NON-NLS-1$
		tableNames.put(SurveyWaypoint.class, "smart.survey_waypoint"); //$NON-NLS-1$
		tableNames.put(SamplingUnit.class, "smart.sampling_unit"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttribute.class, "smart.sampling_unit_attribute"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttributeValue.class, "smart.sampling_unit_attribute_value"); //$NON-NLS-1$
		tableNames.put(SamplingUnitAttributeListItem.class, "smart.sampling_unit_attribute_list"); //$NON-NLS-1$
		
		tableNames.put(Intelligence.class, "smart.intelligence"); //$NON-NLS-1$
		tableNames.put(Informant.class, "smart.informant"); //$NON-NLS-1$
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(IntelligenceSource.class, "smart.intelligence_source"); //$NON-NLS-1$
		tableNames.put(IntelligencePoint.class, "smart.intelligence_point"); //$NON-NLS-1$
		tableNames.put(Label.class, "smart.i18n_label"); //$NON-NLS-1$
	}
		
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	public void dropTable(Connection c, String tableName) throws SQLException  {
		String sql = "DROP TABLE IF EXISTS " + tableName; //$NON-NLS-1$
		logger.finest(sql);
		c.createStatement().execute(sql);
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
	 * Removes any temporary tables generated during
	 * execution retrieval of results. 
	 */
	public abstract void cleanUp(Session session);
	
	
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String[] getCategoryLabels(UUID uuid, Locale l, Session session){
		Category category = (Category) session.load(Category.class, uuid);
		ArrayList<String> values = new ArrayList<String>();
		values.add(category.getName());
		Category parent = category.getParent();
		while(parent != null){
			values.add(parent.getName());
			parent = parent.getParent();
		}
		Collections.reverse(values);
		return values.toArray(new String[values.size()]);
		
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
				return MessageFormat.format("{0} {1} [{2}]", x.getGivenName(), x.getFamilyName(), x.getId());
			}
		}
		return null;
	}
	
	protected String getName(UUID uuid, UUID cauuid, Session session){
//		if (SmartDB.isMultipleAnalysis()){
//			//need find label for the given conservation area
//			return Label.getDescription(uuid, cauuid);	
//		}else{
			return Label.getDescription(uuid, session);
//		}
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
			return "double precision"; //$NON-NLS-1$
		case BOOLEAN:
			return "double precision"; //$NON-NLS-1$
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
		logger.finest(sql.toString());
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
		PsqlNamedPreparedStatement pp = new PsqlNamedPreparedStatement(connection, query);
		StringBuilder log = new StringBuilder();
		for (Entry<String, Object> entry : currentParameters.entrySet()){
			if (entry.getValue() instanceof UUID){
				pp.setObject(entry.getKey(), (UUID)entry.getValue());
				log.append("'"+ UuidUtils.uuidToString((UUID)entry.getValue()) + "', "); //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				pp.setObject(entry.getKey(), entry.getValue());
				log.append(entry.getValue().toString().toString() + ", "); //$NON-NLS-1$
			}
		}
		logger.finest(log.toString());
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
	

	public abstract String getSurveySamplingUnitJoinFieldName();
	
	
	/**
	 * Creates the filter processor based on the query filter type
	 * 
	 * @param filterType
	 * @param queryDataTable
	 * @return
	 */
	protected abstract IFilterProcessor getFilterProcessor(IFilter.FilterType filterType, 
			String queryDataTable);
	
	public static ConservationAreaFilter parseConservationAreaFilter(Query query){
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			//TODO: get all valid cas from the database
			//and pass as second argument
			return ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(),
					Collections.singleton(query.getConservationArea()));
		}else{
			//we don't care 
			return ConservationAreaFilter.parseFilter(query.getConservationAreaFilter(),
					Collections.singleton(query.getConservationArea()));
		}
		
	}
}
