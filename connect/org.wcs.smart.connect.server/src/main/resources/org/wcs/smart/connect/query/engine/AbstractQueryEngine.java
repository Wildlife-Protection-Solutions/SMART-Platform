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
import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.query.QueryManager;
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
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.util.UuidUtils;

/**
 * Query engine shared functionality.
 * 
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryEngine implements IQueryEngine {
	
	protected final Logger logger = Logger.getLogger(AbstractQueryEngine.class.getName());
	
	protected Map<String, Object> currentParameters = new HashMap<String, Object>();
	public HashMap<IFilter, String> filterTables = new HashMap<IFilter, String>();
	protected Session session;
	protected Locale locale = Locale.getDefault();
	protected int categoryCount = -1;
	
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
		tablePrefix.put(Employee.class, "e"); //$NON-NLS-1$
		
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
		tablePrefix.put(IntelligenceSource.class, "iis"); //$NON-NLS-1$
		tablePrefix.put(IntelligencePoint.class, "iip"); //$NON-NLS-1$
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
		tableNames.put(WaypointObservationAttribute.class, "smart.wp_observation_attributes"); //$NON-NLS-1$
		tableNames.put(Attribute.class, "smart.dm_attribute"); //$NON-NLS-1$
		tableNames.put(Category.class, "smart.dm_category"); //$NON-NLS-1$
		tableNames.put(AttributeTreeNode.class, "smart.dm_attribute_tree"); //$NON-NLS-1$
		tableNames.put(AttributeListItem.class, "smart.dm_attribute_list"); //$NON-NLS-1$
		tableNames.put(Area.class, "smart.area_geometries"); //$NON-NLS-1$
		tableNames.put(Employee.class, "smart.employee"); //$NON-NLS-1$
		
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
		tableNames.put(Language.class, "smart.language"); //$NON-NLS-1$
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
		s.createSQLQuery(sql).executeUpdate();
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
	public String createTempTableName(){
		return "query_temp_" + System.nanoTime(); //$NON-NLS-1$
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
	public String getEmployeeName(UUID uuid, Session session){
		if (uuid != null){
			Employee x = (Employee) session.load(Employee.class, uuid);
			if (x != null) {
				return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(x, locale);
			}
		}
		return null;
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
	 * Parses the conservation area filter based on the query conservation
	 * area string and the query uuid.  For ConservationArea based queries this
	 * will ignore the query conservation area string and return only the ca the query
	 * belongs to.  
	 * 
	 * @param query
	 * @return
	 */
	public static ConservationAreaFilter parseConservationAreaFilter(Query query){
		if (query.getConservationArea().getUuid().equals(ConservationArea.MULTIPLE_CA)){
			ConservationAreaFilter caFilter = new ConservationAreaFilter();
			String uuids[] = query.getConservationAreaFilter().split(ConservationAreaFilter.CA_SPLITTER);
			for (String uuid : uuids){
				caFilter.addConservationArea(UuidUtils.stringToUuid(uuid));
			}
			return caFilter;
			
		}else{
			//we don't care
			ConservationAreaFilter caFilter = new ConservationAreaFilter();
			caFilter.addConservationArea(query.getConservationArea());
			return caFilter;
		}
		
	}
	
	public String getEntityDmAttributeKey(String entityKey, ConservationAreaFilter caFilter, Connection c){
		String dmEntityTypeAttributeKey;
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT " + tablePrefix(Attribute.class) + ".keyid"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(tableNamePrefix(EntityType.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(tableNamePrefix(Attribute.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class) + ".dm_attribute_uuid = "); //$NON-NLS-1$
		sql.append(tablePrefix(Attribute.class) + ".uuid "); //$NON-NLS-1$
		sql.append(" WHERE "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class));
		sql.append(".keyid = '" + entityKey + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" AND "); //$NON-NLS-1$
		sql.append(tablePrefix(EntityType.class));
		sql.append(".ca_uuid IN ("); //$NON-NLS-1$
		for (UUID uuid : caFilter.getConservationAreaFilterIds()){
			sql.append("'" + uuid.toString() + "',"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(")"); //$NON-NLS-1$
		try{
			logger.finest(sql.toString());
			ResultSet rs = c.createStatement().executeQuery(sql.toString());
			if (rs.next()){
				dmEntityTypeAttributeKey = rs.getString(1);
			}else{
				throw new RuntimeException(MessageFormat.format("Entity not found for entity attribute key {0}.", new Object[]{entityKey})); //$NON-NLS-1$
			}
			rs.close();
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
		
		return dmEntityTypeAttributeKey;
	}
	
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
				
				org.hibernate.Query q = session.createSQLQuery("SELECT srid, srtext FROM " + spatialRefSys + " WHERE auth_name = :auth and auth_srid = :srid"); //$NON-NLS-1$ //$NON-NLS-2$
				q.setString("auth", auth); //$NON-NLS-1$
				q.setInteger("srid", code); //$NON-NLS-1$
				Object[] data = (Object[]) q.uniqueResult();
				if (data != null){
					CoordinateReferenceSystem c = CRS.parseWKT((String)data[1]);
					if (CRS.equalsIgnoreMetadata(crs, c)){
						return (Integer)data[0];
					}
				}
			}
		}catch (Exception ex){
			logger.log(Level.FINEST, ex.getMessage(), ex);
		}
		//if above fails, lets search entire list for match
		@SuppressWarnings("unchecked")
		List<Object[]> choices = session.createSQLQuery("SELECT srid, srtext from " + spatialRefSys).list(); //$NON-NLS-1$
		for (Object[] data : choices){
			try{
				CoordinateReferenceSystem c = CRS.parseWKT((String)data[1]);
				if (CRS.equalsIgnoreMetadata(crs, c)){
					return (Integer)data[0];
				}
			}catch (Exception ex){
				logger.log(Level.FINEST, ex.getMessage(), ex);
			}
			
		}
		return null;
		
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
		sb.append(tableName + "." + uuid); //$NON-NLS-1$
		sb.append(" = "); //$NON-NLS-1$
		sb.append(tablePrefix(Label.class) + ".element_uuid"); //$NON-NLS-1$
		sb.append(" ORDER BY CASE WHEN upper(code) = '" + locale.toString().toUpperCase() + "' THEN 1 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" CASE WHEN upper(code) = '" + locale.getLanguage().toUpperCase() + "' THEN 2 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" CASE WHEN isdefault THEN 3 ELSE 4 END END END LIMIT 1)"); //$NON-NLS-1$

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
		categoryCount = QueryManager.INSTANCE.getCategoryDepth(session, caFilter);
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
		String sql = "create index " + queryDataTable + "_categories_uuid_idx on " + queryDataTable + "_categories(uuid)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql);
		c.createStatement().execute(sql);
		
		sql = "analyze " + queryDataTable + "_categories"; //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql);
		c.createStatement().execute(sql);
		sql = "analyze " + queryDataTable ; //$NON-NLS-1$
		logger.finest(sql);
		c.createStatement().execute(sql);
		
		//populate labels
		for (int i = 0; i <categoryCount; i ++){
			sb = new StringBuilder();
			sb.append("UPDATE " + queryDataTable + " SET category_" + i + " = "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append(" (SELECT "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class));
			sb.append(".value FROM "); //$NON-NLS-1$
			sb.append(tableNamePrefix(Label.class));
			sb.append(","); //$NON-NLS-1$
			sb.append(tableNamePrefix(Language.class));
			sb.append(","); //$NON-NLS-1$
			sb.append(categoryTable + " mash"); //$NON-NLS-1$
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append(tablePrefix(Label.class) + ".language_uuid = "); //$NON-NLS-1$
			sb.append(tablePrefix(Language.class) + ".uuid "); //$NON-NLS-1$
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(queryDataTable + ".OB_CATEGORY_UUID "  ); //$NON-NLS-1$
			sb.append(" = "); //$NON-NLS-1$
			sb.append("mash.uuid "); //$NON-NLS-1$
			sb.append(" AND "); //$NON-NLS-1$
			sb.append("mash.mashup_" + (i+1) + " = " ); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(tablePrefix(Label.class) + ".element_uuid"); //$NON-NLS-1$
			sb.append(" ORDER BY CASE WHEN upper(code) = '" + locale.toString().toUpperCase() + "' THEN 1 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" CASE WHEN upper(code) = '" + locale.getLanguage().toUpperCase() + "' THEN 2 ELSE "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" CASE WHEN isdefault THEN 3 ELSE 4 END END END LIMIT 1)"); //$NON-NLS-1$
			
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
}
