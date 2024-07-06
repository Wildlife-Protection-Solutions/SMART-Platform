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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;

import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.NamedPreparedStatement;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.query.QueryManager;
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
import org.wcs.smart.filter.AttributeFilter;
import org.wcs.smart.filter.AttributeFilter.GeometryProperty;
import org.wcs.smart.filter.CategoryAttributeFilter;
import org.wcs.smart.filter.CategoryFilter;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.patrol.model.PatrolAttributeListItem;
import org.wcs.smart.patrol.model.PatrolAttributeValue;
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
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * Query engine shared functionality.
 * 
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryEngine implements IQueryEngine {
	
	/**
	 * Parameter key for providing if uuids should be included in query export results
	 */
	public static final String INCLUDE_UUID_PARAMETER = "org.wcs.smart.includeUuids"; //$NON-NLS-1$
	
	protected final Logger logger = Logger.getLogger(AbstractQueryEngine.class.getName());
	
	protected Map<String, Object> currentParameters = new HashMap<String, Object>();
	public HashMap<IFilter, FilterTable> filterTables = new HashMap<>();
	protected Session session;
	protected Locale locale = Locale.getDefault();
	protected int categoryCount = -1;
	
	protected ConservationAreaFilter caFilter = null;
	
	/**
	 * Maps database tables to a prefix to use in the query.
	 */
	protected static HashMap<Class<?>, String> tablePrefix = new HashMap<Class<?>, String>();
	static {
		tablePrefix = new HashMap<Class<?>, String>();
		tablePrefix.put(ConservationArea.class, "ca"); //$NON-NLS-1$
		tablePrefix.put(Waypoint.class, "wp"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservationGroup.class, "wpg"); //$NON-NLS-1$
		tablePrefix.put(WaypointObservation.class, "wpo"); //$NON-NLS-1$
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
		
		tablePrefix.put(Patrol.class, "p"); //$NON-NLS-1$
		tablePrefix.put(PatrolLeg.class, "pl"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegDay.class, "pld"); //$NON-NLS-1$
		tablePrefix.put(PatrolWaypoint.class, "pwp"); //$NON-NLS-1$
		tablePrefix.put(PatrolLegMember.class, "plm"); //$NON-NLS-1$
		tablePrefix.put(Track.class, "t"); //$NON-NLS-1$
		tablePrefix.put(Team.class, "smart.team"); //$NON-NLS-1$
		tablePrefix.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tablePrefix.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttribute.class, "spa"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttributeListItem.class, "spal"); //$NON-NLS-1$
		tablePrefix.put(PatrolAttributeValue.class, "spav"); //$NON-NLS-1$
		
//		tablePrefix.put(Entity.class, "e"); //$NON-NLS-1$
//		tablePrefix.put(EntityType.class, "et"); //$NON-NLS-1$
//		tablePrefix.put(EntityAttribute.class, "ea"); //$NON-NLS-1$
//		tablePrefix.put(EntityAttributeValue.class, "eav"); //$NON-NLS-1$
//		
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
		
//		tablePrefix.put(Intelligence.class, "i"); //$NON-NLS-1$
//		tablePrefix.put(Informant.class, "ii"); //$NON-NLS-1$
//		tablePrefix.put(IntelligenceSource.class, "iis"); //$NON-NLS-1$
//		tablePrefix.put(IntelligencePoint.class, "iip"); //$NON-NLS-1$
		tablePrefix.put(Label.class, "lbl"); //$NON-NLS-1$
		tablePrefix.put(Language.class, "ll"); //$NON-NLS-1$
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
		
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
		tableNames.put(PatrolLeg.class, "smart.patrol_leg"); //$NON-NLS-1$
		tableNames.put(PatrolLegDay.class, "smart.patrol_leg_day"); //$NON-NLS-1$
		tableNames.put(PatrolWaypoint.class, "smart.patrol_waypoint"); //$NON-NLS-1$
		tableNames.put(PatrolLegMember.class, "smart.patrol_leg_members"); //$NON-NLS-1$
		tableNames.put(Track.class, "smart.track"); //$NON-NLS-1$
		tableNames.put(Team.class, "smart.team"); //$NON-NLS-1$
		tableNames.put(PatrolTransportType.class, "smart.patrol_transport"); //$NON-NLS-1$
		tableNames.put(PatrolMandate.class, "smart.patrol_mandate"); //$NON-NLS-1$
		tableNames.put(PatrolAttribute.class, "smart.patrol_attribute"); //$NON-NLS-1$
		tableNames.put(PatrolAttributeListItem.class, "smart.patrol_attribute_list"); //$NON-NLS-1$
		tableNames.put(PatrolAttributeValue.class, "smart.patrol_attribute_value"); //$NON-NLS-1$
		
//		tableNames.put(Entity.class, "smart.entity"); //$NON-NLS-1$
//		tableNames.put(EntityType.class, "smart.entity_type"); //$NON-NLS-1$
//		tableNames.put(EntityAttribute.class, "smart.entity_attribute"); //$NON-NLS-1$
//		tableNames.put(EntityAttributeValue.class, "smart.entity_attribute_value"); //$NON-NLS-1$
		
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
		
//		tableNames.put(Intelligence.class, "smart.intelligence"); //$NON-NLS-1$
//		tableNames.put(Informant.class, "smart.informant"); //$NON-NLS-1$
		tableNames.put(Patrol.class, "smart.patrol"); //$NON-NLS-1$
//		tableNames.put(IntelligenceSource.class, "smart.intelligence_source"); //$NON-NLS-1$
//		tableNames.put(IntelligencePoint.class, "smart.intelligence_point"); //$NON-NLS-1$
		tableNames.put(Label.class, "smart.i18n_label"); //$NON-NLS-1$
		tableNames.put(Language.class, "smart.language"); //$NON-NLS-1$
	}
	
	/**
	 * Searches the parameters for the include uuids flag and returns true or false
	 * based on the false value 
	 * 
	 * @param params
	 * @return
	 */
	protected boolean getIncludeUuids(HashMap<String,Object> params) { 
		if (params.containsKey(AbstractQueryEngine.INCLUDE_UUID_PARAMETER)) {
			return (boolean) params.get(AbstractQueryEngine.INCLUDE_UUID_PARAMETER);
		}
		return false;
	}
	/**
	 * 
	 * @return the active session if set; null otherwise
	 */
	public Session getCurrentSession(){
		return this.session;
	}
	
	/**
	 * 
	 * @return the requested locale or the default locale if not set
	 */
	public Locale getLocale(){
		return this.locale;
	}
	
	/**
	 * Drop the created temporary tables.
	 * 
	 * @param c connection 
	 * @throws SQLException
	 */
	public void dropTable(Session s, String tableName) throws SQLException  {
		String sql = "DROP TABLE IF EXISTS " + tableName; //$NON-NLS-1$
		logger.finest(sql);
		s.createNativeMutationQuery(sql).executeUpdate();
	}

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
	public synchronized String createTempTableName(){
		return "query_temp.query_temp_" + System.nanoTime(); //$NON-NLS-1$
	}

	public String getIndexName(String tableName){
		int index = tableName.lastIndexOf('.');
		if (index > 0){
			return tableName.substring(index + 1);
		}
		return tableName;
	}

	/**
	 * Removes any temporary tables generated during
	 * execution retrieval of results. 
	 */
	public abstract void cleanUp(Session session) throws SQLException;
	
	
	/**
	 * Loads the category object from the session
	 * 
	 * 
	 * @param suuid
	 * @param session
	 * @return
	 */
	protected String[] getCategoryLabels(UUID uuid, Locale l, Session session){
		Category category = (Category) session.get(Category.class, uuid);
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
	public String getEmployeeName(UUID uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.get(Employee.class, uuid);
			if (x != null) {
				return getEmployeeName(x);
			}
		}
		return null;
	}

	public String getEmployeeName(Employee x){
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(x, locale);
	}
	
	/**
	 * Returns the database data type for a given 
	 * attribute type.
	 * @param type the attribute type
	 * @return the database datatype for the observation
	 * temporary table
	 */
	public String getDataType(AttributeInfo info) {
		switch (info.getType()) {
		case LIST:
			return "varchar(128)"; //keyid //$NON-NLS-1$
		case TREE:
			return "varchar(32672)"; ///hkey //$NON-NLS-1$
		case NUMERIC:
			return "double precision"; //$NON-NLS-1$
		case BOOLEAN:
			return "double precision"; //$NON-NLS-1$
		case TEXT:
			return "varchar(" + Attribute.STRING_ATTRIBUTE_MAX_LENGTH + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		case DATE:
			return "varchar(10)"; //$NON-NLS-1$
		case TIME:
			return "varchar(32)"; //$NON-NLS-1$
		case MLIST:
			throw new IllegalArgumentException();
		case LINE:
		case POLYGON:
			if (info.getGeometryProperty() == GeometryProperty.AREA || info.getGeometryProperty() == GeometryProperty.PERIMETER)
				return "double precision"; //$NON-NLS-1$
			throw new UnsupportedOperationException("Geometry property not support in queries"); //$NON-NLS-1$
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
		sql.append("CREATE INDEX " + getIndexName(tableName) + "_ob_uuid_idx on " +  tableName + "(ob_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	
	public PsqlNamedPreparedStatement parseQueryString(Connection connection, String query) throws SQLException{
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
		return pp;
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
	 * Parses the conservation area filter based on the query conservation
	 * area string and the query uuid.  For ConservationArea based queries this
	 * will ignore the query conservation area string and return only the ca the query
	 * belongs to.  
	 * 
	 * @param query
	 * @return
	 */
	protected void parseConservationAreaFilterInternal(Query query){
		this.caFilter = parseConservationAreaFilter(query);
	}
	
	public static ConservationAreaFilter parseConservationAreaFilter(Query query){
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			ConservationAreaFilter tmp = new ConservationAreaFilter();
			String uuids[] = query.getConservationAreaFilter().split(ConservationAreaFilter.CA_SPLITTER);
			for (String uuid : uuids){
				tmp.addConservationArea(UuidUtils.stringToUuid(uuid));
			}
			return tmp;
			
		}else{
			//we don't care
			ConservationAreaFilter tmp = new ConservationAreaFilter();
			tmp.addConservationArea(query.getConservationArea());
			return tmp;
		}
		
	}
//	public String getEntityDmAttributeKey(String entityKey, ConservationAreaFilter caFilter, Connection c){
//		String dmEntityTypeAttributeKey;
//		StringBuilder sql = new StringBuilder();
//		sql.append("SELECT " + tablePrefix(Attribute.class) + ".keyid"); //$NON-NLS-1$ //$NON-NLS-2$
//		sql.append(" FROM "); //$NON-NLS-1$
//		sql.append(tableNamePrefix(EntityType.class));
//		sql.append(" join "); //$NON-NLS-1$
//		sql.append(tableNamePrefix(Attribute.class));
//		sql.append(" on "); //$NON-NLS-1$
//		sql.append(tablePrefix(EntityType.class) + ".dm_attribute_uuid = "); //$NON-NLS-1$
//		sql.append(tablePrefix(Attribute.class) + ".uuid "); //$NON-NLS-1$
//		sql.append(" WHERE "); //$NON-NLS-1$
//		sql.append(tablePrefix(EntityType.class));
//		sql.append(".keyid = '" + entityKey + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//		sql.append(" AND "); //$NON-NLS-1$
//		sql.append(tablePrefix(EntityType.class));
//		sql.append(".ca_uuid IN ("); //$NON-NLS-1$
//		for (UUID uuid : caFilter.getConservationAreaFilterIds()){
//			sql.append("'" + uuid.toString() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		sql.deleteCharAt(sql.length() - 1);
//		sql.append(")"); //$NON-NLS-1$
//		try{
//			logger.finest(sql.toString());
//			ResultSet rs = c.createStatement().executeQuery(sql.toString());
//			if (rs.next()){
//				dmEntityTypeAttributeKey = rs.getString(1);
//			}else{
//				throw new RuntimeException(MessageFormat.format("Entity not found for entity attribute key {0}.", new Object[]{entityKey})); //$NON-NLS-1$
//			}
//			rs.close();
//		}catch (Exception ex){
//			throw new RuntimeException(ex);
//		}
//		
//		return dmEntityTypeAttributeKey;
//	}
	
	/**
	 * Finds the srid from the spatial_ref_sys table that matches
	 * the given crs.  Will return null if none found.
	 * 
	 * @param crs
	 * @return
	 * @throws NamingException 
	 */
	public Integer findPostgresqlProjectionSrid(CoordinateReferenceSystem crs) throws NamingException{
		//try to parse the epsg code and match to one from the database
		String spatialRefSys = (String) EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.SPATIAL_REF_SYS_TABLE);
		try{
			String crsid = CRS.lookupIdentifier(crs,  true );
			if (crsid.contains(":")){ //$NON-NLS-1$
				String auth = crsid.split(":")[0]; //$NON-NLS-1$
				int code = Integer.parseInt(crsid.split(":")[1]); //$NON-NLS-1$
				
				org.hibernate.query.NativeQuery<Tuple> q = session.createNativeQuery("SELECT srid, srtext FROM " + spatialRefSys + " WHERE auth_name = :auth and auth_srid = :srid", Tuple.class); //$NON-NLS-1$ //$NON-NLS-2$
				q.setParameter("auth", auth); //$NON-NLS-1$
				q.setParameter("srid", code); //$NON-NLS-1$
				Tuple data =  q.uniqueResult();
				if (data != null){
					CoordinateReferenceSystem c = CRS.parseWKT((String)data.get(1));
					if (CRS.equalsIgnoreMetadata(crs, c)){
						return (Integer)data.get(0);
					}
				}
			}
		}catch (Exception ex){
			logger.log(Level.FINEST, ex.getMessage(), ex);
		}
		//if above fails, lets search entire list for match
		List<Tuple> choices = session.createNativeQuery("SELECT srid, srtext from " + spatialRefSys, Tuple.class).list(); //$NON-NLS-1$
		for (Tuple data : choices){
			try{
				CoordinateReferenceSystem c = CRS.parseWKT((String)data.get(1));
				if (CRS.equalsIgnoreMetadata(crs, c)){
					return (Integer)data.get(0);
				}
			}catch (Exception ex){
				logger.log(Level.FINEST, ex.getMessage(), ex);
			}
			
		}
		return null;
		
	}
	
	public void createLabelTable(Session session, String labelTable ) {
		StringBuilder sql = new StringBuilder();
		sql.append( "CREATE TABLE "); //$NON-NLS-1$
		sql.append( labelTable );
		sql.append("(uuid uuid, value varchar(1024))"); //$NON-NLS-1$
		session.createNativeMutationQuery(sql.toString()).executeUpdate();
	}
	/**
	 * Populates the observation label table with all the list elements
	 * and tree nodes in the result set.
	 * @param c
	 * @param session
	 * @throws SQLException
	 */
	public void populateListTreeDataTable(Session session, String obsTable, String labelTable) throws SQLException {
		
		
		StringBuilder sql = new StringBuilder();
		
		sql.append(" insert into " + labelTable + "(uuid) select distinct "); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append( "case when list_element_uuid is not null then list_element_uuid "); //$NON-NLS-1$
		sql.append(" when tree_node_uuid is not null then tree_node_uuid else null end "); //$NON-NLS-1$
//		sql.append( "case when list_element_uuid is not null then 'l' "); //$NON-NLS-1$
//		sql.append(" when tree_node_uuid is not null then 't' else null end, "); //$NON-NLS-1$
//		sql.append(" r.ca_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append( obsTable + " r on "); //$NON-NLS-1$
		sql.append( tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" WHERE list_element_uuid is not null or tree_node_uuid is not null "); //$NON-NLS-1$
		sql.append(" UNION DISTINCT "); //$NON-NLS-1$
		sql.append( "SELECT distinct "); //$NON-NLS-1$
		sql.append( tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append( tableNamePrefix(WaypointObservationAttribute.class) );
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append( obsTable  + " r on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttribute.class));
		sql.append(".OBSERVATION_UUID = r.OB_UUID "); //$NON-NLS-1$
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttribute.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid"); //$NON-NLS-1$
		
		session.createNativeMutationQuery(sql.toString()).executeUpdate();
		
		session.doWork(new Work() {

			@Override
			public void execute(Connection c) throws SQLException {
				updateLabel(c, labelTable, "uuid", "value"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		});
		
	}
	
	/** 
	 * Looks up the name from the i18n_label table based on the locale
	 * Matches the country and language first, language only second, and default
	 * last.
	 * 
	 * @param c
	 * @param tableName table to update
	 * @param uuid element uuid column
	 * @param value varchar column to update
	 * @param l
	 * @throws SQLException
	 */
	protected void updateLabel(Connection c, String tableName, String uuid, String value) throws SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append( "UPDATE "); //$NON-NLS-1$
		sb.append(tableName);
		sb.append(" SET "); //$NON-NLS-1$
		sb.append( value );
		sb.append(" = a.lbl FROM (SELECT "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".value as lbl, "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".element_uuid as uuid "); //$NON-NLS-1$
		sb.append("FROM "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Label.class));
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Language.class));
		sb.append(" ON "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".language_uuid  = "); //$NON-NLS-1$
		sb.append(tablePrefix(Language.class) + ".uuid "); //$NON-NLS-1$
		sb.append(" JOIN ( SELECT DISTINCT " + uuid + " FROM " + tableName + ") z ON z." + uuid + " = " + tablePrefix(Label.class) + ".element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		sb.append(" WHERE upper(code) = upper('" + locale.toString() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (!locale.toString().toUpperCase(Locale.ROOT).endsWith(locale.getLanguage().toUpperCase(Locale.ROOT))){
			sb.append(" UNION "); //$NON-NLS-1$
			
			sb.append(" SELECT "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class));
			sb.append(".value as lbl, "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class));
			sb.append(".element_uuid as uuid "); //$NON-NLS-1$
			sb.append("FROM "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Label.class));
			sb.append(" JOIN "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Language.class));
			sb.append(" ON "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class));
			sb.append(".language_uuid  = "); //$NON-NLS-1$
			sb.append(tablePrefix(Language.class) + ".uuid "); //$NON-NLS-1$
			sb.append(" JOIN ( SELECT DISTINCT " + uuid + " FROM " + tableName + ") z ON z." + uuid + " = " + tablePrefix(Label.class) + ".element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			sb.append(" WHERE upper(code) = upper('" + locale.getLanguage() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
			
			sb.append("AND " + tablePrefix(Label.class) + ".element_uuid not in ("); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" SELECT " + tablePrefix(Label.class) + ".element_uuid as uuid FROM " + tableNamePrefix(Label.class) + " JOIN " + tableNamePrefix(Language.class) + " on " + tablePrefix(Label.class) + ".language_uuid = " + tablePrefix(Language.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			sb.append(" JOIN ( SELECT DISTINCT " + uuid + " FROM " + tableName + ") z ON z." + uuid + " = " + tablePrefix(Label.class) + ".element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			sb.append(" WHERE  upper(code) = upper('" +  locale.toString() + "'))"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		sb.append(" UNION "); //$NON-NLS-1$
		
		sb.append(" SELECT "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".value as lbl, "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".element_uuid as uuid "); //$NON-NLS-1$
		sb.append("FROM "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Label.class));
		sb.append(" JOIN "); //$NON-NLS-1$
		sb.append(tableNamePrefix(Language.class));
		sb.append(" ON "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class));
		sb.append(".language_uuid  = "); //$NON-NLS-1$
		sb.append(tablePrefix(Language.class) + ".uuid "); //$NON-NLS-1$
		sb.append(" JOIN ( SELECT DISTINCT " + uuid + " FROM " + tableName + ") z ON z." + uuid + " = " + tablePrefix(Label.class) + ".element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		sb.append(" WHERE " + tablePrefix(Language.class) + ".isdefault "); //$NON-NLS-1$ //$NON-NLS-2$
		
		sb.append("AND " + tablePrefix(Label.class) + ".element_uuid not in ("); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" SELECT " + tablePrefix(Label.class) + ".element_uuid as uuid FROM " + tableNamePrefix(Label.class) + " JOIN " + tableNamePrefix(Language.class) + " on " + tablePrefix(Label.class) + ".language_uuid = " + tablePrefix(Language.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		sb.append(" JOIN ( SELECT DISTINCT " + uuid + " FROM " + tableName + ") z ON z." + uuid + " = " + tablePrefix(Label.class) + ".element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		sb.append(" WHERE  upper(code) IN( upper('" +  locale.toString() + "'),  upper('" +  locale.getLanguage() + "')))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		sb.append(") a "); //$NON-NLS-1$
		sb.append(" WHERE a.uuid = " + tableName + "." + uuid + " AND " + value + " is null "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		logger.finest(sb.toString());
		c.createStatement().execute(sb.toString());	
	}
	
	/**
	 * The number of category levels in the data model.
	 * @return
	 */
	public int getCategoryCnt(){
		return this.categoryCount;
	}
	
	/**
	 * Adds category column labels to the given data table.  It will add one column
	 * for each category level.  The column will be populated with the label value matching
	 * country and language first, language only second, then the default third.
	 * 
	 * @param c
	 * @param session
	 * @param caFilter
	 * @param queryDataTable
	 * @throws SQLException
	 */
	protected void populateTemporaryTableCategory(Connection c, Session session, 
			ConservationAreaFilter caFilter, String queryDataTable) throws SQLException {
		
		// get number of levels
		categoryCount = QueryManager.INSTANCE.getActiveCategoryDepth(session, caFilter);
		if (categoryCount < 0){
			return;
		}
		
		//add data model category columns
		for (int i = 0; i < categoryCount; i++) {
			String sql = "ALTER TABLE "+queryDataTable+" ADD category_"+i+" varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);
		}

		//create a table that lists the uuid for each category level for each
		//category used in an observation
		// category_uuid | uuid mashup | uuid level 0 | uuid level 1 | etc.
		String categoryTable = queryDataTable + "_categories"; //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(categoryTable);
		sb.append(" AS "); //$NON-NLS-1$
		sb.append(" SELECT c.uuid, b.mashup "); //$NON-NLS-1$
		for (int i = 0; i < categoryCount; i ++){
			sb.append(", cast(case when length(split_part(b.mashup, '.', " + (i+1) + ")) = 0 then null else split_part(b.mashup, '.', " + (i+1) + ") end as uuid )  as mashup_" + (i+1)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sb.append(" FROM " + tableNamePrefix(Category.class)); //$NON-NLS-1$
		sb.append(", ( WITH RECURSIVE allparts(uuid, srcuuid, mashup) AS ("); //$NON-NLS-1$
		sb.append(" SELECT distinct ob_category_uuid, ob_category_uuid, ob_category_uuid || '' FROM "); //$NON-NLS-1$
		sb.append(queryDataTable);
		sb.append(" UNION ALL "); //$NON-NLS-1$
		sb.append(" SELECT " + tablePrefix(Category.class) ); //$NON-NLS-1$
		sb.append(".parent_category_uuid, d.srcuuid, " + tablePrefix(Category.class) + ".parent_category_uuid || '.' || d.mashup "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" FROM " + tableNamePrefix(Category.class) + ", allparts d WHERE c.uuid = d.uuid )"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" SELECT y.srcuuid, y.mashup FROM (SELECT srcuuid, max(length(mashup)) as mashupcnt from allparts group by srcuuid) x, allparts y where x.srcuuid = y.srcuuid and x.mashupcnt = length(y.mashup)"); //$NON-NLS-1$
		sb.append(") b WHERE b.srcuuid = c.uuid"); //$NON-NLS-1$
		logger.finest(sb.toString());
		c.createStatement().execute(sb.toString());
		
		//create some indexes and analyze
		//analyze is important here for performance
		String sql = "create index " + getIndexName(queryDataTable) + "_categories_uuid_idx on " + queryDataTable + "_categories(uuid)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql);
		c.createStatement().execute(sql);
		
		sql = "analyze " + queryDataTable + "_categories"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql);
		c.createStatement().execute(sql);
		sql = "analyze " + queryDataTable ; //$NON-NLS-1$
		logger.finest(sql);
		c.createStatement().execute(sql);
		
		//find names for each level
		for (int i = 1; i <= categoryCount; i++) {
			sql = "ALTER TABLE " + categoryTable + " ADD name_" + i + " varchar(1024)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql);
			c.createStatement().execute(sql);

			sb = new StringBuilder();
			sb.append("UPDATE "); //$NON-NLS-1$
			sb.append(categoryTable);
			sb.append(" SET name_" + i); //$NON-NLS-1$
			sb.append(" = (SELECT "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class));
			sb.append(".value FROM "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Label.class));
			sb.append(","); //$NON-NLS-1$
			sb.append(tableNamePrefix(Language.class));
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class) + ".language_uuid = "); //$NON-NLS-1$
			sb.append(tablePrefix(Language.class) + ".uuid "); //$NON-NLS-1$
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class) + ".element_uuid = "); //$NON-NLS-1$
			sb.append(categoryTable);
			sb.append(".mashup_" + i); //$NON-NLS-1$
			sb.append(" ORDER BY CASE WHEN upper(code) = '" + locale.toString().toUpperCase() + "' THEN 1 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" CASE WHEN upper(code) = '" + locale.getLanguage().toUpperCase() + "' THEN 2 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" CASE WHEN isdefault THEN 3 ELSE 4 END END END LIMIT 1)"); //$NON-NLS-1$

			logger.finest(sb.toString());
			c.createStatement().execute(sb.toString());
		}
				
		//populate labels
		for (int i = 0; i <categoryCount; i ++){
			sb = new StringBuilder();
			sb.append("UPDATE " + queryDataTable + " SET category_" + i + " = name_" + (i+1)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append(" FROM "); //$NON-NLS-1$
			sb.append(categoryTable);
			sb.append(" ct WHERE uuid  = " + queryDataTable + ".ob_category_uuid"); //$NON-NLS-1$ //$NON-NLS-2$
			
			logger.finest(sb.toString());
			c.createStatement().execute(sb.toString());
		}
		
		//drop temporary table
		sql = "DROP TABLE " +categoryTable; //$NON-NLS-1$
		logger.finest(sql);
		c.createStatement().execute(sql);

	}
	
	/**
	 * populate a ca_id and ca_name column in the query data table if the query
	 * is a ccaa query.
	 * @param c
	 * @param queryDataTable
	 * @param query
	 * @throws SQLException
	 */
	protected void populateCaDetails(Connection c, String queryDataTable, String caUuidColumn, Query query) throws SQLException{
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			//ca id and names are only used for cross-ca analysis
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_id = (select id FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + "." + caUuidColumn +")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
			
			sql = new StringBuilder();
			sql.append("UPDATE "); //$NON-NLS-1$
			sql.append(queryDataTable);
			sql.append(" SET ca_name = (select name FROM "); //$NON-NLS-1$
			sql.append(tableNames.get(ConservationArea.class) + " a "); //$NON-NLS-1$
			sql.append("WHERE a.uuid = " + queryDataTable + "." + caUuidColumn + ")");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			logger.finest(sql.toString());
			c.createStatement().executeUpdate(sql.toString());
		}
	}
	
	/**
	 * Populate the last modified by column
	 * @param c
	 * @param queryDataTable
	 */
	protected void populatedLastModifiedName(Connection c, Session session, String queryDataTable) throws SQLException{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT wp_lastmodifiedby FROM "); //$NON-NLS-1$
		sql.append(queryDataTable);
		logger.finest(sql.toString());
				
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE "); //$NON-NLS-1$
		sb.append( queryDataTable );
		sb.append(" SET wp_lastmodifiedbyname = ? WHERE wp_lastmodifiedby = ?"); //$NON-NLS-1$
		logger.finest(sb.toString());
		
		PreparedStatement lastmodified = c.prepareStatement(sb.toString());
		int cnt = 0;
		try(ResultSet rs = c.createStatement().executeQuery(sql.toString())){
			while (rs.next()) {
				UUID uuid = (UUID) rs.getObject(1);
				if (uuid == null) continue;
				String name = getEmployeeName(uuid, session);
					
				if (name != null) {
					lastmodified.setString(1, name);
					lastmodified.setObject(2,  uuid);
					lastmodified.addBatch();
					cnt++;
					if (cnt >= 100){
						lastmodified.executeBatch();
						cnt = 0;
					}
				}
			}
			lastmodified.executeBatch();
		}
	}
	/**
	 * For waypoint date filter fields return the field name for filtering. Can return null
	 * if should use datetime from waypoint table.
	 * @param field
	 * @return
	 * @throws SQLException
	 */
	public String getDateFilterTable() throws SQLException{
		return null;
	}
	
	
	/**
	 * For waypoint date filter fields return the table name for filtering.  Can return null
	 * if should use datetime from waypoint table
	 * @param field
	 * @return
	 * @throws SQLException
	 */
	public String getDateFilterField() throws SQLException{
		return null;
	}
	
	/**
	 * Returns the ca filter associated with the query engine; only available AFTER the
	 * query has been executed
	 * @return
	 */
	public ConservationAreaFilter getCaFilter(){
		return this.caFilter;
	}
	
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
	
	public void createFilterTable(Connection c, FilterTable t) throws SQLException{
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(t.tablename);
		sql.append("("); //$NON-NLS-1$
		sql.append( t.primarykey + " uuid "); //$NON-NLS-1$
		if (t.secondarykey != null) {
			sql.append(","); //$NON-NLS-1$
			sql.append( t.secondarykey + " uuid "); //$NON-NLS-1$	
		}
		sql.append(")"); //$NON-NLS-1$
		
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());


		sql = new StringBuilder();
		sql.append("CREATE INDEX "); //$NON-NLS-1$
		sql.append(getIndexName(t.tablename) + "_wp_uuid_idx on "); //$NON-NLS-1$
		sql.append(t.tablename + "(" + t.primarykey + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	public void processWaypointGroupDataModelFilter(FilterTable t, IFilter lfilter, 
			String waypointTable, Connection c) throws SQLException{
		processDataModelFilter(t, lfilter, waypointTable, c);
	}
	
	public void processWaypointDataModelFilter(FilterTable t, IFilter lfilter, 
			String waypointTable, Connection c) throws SQLException {
		processDataModelFilter(t, lfilter, waypointTable, c);
	}
	
	private void processDataModelFilter(FilterTable t, IFilter lfilter, 
			String waypointTable, Connection c) throws SQLException {
		
		clearParameters();
		
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.primarykey ); //$NON-NLS-1$ 
		if (t.secondarykey != null) {
			sql.append(", " + t.secondarykey); //$NON-NLS-1$
		}
		sql.append(")"); //$NON-NLS-1$
		sql.append(" SELECT distinct ");  //$NON-NLS-1$
		
		sql.append(tablePrefix(WaypointObservationGroup.class));
		sql.append(".wp_uuid");  //$NON-NLS-1$
		if (t.secondarykey != null) {
			sql.append(","); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationGroup.class));
			sql.append(".uuid");  //$NON-NLS-1$
		}
		
		AttributeFilter attfilter = null;
		CategoryFilter catfilter = null;
		if (lfilter instanceof AttributeFilter){
			attfilter = (AttributeFilter) lfilter;
		}else if (lfilter instanceof CategoryAttributeFilter){
			attfilter = ((CategoryAttributeFilter) lfilter).getAttributeFilter();
			catfilter = ((CategoryAttributeFilter) lfilter).getCategoryFilter();
		}else if (lfilter instanceof CategoryFilter){
			catfilter = (CategoryFilter) lfilter;
		}else {
			throw new UnsupportedOperationException("this processor only supports data model filter"); //$NON-NLS-1$
		}
		
		sql.append(" FROM ");  //$NON-NLS-1$
		sql.append(waypointTable);

		sql.append(" join ");  //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservationGroup.class));
		
		sql.append(" on " + waypointTable + ".wp_uuid = "); //$NON-NLS-1$  //$NON-NLS-2$
		sql.append(tablePrefix(WaypointObservationGroup.class));
		sql.append(".wp_uuid "); //$NON-NLS-1$
		
		sql.append(" join ");  //$NON-NLS-1$
		sql.append(tableNamePrefix(WaypointObservation.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservationGroup.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(WaypointObservation.class));
		sql.append(".wp_group_uuid "); //$NON-NLS-1$

		if (catfilter != null){
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Category.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(Category.class));
			sql.append(".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class));
			sql.append(".category_uuid "); //$NON-NLS-1$
		}
		if (attfilter != null){
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservationAttribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservation.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$
			sql.append(" join "); //$NON-NLS-1$
			sql.append(tableNamePrefix(Attribute.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(tablePrefix(Attribute.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$
			if (attfilter.getAttributeType() == AttributeType.LIST){
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(AttributeListItem.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class) + ".list_element_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
			}
			if (attfilter.getAttributeType().equals(AttributeType.MLIST)) {
				processMultiSelectAttributeFilter(sql, attfilter);
			}
			if (attfilter.getAttributeType() == AttributeType.TREE){
				sql.append(" join "); //$NON-NLS-1$
				sql.append(tableNamePrefix(AttributeTreeNode.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class) + ".tree_node_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeTreeNode.class) + ".uuid"); //$NON-NLS-1$
			}
		}
		sql.append(" WHERE "); //$NON-NLS-1$
		if (catfilter != null){
			String keyPart = catfilter.getCategoryKey();
			String p1 = addParameterValue(keyPart+ "%"); //$NON-NLS-1$
			sql.append(" ( "); //$NON-NLS-1$
			sql.append(tablePrefix(Category.class));
			sql.append(".hkey like " + p1 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (attfilter != null){
			if (catfilter != null){
				sql.append(" AND "); //$NON-NLS-1$
			}
			sql.append(tablePrefix(Attribute.class) + ".keyid='" + attfilter.getAttributeKey() + "' "); //$NON-NLS-1$  //$NON-NLS-2$
			if (attfilter.getAttributeType() != AttributeType.MLIST) {
				sql.append(" AND "); //$NON-NLS-1$
			}
			if (attfilter.getAttributeType() == AttributeType.NUMERIC){
				sql.append("("); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class));
				sql.append(".number_value "); //$NON-NLS-1$
				sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
				String p1 = addParameterValue((Double)attfilter.getValue());
				sql.append(" " + p1 + ") "); //$NON-NLS-1$ //$NON-NLS-2$
			}else if (attfilter.getAttributeType() == AttributeType.BOOLEAN){
				sql.append("("); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class));
				sql.append(".number_value > 0.5 "); //$NON-NLS-1$
				sql.append(") "); //$NON-NLS-1$
			}else if (attfilter.getAttributeType() == AttributeType.TEXT){
				sql.append("(lower("); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class));
				sql.append(".string_value) "); //$NON-NLS-1$
				
				if (attfilter.getOperator() == Operator.STR_CONTAINS || attfilter.getOperator() == Operator.STR_NOTCONTAINS){
					String p1 = addParameterValue("%" + ((String)attfilter.getValue()) + "%"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()) + " LOWER(" + p1 + ") )"); //$NON-NLS-1$ //$NON-NLS-2$  	
				}else if (attfilter.getOperator() == Operator.STR_EQUALS){
					String p1 = addParameterValue(((String)attfilter.getValue()));
					sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()) + " LOWER(" + p1 + ") )");  //$NON-NLS-1$ //$NON-NLS-2$  
				}
			}else if (attfilter.getAttributeType() == AttributeType.LIST){
				sql.append("("); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeListItem.class));
				sql.append(".keyid ");  //$NON-NLS-1$
				
				if (((String)attfilter.getValue()).equals(AttributeFilter.ANY_OPTION_KEY)){
					sql.append (" is not null "); //$NON-NLS-1$
				}else{
					String p1 = addParameterValue((String)attfilter.getValue());
					sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
					sql.append(" " + p1);  //$NON-NLS-1$ 
				}
				sql.append(") "); //$NON-NLS-1$
				
				
			}else if (attfilter.getAttributeType() == AttributeType.TREE){
				sql.append("("); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeTreeNode.class));
				String p1 = addParameterValue(((String)attfilter.getValue()) + "%"); //$NON-NLS-1$
				sql.append(".hkey like " + p1 + " ) " );  //$NON-NLS-1$ //$NON-NLS-2$ 
			}else if (attfilter.getAttributeType() == AttributeType.DATE){

				String p1 = addParameterValue(attfilter.getValue());
				String p2 = addParameterValue(attfilter.getValue2());
				
				//order of execution in where in postgresql is not determined
				//so it will try to parse all string values as dates and fail
				//so here we check the attribute type before parsing the string value.

				sql.append("("); //$NON-NLS-1$
				sql.append(" CASE WHEN "); //$NON-NLS-1$
				sql.append(tablePrefix(Attribute.class) + ".att_type = 'DATE' THEN"); //$NON-NLS-1$
				sql.append(" DATE ("); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class));
				sql.append(".string_value ) "); //$NON-NLS-1$
				sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
				sql.append(" cast(" + p1 + " as date)"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(PsqlFilterToSqlGenerator.asSql(Operator.AND));
				sql.append(" cast(" + p2 + " as date)"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(" ELSE FALSE END ) "); //$NON-NLS-1$					
			}else if (attfilter.getAttributeType() == AttributeType.TIME){

				String p1 = addParameterValue(attfilter.getValue());
				String p2 = addParameterValue(attfilter.getValue2());
				
				//order of execution in where in postgresql is not determined
				//so it will try to parse all string values as dates and fail
				//so here we check the attribute type before parsing the string value.

				sql.append("("); //$NON-NLS-1$
				sql.append(" CASE WHEN "); //$NON-NLS-1$
				sql.append(tablePrefix(Attribute.class) + ".att_type = 'TIME' THEN"); //$NON-NLS-1$
				sql.append(" cast ("); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttribute.class));
				sql.append(".string_value as time) "); //$NON-NLS-1$
				sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
				sql.append(" cast(" + p1 + " as time)"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(PsqlFilterToSqlGenerator.asSql(Operator.AND));
				sql.append(" cast(" + p2 + " as time)"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(" ELSE FALSE END ) "); //$NON-NLS-1$					
			}
		}
		
		logger.finest(sql.toString());
		try(NamedPreparedStatement ps = parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	private void processMultiSelectAttributeFilter(StringBuilder sql, AttributeFilter attfilter) {
		String[] keys = ((String) attfilter.getValue()).split(AttributeFilter.MLIST_SEPERATOR);
		Operator op = attfilter.getOperator();

		if (op == Operator.OR) {
			sql.append(" JOIN ("); //$NON-NLS-1$
			sql.append("SELECT "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
			sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(tableNamePrefix(AttributeListItem.class));
			sql.append(" ON "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
			sql.append(" AND "); //$NON-NLS-1$
			sql.append(tablePrefix(AttributeListItem.class) +".keyid in ("); //$NON-NLS-1$
			for (String key : keys) {
				String px = addParameterValue(key);
				sql.append(px);
				sql.append(","); //$NON-NLS-1$
			}
			sql.deleteCharAt(sql.length() - 1);
			sql.append(")) foo"); //$NON-NLS-1$
			sql.append(" ON foo.observation_attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
				
			
		}
		if (op == Operator.AND || op == Operator.EXACT) {
			sql.append(" JOIN ("); //$NON-NLS-1$
			
			int cnt = 0;
			for (String key : keys) {
				String px = addParameterValue(key);
				if (cnt != 0) sql.append(" INTERSECT "); //$NON-NLS-1$
				cnt++;
				sql.append("SELECT "); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
				sql.append(" JOIN "); //$NON-NLS-1$
				sql.append(tableNamePrefix(AttributeListItem.class));
				sql.append(" ON "); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".list_element_uuid = "); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeListItem.class) + ".uuid "); //$NON-NLS-1$
				sql.append(" AND "); //$NON-NLS-1$
				sql.append(tablePrefix(AttributeListItem.class) +".keyid =" + px );	 //$NON-NLS-1$
			}
			
			if (op == Operator.EXACT) {
				String px = addParameterValue(keys.length);
				sql.append(" INTERSECT "); //$NON-NLS-1$
				sql.append("SELECT "); //$NON-NLS-1$
				sql.append(tablePrefix(WaypointObservationAttributeList.class) + ".observation_attribute_uuid FROM "); //$NON-NLS-1$
				sql.append(tableNamePrefix(WaypointObservationAttributeList.class));
				sql.append(" GROUP BY observation_attribute_uuid HAVING count(*) = " + px); //$NON-NLS-1$
			}
			sql.append(" ) k"); //$NON-NLS-1$
			
			sql.append(" ON k.observation_attribute_uuid = "); //$NON-NLS-1$
			sql.append(tablePrefix(WaypointObservationAttribute.class) + ".uuid"); //$NON-NLS-1$
		}
	}
	
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
}
