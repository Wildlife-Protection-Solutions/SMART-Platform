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
package org.wcs.smart.er.query.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.er.query.filter.SurveyDesignFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.common.engine.visitors.AttributeFilterCollectorVisitor;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.date.WaypointDateField;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class FilterProcessor implements IFilterProcessor {

	private String tableName;
	private String observationTable;
	private String missionTable;
	private String suAttributeTable;
	
	private DerbySurveyQueryEngine engine;
	private SurveyDesignFilter designFilter;
	
	private SurveyHasObservationFilterVisitor observationFilterVisitor = new SurveyHasObservationFilterVisitor();
	private MissionPropertyFilterCollectorVisitor mpcollector = new MissionPropertyFilterCollectorVisitor();
	private SamplingUnitAttributeFilterCollectorVisitor sucollector = new SamplingUnitAttributeFilterCollectorVisitor();
	
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public FilterProcessor(String tableName, DerbySurveyQueryEngine engine, SurveyDesignFilter designFilter ){
		this.tableName = tableName;
		this.engine = engine;
		this.observationTable = engine.createTempTableName();
		this.designFilter = designFilter;
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c){
		engine.dropTable(c, observationTable);
		if (missionTable != null){
			engine.dropTable(c, missionTable);
		}
	}

	/**
	 * 
	 * @param c database connection
	 * @param queryFilter query filter
	 * @param dateFilter date filter
	 * @param caFilter conservation area filter
	 * @param populateObservation if observation fields (wp_uuid, wp_ob_uuid) are to be populated
	 * @param includeEmptyObservations if waypoints with no observations should be included
	 * @param monitor
	 * @throws SQLException
	 */
	@Override
	public void processFilter(Connection c, IFilter queryFilter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter, 
			boolean populateObservation,
			boolean includeEmptyObservations,
			IProgressMonitor monitor) throws SQLException{
		
		monitor.beginTask(Messages.FilterProcessor_progress1, 50);
		
		IFilter qFilter = queryFilter;
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		
		//observation filter
		qFilter.accept(observationFilterVisitor);		
		if (observationFilterVisitor.hasAttributeFilter()){
			createObservationTable(c, qFilter, dateFilter, caFilter, monitor);
		}
		monitor.worked(10);
		if (monitor.isCanceled()){
			return;
		}

		//mission filters
		monitor.subTask(Messages.FilterProcessor_ProgressMissionFilters);
		queryFilter.accept(mpcollector);
		if (mpcollector.getAttributeInfo().size() > 0){
			this.missionTable = engine.createTempTableName();
			createMissionTable(c, qFilter, caFilter, dateFilter, monitor);
		}
		monitor.worked(10);
		
		monitor.subTask(Messages.FilterProcessor_ProgressSuFilters);
		queryFilter.accept(sucollector);
		if (sucollector.getAttributeInfo().size() > 0){
			this.suAttributeTable = engine.createTempTableName();
			createSamplingUnitAttributeTable(c, qFilter, caFilter, dateFilter, monitor);
		}
		monitor.worked(10);
		if (monitor.isCanceled()){
			return;
		}

		monitor.subTask(Messages.FilterProcessor_ProgressDataTableCreate);
		createTemporaryTable(c);
		monitor.worked(10);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.subTask(Messages.FilterProcessor_ProgressDataTablePopulate);
		populateTemporaryTable(qFilter, dateFilter, caFilter, 
				includeEmptyObservations, c, populateObservation);
		monitor.worked(10);
		if (monitor.isCanceled()){
			return;
		}
		
		monitor.done();
	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		QueryPlugIn.logSql(createTableStatement);
		c.createStatement().execute(createTableStatement);
		
		engine.buildTemporaryTableIndexes(c, tableName);
	}
	
	
	/*
	 * return the table name for the associate object 
	 */
	private String name(Class<?> clazz){
		return engine.tableName(clazz);
	}
	
	/*
	 * return the sql prefix for the given class
	 */
	private String prefix(Class<?> clazz){
		return engine.tablePrefix(clazz);
	}
	
	/*
	 * combine the table name with the table prefix for
	 * the given class
	 * Patrol.cass = "smart.patrol p"
	 */
	private String namePrefix(Class<?> clazz){
		return engine.tableNamePrefix(clazz);
	}
	
	/**
	 * Populates the query temporary table.
	 * 
	 * @param queryFilter the query filter
	 * @param dateFilter the date filter
	 * @param caFilter the conservation area filter
	 * @param onlyObservations if only observation patrol records with observations
	 * are to be returned,  false will return all patrol records
	 * even if they don't have an observation
	 * @param c database connection
	 * @param populateObservation if the processing requires the observation
	 * information attached to the results (otherwise ob_uuid will be populated
	 * with null)
	 * 
	 * @param c the database connection
	 * 
	 * @throws Exception
	 */
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		if (dateFilter.getDateFieldOption() != MissionStartDateField.INSTANCE 
				&& dateFilter.getDateFieldOption() != MissionEndDateField.INSTANCE
				&& dateFilter.getDateFieldOption() != WaypointDateField.INSTANCE){
			throw new SQLException(MessageFormat.format(Messages.FilterProcessor_DateFilterNotSupported, new Object[]{dateFilter.getDateFilterOption().getGuiName()}));
		}
		StringBuilder sql = new StringBuilder();
		
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(namePrefix(SurveyDesign.class));
		usedTables.add(SurveyDesign.class);
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(namePrefix(Survey.class));
		usedTables.add(Survey.class);
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(SurveyDesign.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(Survey.class));
		sql.append(".survey_design_uuid "); //$NON-NLS-1$
		
		if (designFilter != null){
			String filter = SurveyFilterSqlGenerator.INSTANCE.toSql(designFilter, engine);
			if (filter.length() > 0){
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (caFilter != null) {
			String filter = SurveyFilterSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(namePrefix(Mission.class));
		usedTables.add(Mission.class);
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(prefix(Survey.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(Mission.class));
		sql.append(".survey_uuid "); //$NON-NLS-1$
		
		sql.append(" inner join "); //$NON-NLS-1$
		sql.append(namePrefix(MissionDay.class));
		usedTables.add(MissionDay.class);
		sql.append(" on ");  //$NON-NLS-1$
		sql.append(prefix(Mission.class));
		sql.append(".uuid = "); //$NON-NLS-1$
		sql.append(prefix(MissionDay.class));
		sql.append(".mission_uuid "); //$NON-NLS-1$
		
		if (dateFilter != null){
			String filter = SurveyFilterSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		
		if (this.missionTable != null){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(missionTable + " mt"); //$NON-NLS-1$
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Mission.class));
			sql.append(".uuid = mt.mission_uuid"); //$NON-NLS-1$
		}

		if (populateObservation ){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(SurveyWaypoint.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(SurveyWaypoint.class));
			sql.append(".mission_day_uuid = "); //$NON-NLS-1$
			sql.append(prefix(MissionDay.class));
			sql.append(".uuid "); //$NON-NLS-1$
		
			if (onlyObservations){
				sql.append(" inner join "); //$NON-NLS-1$
			}else{
				sql.append(" left join "); //$NON-NLS-1$
			}
			sql.append(namePrefix(Waypoint.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(SurveyWaypoint.class));
			sql.append(".wp_uuid = "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class));
			sql.append(".uuid "); //$NON-NLS-1$
			usedTables.add(Waypoint.class);
			usedTables.add(SurveyWaypoint.class);

			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(SamplingUnit.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(SurveyWaypoint.class));
			sql.append(".sampling_unit_uuid = "); //$NON-NLS-1$
			sql.append(prefix(SamplingUnit.class));
			sql.append(".uuid "); //$NON-NLS-1$
				
			if (populateObservation){
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservation.class));
				usedTables.add(WaypointObservation.class);
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(Waypoint.class));
				sql.append(".uuid = "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservation.class));
				sql.append(".wp_uuid "); //$NON-NLS-1$
					
				sql.append(" left join "); //$NON-NLS-1$
				sql.append(name(Category.class));
				usedTables.add(Category.class);
				sql.append(" "); //$NON-NLS-1$
				sql.append(prefix(Category.class));
				
				sql.append(" on " + prefix(Category.class) //$NON-NLS-1$
					+ ".uuid = " //$NON-NLS-1$
					+ prefix(WaypointObservation.class)
					+ ".category_uuid "); //$NON-NLS-1$
					
				if (observationFilterVisitor.hasAttributeFilter()){
					sql.append(" left join "); //$NON-NLS-1$
					sql.append(observationTable + " qa on qa.observation_uuid = "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservation.class) + ".uuid"); //$NON-NLS-1$
				}
			}

		}
		
		//do need join mission tracks?
		final boolean[] needstracks = new boolean[]{false};
		IFilterVisitor missionTracks = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if (needstracks[0]) return;
				if ((filter instanceof SamplingUnitFilter
						&& ((SamplingUnitFilter)filter).getSource() == Source.TRACK) ||
					(filter instanceof SamplingUnitAttributeFilter &&
						((SamplingUnitAttributeFilter)filter).getSource() == Source.TRACK)){
					needstracks[0] = true;
				}
				
			}
		};
		queryFilter.accept(missionTracks);
		if (needstracks[0]){
			if (populateObservation){
				throw new SQLException("Cannot process a query that filters both observation and track items.");
			}
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(MissionTrack.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(MissionTrack.class));
			sql.append(".mission_day_uuid = "); //$NON-NLS-1$
			sql.append(prefix(MissionDay.class));
			sql.append(".uuid "); //$NON-NLS-1$
		}
		
		if (this.suAttributeTable != null){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(suAttributeTable + " sua"); //$NON-NLS-1$
			sql.append(" on "); //$NON-NLS-1$
			if (sucollector.getSource() == Source.OBSERVATION){
				sql.append(prefix(SurveyWaypoint.class));
				sql.append(".sampling_unit_uuid = sua.sampling_unit_uuid"); //$NON-NLS-1$
			}else{
				//join attributes to track
				sql.append(prefix(MissionTrack.class));
				sql.append(".sampling_unit_uuid = sua.sampling_unit_uuid"); //$NON-NLS-1$
			}
		}
		// area filters
		AreaFilterVisitor areaVisitor = new AreaFilterVisitor(sql, engine, usedTables);
		queryFilter.accept(areaVisitor);
		
		sql.append(engine.appendFromClause(usedTables));
		
		boolean where = true;
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = SurveyFilterSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
				where = false;
			    sql.append( "(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		
		if (dateFilter != null) {
			String filter = SurveyFilterSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
					where = false;
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append( "(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
	}
	
	
	private void createObservationTable(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter, IProgressMonitor monitor)
			throws SQLException {
		
		monitor.subTask(Messages.FilterProcessor_progress3);
		
		AttributeFilterCollectorVisitor collector = new AttributeFilterCollectorVisitor();
		filter.accept(collector);
		Collection<AttributeInfo> keys = collector.getAttributeInfo();
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + observationTable + " (observation_uuid char(16) for bit data"); //$NON-NLS-1$ //$NON-NLS-2$
		for (AttributeInfo key : keys) {
			sql.append(", " + key.getKey() + " " //$NON-NLS-1$ //$NON-NLS-2$
					+ engine.getDataType(key.getType()));
		}
		sql.append(")"); //$NON-NLS-1$
		
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + observationTable + "_obuuid_idx on " + observationTable + " (observation_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		String attributeTempTable = engine.createTempTableName();
			
		for (AttributeInfo key : keys){
			monitor.subTask(Messages.FilterProcessor_progress4  + key.getKey());
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(attributeTempTable); 
			sql.append("(observation_uuid char(16) for bit data, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class));
				sql.append(".observation_uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(prefix(WaypointObservationAttribute.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" "); //$NON-NLS-1$

				sql.append("FROM "); //$NON-NLS-1$
				
				
				sql.append(name(SurveyDesign.class));
				sql.append(" as "); //$NON-NLS-1$
				sql.append(prefix(SurveyDesign.class)); 
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Survey.class));
				sql.append( " as " ); //$NON-NLS-1$
				sql.append( prefix(Survey.class)); 
				sql.append(" ON " + prefix(SurveyDesign.class) + ".uuid = " + prefix(Survey.class) + ".survey_design_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (caFilter != null) {
					String cfilter = SurveyFilterSqlGenerator.INSTANCE.toSql(caFilter, engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
					
					
				}
				sql.append(" join "); //$NON-NLS-1$

				sql.append(name(Mission.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(Mission.class)); 
				sql.append(" ON " + prefix(Survey.class) + ".uuid = " + prefix(Mission.class) + ".survey_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$

				sql.append(name(MissionDay.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(MissionDay.class)); 
				sql.append(" ON " + prefix(Mission.class) + ".uuid = " + prefix(MissionDay.class) + ".mission_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$

				sql.append(name(SurveyWaypoint.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(SurveyWaypoint.class)); 
				sql.append(" on " + prefix(SurveyWaypoint.class) + ".mission_day_uuid = " + prefix(MissionDay.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Waypoint.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(Waypoint.class)); 
				sql.append(" on " + prefix(SurveyWaypoint.class) + ".wp_uuid = " + prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (dateFilter != null) {
					String dfilter = SurveyFilterSqlGenerator.INSTANCE.toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}

				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservation.class)
						+ " as " + prefix(WaypointObservation.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Waypoint.class) + ".uuid = " + prefix(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(WaypointObservationAttribute.class)
						+ " as " + prefix(WaypointObservationAttribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(WaypointObservation.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Attribute.class)
						+ " as " + prefix(Attribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(Attribute.class) + ".uuid = " + prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(WaypointObservationAttribute.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(AttributeTreeNode.class));
					sql.append(" t on t.uuid = " + prefix(WaypointObservationAttribute.class) + ".tree_node_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sql.append("WHERE "); //$NON-NLS-1$
				sql.append(" " + prefix(Attribute.class) + ".keyid = '"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(key.getKey());
				sql.append("'"); //$NON-NLS-1$

				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("__observation_uuid_idx on "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append("(observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" set "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE a.observation_uuid = "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(".observation_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append("(observation_uuid, "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  observation_uuid, value FROM "); //$NON-NLS-1$
				sql.append(attributeTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT observation_uuid FROM "); //$NON-NLS-1$
				sql.append(observationTable);
				sql.append(" b WHERE b.observation_uuid = a.observation_uuid))"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + attributeTempTable); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
		
	}
	
	protected void createMissionTable(Connection c, 
			IFilter filter, 
			ConservationAreaFilter caFilter,
			DateFilter dateFilter,
			IProgressMonitor monitor) throws SQLException{

		Set<AttributeInfo> keys = mpcollector.getAttributeInfo();
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(missionTable);
		sql.append("(mission_uuid char(16) for bit data,"); //$NON-NLS-1$
		for (AttributeInfo key : keys){
			sql.append("ma_" + key.getKey() + " " + engine.getDataType(key.getType())); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(",");	 //$NON-NLS-1$
		}
		sql.deleteCharAt(sql.length()-1);
		sql.append(")"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		
		String lTempTable = engine.createTempTableName();
		for (AttributeInfo key : keys){
			monitor.subTask(Messages.FilterProcessor_progress5  + key.getKey());
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(lTempTable); 
			sql.append("(mission_uuid char(16) for bit data, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" SELECT "); //$NON-NLS-1$
				sql.append(prefix(Mission.class));
				sql.append(".uuid, "); //$NON-NLS-1$

				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else if (key.getType() == AttributeType.TREE) {
					sql.append("t.hkey "); //$NON-NLS-1$
				} else {
					sql.append(prefix(MissionPropertyValue.class)
							+ "." + key.getColumn()); //$NON-NLS-1$						
				}
				sql.append(" as "); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" "); //$NON-NLS-1$

				sql.append("FROM "); //$NON-NLS-1$
				
				
				sql.append(name(SurveyDesign.class));
				sql.append(" as "); //$NON-NLS-1$
				sql.append(prefix(SurveyDesign.class)); 
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(Survey.class));
				sql.append( " as " ); //$NON-NLS-1$
				sql.append( prefix(Survey.class)); 
				sql.append(" ON " + prefix(SurveyDesign.class) + ".uuid = " + prefix(Survey.class) + ".survey_design_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (caFilter != null) {
					String cfilter = SurveyFilterSqlGenerator.INSTANCE.toSql(caFilter, engine);
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
					
					
				}
				sql.append(" join "); //$NON-NLS-1$

				sql.append(name(Mission.class));
				sql.append(" as ");//$NON-NLS-1$
				sql.append(prefix(Mission.class)); 
				sql.append(" ON " + prefix(Survey.class) + ".uuid = " + prefix(Mission.class) + ".survey_uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$

				sql.append(name(MissionPropertyValue.class)
						+ " as " + prefix(MissionPropertyValue.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(MissionPropertyValue.class) + ".mission_uuid = " + prefix(Mission.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(name(MissionAttribute.class)
						+ " as " + prefix(MissionAttribute.class)); //$NON-NLS-1$
				sql.append(" on " + prefix(MissionAttribute.class) + ".uuid = " + prefix(MissionPropertyValue.class) + ".mission_attribute_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(MissionAttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(MissionPropertyValue.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sql.append("WHERE "); //$NON-NLS-1$
				sql.append(" " + prefix(MissionAttribute.class) + ".keyid = '"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(key.getKey());
				sql.append("'"); //$NON-NLS-1$

				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append("_mission_uuid_idx on "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append("(mission_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(missionTable);
				sql.append(" set ma_"); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" a WHERE a.mission_uuid = "); //$NON-NLS-1$
				sql.append(missionTable);
				sql.append(".mission_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(missionTable);
				sql.append("(mission_uuid, ma_"); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  mission_uuid, value FROM "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT mission_uuid FROM "); //$NON-NLS-1$
				sql.append(missionTable);
				sql.append(" b WHERE b.mission_uuid = a.mission_uuid))"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + lTempTable); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
	}
	
	
	protected void createSamplingUnitAttributeTable(Connection c, 
			IFilter filter, 
			ConservationAreaFilter caFilter,
			DateFilter dateFilter,
			IProgressMonitor monitor) throws SQLException{

		Set<AttributeInfo> keys = sucollector.getAttributeInfo();
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE "); //$NON-NLS-1$
		sql.append(suAttributeTable);
		sql.append("(sampling_unit_uuid char(16) for bit data,"); //$NON-NLS-1$
		for (AttributeInfo key : keys){
			sql.append("sua_" + key.getKey() + " " + engine.getDataType(key.getType())); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(",");	 //$NON-NLS-1$
		}
		sql.deleteCharAt(sql.length()-1);
		sql.append(")"); //$NON-NLS-1$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		
		String lTempTable = engine.createTempTableName();
		for (AttributeInfo key : keys){
			monitor.subTask(Messages.FilterProcessor_progress5  + key.getKey());
			
			//create temporary table for attribute observations
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(lTempTable); 
			sql.append("(sampling_unit_uuid char(16) for bit data, value "); //$NON-NLS-1$
			sql.append( engine.getDataType(key.getType()) );
			sql.append( ")"); //$NON-NLS-1$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
			try {
				// -- populate table
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" SELECT DISTINCT "); //$NON-NLS-1$
				sql.append(prefix(SamplingUnit.class));
				sql.append(".uuid, "); //$NON-NLS-1$
				if (key.getType() == AttributeType.LIST) {
					sql.append("l.keyid "); //$NON-NLS-1$
				} else {
					sql.append(prefix(SamplingUnitAttributeValue.class) + "." + key.getColumn()); //$NON-NLS-1$						
				}
										
				sql.append(" as "); //$NON-NLS-1$
				sql.append("col_" + key.getKey());
				sql.append(" "); //$NON-NLS-1$
				
				sql.append("FROM "); //$NON-NLS-1$
				sql.append(namePrefix(SamplingUnitAttributeValue.class));
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(SamplingUnitAttribute.class));
				sql.append(" ON " + prefix(SamplingUnitAttributeValue.class) + ".su_attribute_uuid = ");  //$NON-NLS-1$//$NON-NLS-2$
				sql.append(prefix(SamplingUnitAttribute.class) + ".uuid AND "); //$NON-NLS-1$
				sql.append(prefix(SamplingUnitAttribute.class) + ".keyid = '" + key.getKey() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(SamplingUnit.class));
				sql.append(" on " + prefix(SamplingUnitAttributeValue.class) + ".su_uuid= " + prefix(SamplingUnit.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(SurveyWaypoint.class));
				sql.append(" on " + prefix(SurveyWaypoint.class) + ".sampling_unit_uuid= " + prefix(SamplingUnit.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(Waypoint.class));
				sql.append(" on " + prefix(SurveyWaypoint.class) + ".wp_uuid = " + prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(MissionDay.class));
				sql.append(" on " + prefix(SurveyWaypoint.class) + ".mission_day_uuid = " + prefix(MissionDay.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (dateFilter != null &&
						(dateFilter.getDateFieldOption() instanceof MissionStartDateField ||
								dateFilter.getDateFieldOption() instanceof MissionStartDateField)){
					sql.append(" join "); //$NON-NLS-1$
					sql.append(namePrefix(Mission.class));
					sql.append(" on " + prefix(MissionDay.class) + ".mission_uuid = " + prefix(Mission.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					
				}
				if (dateFilter != null) {
					String dfilter = SurveyFilterSqlGenerator.INSTANCE.toSql(dateFilter, engine);
					if (dfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(dfilter);
					}
				}
				
				if (caFilter != null) {
					String cfilter = SurveyFilterSqlGenerator.INSTANCE. asSql(caFilter, prefix(Waypoint.class));
					if (cfilter.length() > 0) {
						sql.append(" and "); //$NON-NLS-1$
						sql.append(cfilter);
					}
				}
				
				if (key.getType() == AttributeType.LIST) {
					sql.append(" JOIN "); //$NON-NLS-1$
					sql.append(name(SamplingUnitAttributeListItem.class));
					sql.append(" l on l.uuid = " + prefix(SamplingUnitAttributeValue.class) + ".list_element_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				sql.append("WHERE "); //$NON-NLS-1$
				sql.append(" " + prefix(SamplingUnitAttribute.class) + ".keyid = '"); //$NON-NLS-1$ //$NON-NLS-2$
				sql.append(key.getKey());
				sql.append("'"); //$NON-NLS-1$

				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - create index
				sql = new StringBuilder();
				sql.append("create index "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append("_su_uuid_idx on "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append("(sampling_unit_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

				// - add observation to main table
				// join existing readings
				sql = new StringBuilder();
				sql.append("UPDATE "); //$NON-NLS-1$
				sql.append(suAttributeTable);
				sql.append(" set sua_"); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(" = "); //$NON-NLS-1$
				sql.append("(SELECT a.value FROM "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" a WHERE a.sampling_unit_uuid = "); //$NON-NLS-1$
				sql.append(suAttributeTable);
				sql.append(".sampling_unit_uuid)"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
				
				// add missing observations
				sql = new StringBuilder();
				sql.append("INSERT INTO "); //$NON-NLS-1$
				sql.append(suAttributeTable);
				sql.append("(sampling_unit_uuid, sua_"); //$NON-NLS-1$
				sql.append(key.getKey());
				sql.append(")"); //$NON-NLS-1$
				sql.append("(SELECT  sampling_unit_uuid, value FROM "); //$NON-NLS-1$
				sql.append(lTempTable);
				sql.append(" a WHERE NOT EXISTS (SELECT sampling_unit_uuid FROM "); //$NON-NLS-1$
				sql.append(suAttributeTable);
				sql.append(" b WHERE b.sampling_unit_uuid = a.sampling_unit_uuid))"); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());

			} finally {
				// -- drop attribute table
				sql = new StringBuilder();
				sql.append("DROP TABLE " + lTempTable); //$NON-NLS-1$
				QueryPlugIn.logSql(sql.toString());
				c.createStatement().execute(sql.toString());
			}
		}
	}
}
