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
package org.wcs.smart.connect.query.engine.observation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.IFilterProcessor;
import org.wcs.smart.connect.query.engine.PsqlFilterToSqlGenerator;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.query.engine.visitor.AreaFilterVisitor;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.CategoryAttributeFilter;
import org.wcs.smart.query.model.filter.CategoryFilter;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.model.filter.Operator;

/**
 * Processes an query filter creating a temporary table
 * of the data that matches the filter.
 * 
 * @author Emily
 *
 */
public class ObsWaypointFilterProcessor implements IFilterProcessor{

	private final Logger logger = Logger.getLogger(ObsWaypointFilterProcessor.class.getName());
	
	private String tableName;
	private String waypointTable;
	
	private AbstractQueryEngine engine;

	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public ObsWaypointFilterProcessor(String tableName, AbstractQueryEngine engine){
		this.tableName = tableName;
		this.engine = engine;
		this.waypointTable = engine.createTempTableName();
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c) throws SQLException{
		engine.dropTable(c, waypointTable);
		
		for (String tableName: engine.filterTables.values()){
			engine.dropTable(c,  tableName);
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
			boolean includeEmptyObservations) throws SQLException{
		
		IFilter qFilter = queryFilter;
		
		if (qFilter == null){
			qFilter = EmptyFilter.INSTANCE;
		}
		createWaypointTable(c, qFilter, dateFilter, caFilter);
		createTemporaryTable(c);
		populateTemporaryTable(qFilter, dateFilter, caFilter, 
				includeEmptyObservations, c, populateObservation);
	}
	
	
	/*
	 * creates the query observation flattened table
	 */
	private void createTemporaryTable(Connection c) throws SQLException {

		String createTableStatement = engine.getTemporaryTableCreateClause(tableName);
		logger.finest(createTableStatement);
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
	 * @throws SQLException
	 */
	private void populateTemporaryTable(IFilter queryFilter,
			DateFilter dateFilter, 
			ConservationAreaFilter caFilter,
			boolean onlyObservations,
			Connection c,
			boolean populateObservation)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		
		engine.clearParameters();
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(engine.getTemporaryTableSelectClause(populateObservation));

		HashSet<Class<?>> usedTables = new HashSet<Class<?>>();
		
		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(namePrefix(Waypoint.class));
		sql.append(" join "); //$NON-NLS-1$
		sql.append(waypointTable + " as waypointTable "); //$NON-NLS-1$
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
		sql.append("waypointTable.wp_uuid "); //$NON-NLS-1$
		
		if (caFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (filter.length() > 0) {
				sql.append(" AND "); //$NON-NLS-1$
				sql.append("(" + filter + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	
		
		if (dateFilter != null) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (filter.length() > 0) {
				sql.append(" and "); //$NON-NLS-1$
				sql.append(filter);
			}
		}
		
		if (populateObservation){
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid = "); //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class) + ".wp_uuid "); //$NON-NLS-1$
		}
			
		for (Entry<IFilter, String> cols : engine.filterTables.entrySet()){
			String colName = cols.getValue();
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(colName);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(colName +".wp_uuid = "); //$NON-NLS-1$
			sql.append(prefix(Waypoint.class) + ".uuid "); //$NON-NLS-1$
		}
			
		AreaFilterVisitor av = new AreaFilterVisitor(sql, engine);
		queryFilter.accept(av);

		sql.append(engine.appendFromClause(usedTables));
		
		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = PsqlFilterToSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		
		logger.finest(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();
	}
	
	
	private void createWaypointTable(Connection c, IFilter filter, 
			DateFilter dateFilter, ConservationAreaFilter caFilter)
			throws SQLException {
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + waypointTable + " (wp_uuid uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());
		
		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + waypointTable + "_wpuuid_idx on " + waypointTable + " (wp_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.finest(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		engine.clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(waypointTable);
		sql.append("(wp_uuid) SELECT "); //$NON-NLS-1$
		sql.append(prefix(Waypoint.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$

		

		sql.append(name(Waypoint.class));
		sql.append(" as ");//$NON-NLS-1$
		sql.append(prefix(Waypoint.class)); 
		
		boolean where = true;
		if (caFilter != null) {
			String cfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(caFilter, engine);
			if (cfilter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
				where = false;
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(cfilter);
				sql.append( " ) "); //$NON-NLS-1$
			}
		}
		
		if (dateFilter != null) {
			String dfilter = PsqlFilterToSqlGenerator.INSTANCE.toSql(dateFilter, engine);
			if (dfilter.length() > 0) {
				if (where){
					sql.append(" WHERE "); //$NON-NLS-1$
				}else{
					sql.append(" and "); //$NON-NLS-1$
				}
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(dfilter);
				sql.append( " ) "); //$NON-NLS-1$
			}
		}

		
		logger.finest(sql.toString());
		engine.parseQueryString(c, sql.toString()).executeUpdate();

		IFilterVisitor attProcessor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if ( filter instanceof AttributeFilter ||
					filter instanceof CategoryFilter  ||	
					filter instanceof CategoryAttributeFilter ){						
					
					String colName = engine.createTempTableName();
					engine.filterTables.put(filter, colName);
				}
			}
		};
		filter.accept(attProcessor);
		
		for (Entry<IFilter, String> cols : engine.filterTables.entrySet()){
			IFilter lfilter = cols.getKey();
			String colName = cols.getValue();
			engine.clearParameters();
			
			sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(colName);
			sql.append("(wp_uuid uuid)"); //$NON-NLS-1$
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());


			sql = new StringBuilder();
			sql.append("CREATE INDEX "); //$NON-NLS-1$
			sql.append(colName + "_wp_uuid_idx on "); //$NON-NLS-1$
			sql.append(colName + "(wp_uuid) "); //$NON-NLS-1$
			logger.finest(sql.toString());
			c.createStatement().execute(sql.toString());
			
			
			sql = new StringBuilder();
			sql.append("INSERT INTO "); //$NON-NLS-1$
			sql.append(colName + " (wp_uuid)"); //$NON-NLS-1$	
			sql.append(" SELECT distinct ");  //$NON-NLS-1$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_uuid");  //$NON-NLS-1$
			
			AttributeFilter attfilter = null;
			CategoryFilter catfilter = null;
			if (lfilter instanceof AttributeFilter){
				attfilter = (AttributeFilter) lfilter;
			}else if (lfilter instanceof CategoryAttributeFilter){
				attfilter = ((CategoryAttributeFilter) lfilter).getAttributeFilter();
				catfilter = ((CategoryAttributeFilter) lfilter).getCategoryFilter();
			}else if (lfilter instanceof CategoryFilter){
				catfilter = (CategoryFilter) lfilter;
			}
			
			sql.append(" FROM ");  //$NON-NLS-1$
			sql.append(waypointTable);
			sql.append(" join ");  //$NON-NLS-1$
			sql.append(namePrefix(WaypointObservation.class));
			sql.append(" on " + waypointTable + ".wp_uuid = "); //$NON-NLS-1$  //$NON-NLS-2$
			sql.append(prefix(WaypointObservation.class));
			sql.append(".wp_uuid"); //$NON-NLS-1$

			if (catfilter != null){
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(Category.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(Category.class));
				sql.append(".uuid = "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservation.class));
				sql.append(".category_uuid "); //$NON-NLS-1$
			}
			if (attfilter != null){
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(WaypointObservationAttribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservation.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class) + ".observation_uuid "); //$NON-NLS-1$
				sql.append(" join "); //$NON-NLS-1$
				sql.append(namePrefix(Attribute.class));
				sql.append(" on "); //$NON-NLS-1$
				sql.append(prefix(Attribute.class) + ".uuid = "); //$NON-NLS-1$
				sql.append(prefix(WaypointObservationAttribute.class) + ".attribute_uuid "); //$NON-NLS-1$
				if (attfilter.getAttributeType() == AttributeType.LIST){
					sql.append(" join "); //$NON-NLS-1$
					sql.append(namePrefix(AttributeListItem.class));
					sql.append(" on "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class) + ".list_element_uuid = "); //$NON-NLS-1$
					sql.append(prefix(AttributeListItem.class) + ".uuid"); //$NON-NLS-1$
				}
				if (attfilter.getAttributeType() == AttributeType.TREE){
					sql.append(" join "); //$NON-NLS-1$
					sql.append(namePrefix(AttributeTreeNode.class));
					sql.append(" on "); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class) + ".tree_node_uuid = "); //$NON-NLS-1$
					sql.append(prefix(AttributeTreeNode.class) + ".uuid"); //$NON-NLS-1$
				}
			}
			sql.append(" WHERE "); //$NON-NLS-1$
			if (catfilter != null){
				String keyPart = catfilter.getCategoryKey();
				String p1 = engine.addParameterValue(keyPart+ "%");
				sql.append(" ( "); //$NON-NLS-1$
				sql.append(prefix(Category.class));
				sql.append(".hkey like " + p1 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (attfilter != null){
				if (catfilter != null){
					sql.append(" AND "); //$NON-NLS-1$
				}
				sql.append(prefix(Attribute.class) + ".keyid='" + attfilter.getAttributeKey() + "' AND "); //$NON-NLS-1$  //$NON-NLS-2$
				if (attfilter.getAttributeType() == AttributeType.NUMERIC){
					sql.append("("); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class));
					sql.append(".number_value "); //$NON-NLS-1$
					sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
					String p1 = engine.addParameterValue((Double)attfilter.getValue());
					sql.append(" " + p1 + ") "); //$NON-NLS-1$ //$NON-NLS-2$
				}else if (attfilter.getAttributeType() == AttributeType.BOOLEAN){
					sql.append("("); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class));
					sql.append(".number_value > 0.5 "); //$NON-NLS-1$
					sql.append(") "); //$NON-NLS-1$
				}else if (attfilter.getAttributeType() == AttributeType.TEXT){
					sql.append("(lower("); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class));
					sql.append(".string_value) "); //$NON-NLS-1$
					
					if (attfilter.getOperator() == Operator.STR_CONTAINS || attfilter.getOperator() == Operator.STR_NOTCONTAINS){
						String p1 = engine.addParameterValue("%" + ((String)attfilter.getValue()).toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$
						sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()) + " " + p1 + " )"); //$NON-NLS-1$ //$NON-NLS-2$  	
					}else if (attfilter.getOperator() == Operator.STR_EQUALS){
						String p1 = engine.addParameterValue(((String)attfilter.getValue()).toLowerCase());
						sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()) + " " + p1 + " )");  //$NON-NLS-1$ //$NON-NLS-2$  
					}
				}else if (attfilter.getAttributeType() == AttributeType.LIST){
					sql.append("("); //$NON-NLS-1$
					sql.append(prefix(AttributeListItem.class));
					sql.append(".keyid ");  //$NON-NLS-1$
					
					if (((String)attfilter.getValue()).equals(AttributeFilter.ANY_OPTION_KEY)){
						sql.append (" is not null "); //$NON-NLS-1$
					}else{
						String p1 = engine.addParameterValue((String)attfilter.getValue());
						sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
						sql.append(" " + p1);  //$NON-NLS-1$ 
					}
					sql.append(") "); //$NON-NLS-1$
					
				}else if (attfilter.getAttributeType() == AttributeType.TREE){
					sql.append("("); //$NON-NLS-1$
					sql.append(prefix(AttributeTreeNode.class));
					String p1 = engine.addParameterValue(((String)attfilter.getValue()) + "%");
					sql.append(".hkey like " + p1 + " ) " );  //$NON-NLS-1$ //$NON-NLS-2$ 
				}else if (attfilter.getAttributeType() == AttributeType.DATE){
					String p1 = engine.addParameterValue(attfilter.getValue());
					String p2 = engine.addParameterValue(attfilter.getValue2());
					
					sql.append("("); //$NON-NLS-1$
					sql.append(" DATE ("); //$NON-NLS-1$
					sql.append(prefix(WaypointObservationAttribute.class));
					sql.append(".string_value ) "); //$NON-NLS-1$
					sql.append(PsqlFilterToSqlGenerator.asSql(attfilter.getOperator()));
					sql.append(" cast(" + p1 + " as date)"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(PsqlFilterToSqlGenerator.asSql(Operator.AND));
					sql.append(" cast(" + p2 + " as date)"); //$NON-NLS-1$ //$NON-NLS-2$
					sql.append(") "); //$NON-NLS-1$
				}
			}
			
			logger.finest(sql.toString());
			engine.parseQueryString(c, sql.toString()).executeUpdate();
		}
	}
}
