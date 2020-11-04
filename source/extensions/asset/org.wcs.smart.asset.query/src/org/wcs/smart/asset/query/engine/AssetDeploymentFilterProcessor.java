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
package org.wcs.smart.asset.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationAttributeValue;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter;
import org.wcs.smart.asset.query.parser.internal.filter.AssetAttributeFilter.Source;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.AbstractQueryEngine.FilterTable;
import org.wcs.smart.query.common.engine.IFilterProcessor;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.EmptyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Processes an asset deployment query filter creating a temporary table
 * of the asset deployment data that matches the filter.
 * 
 * @author Emily
 *
 */
public class AssetDeploymentFilterProcessor implements IFilterProcessor{

	private String tableName;
	
	private AssetQueryEngine engine;
	
	private String deploymentTable;
	
	/**
	 * Creates a new process filter
	 * 
	 * @param tableName the output temporary table name
	 * @param engine query engine
	 */
	public AssetDeploymentFilterProcessor(String tableName, AssetQueryEngine engine){
		this.tableName = tableName;
		this.engine = engine;
		this.deploymentTable = engine.createTempTableName();	
	}
	
	/**
	 * 
	 * drops temporary tables created during process of creating the main data table.
	 * Does not drop the main table.
	 */
	@Override
	public void dropTemporaryTables(Connection c){
		engine.dropTable(c, deploymentTable);
		
		for (FilterTable tableName: engine.filterTables.values()){
			engine.dropTable(c,  tableName.tablename);
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
		
		SubMonitor progress = SubMonitor.convert(monitor, 3);
		progress.subTask(Messages.AssetDeploymentFilterProcessor_TaskName1);
		
		IFilter qFilter = queryFilter;		
		if (qFilter == null) qFilter = EmptyFilter.INSTANCE;
		
		//filter on date and ca
		createDeploymentTable(c, dateFilter, caFilter, progress.split(1));
		
		//apply filters
		processFilters(qFilter, c, progress.split(1));
		
		progress.setTaskName(Messages.AssetDeploymentFilterProcessor_TaskName2);
		progress.split(1);
		populateTemporaryTable(qFilter, dateFilter, c);
		
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
	 * Asset.cass = "smart.asset a"
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
	 * @param onlyObservations if only observation asset records with observations
	 * are to be returned,  false will return all asset records
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
	private void populateTemporaryTable(IFilter queryFilter, DateFilter dateFilter, Connection c)
			throws SQLException {

		StringBuilder sql = new StringBuilder();
		sql.append(engine.getTemporaryTableCreateClause(tableName));
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		engine.createTemporaryTableIndexes(c, tableName);
		
		
		sql = new StringBuilder();
		
		engine.clearParameters();
		
		sql.append("INSERT INTO " + tableName ); //$NON-NLS-1$
		// ---- SELECT CLAUSE -----
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid, "); //$NON-NLS-1$
		
		
		LocalDate[] bits = dateFilter.getDateFilterOption().getDates(); 
		
		LocalDateTime filterStart = null;
		LocalDateTime filterEnd = null;
		
		if (bits != null){
			if (bits.length == 1){
				filterStart = bits[0].atTime(LocalTime.MIDNIGHT) ;
			}else if (bits.length == 2){
				filterStart = bits[0].atTime(LocalTime.MIDNIGHT);
				filterEnd = bits[1].atTime(LocalTime.MAX);
			}else {
				throw new IllegalStateException(Messages.AssetDeploymentFilterProcessor_InvalidDateFilter);
			}
		}
			
		String sField = prefix(AssetDeployment.class) + ".start_date"; //$NON-NLS-1$
		if (filterStart != null) {
			String p1 = engine.addParameterValue(filterStart);
			sql.append(" CASE WHEN " + sField + " > " + p1 + " then " + sField + " else " + p1  + " end, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}else {
			sql.append(sField + ","); //$NON-NLS-1$
		}
		
		String eField = prefix(AssetDeployment.class) + ".end_date"; //$NON-NLS-1$
		if (filterStart != null) {
			String p1 = engine.addParameterValue(filterEnd);
			sql.append(" CASE WHEN " + eField + " is not null AND " + eField + " < " + p1 + " THEN " + eField ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			sql.append( " WHEN " + eField + " is not null THEN " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" WHEN " + eField + " is null AND CURRENT_TIMESTAMP < " + p1 + " THEN CURRENT_TIMESTAMP "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sql.append(" ELSE " + p1 ); //$NON-NLS-1$
			sql.append(" END, "); //$NON-NLS-1$
		}else {
			sql.append(eField + ","); //$NON-NLS-1$
		}
		
		sql.append(prefix(AssetDeployment.class) + ".asset_uuid, "); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".station_location_uuid"); //$NON-NLS-1$

		// ---- FROM CLAUSE -----
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(deploymentTable + " a "); //$NON-NLS-1$
		sql.append(" join "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" on "); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = "); //$NON-NLS-1$
		sql.append(" a.deployment_uuid "); //$NON-NLS-1$
		
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			FilterTable t = cols.getValue();
			sql.append(" left join "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append(" on "); //$NON-NLS-1$
			sql.append(t.tablename +"." + t.columnname + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(prefix(AssetDeployment.class) + ".uuid "); //$NON-NLS-1$
		}

		// ---- WHERE CLAUSE -----
		if (queryFilter != EmptyFilter.INSTANCE) {
			String filter = AssetFilterSqlGenerator.INSTANCE.toSql(queryFilter, engine);
			if (filter != null && filter.length() > 0) {
				sql.append(" WHERE "); //$NON-NLS-1$
			    sql.append(filter);
			}
		}
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	
	private void createDeploymentTable(Connection c, 
			DateFilter dateFilter, ConservationAreaFilter caFilter, 
			IProgressMonitor monitor)
			throws SQLException {

		SubMonitor progress = SubMonitor.convert(monitor, 1);
		progress.subTask("Filtering deployments on date and Conservation Area"); //$NON-NLS-1$
		
		// -- build temporary table
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + deploymentTable + " (deployment_uuid char(16) for bit data)"); //$NON-NLS-1$ //$NON-NLS-2$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());
		
		// -- create index
		sql = new StringBuilder();
		sql.append("CREATE INDEX " + deploymentTable + "_depuuid_idx on " + deploymentTable + " (deployment_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QueryPlugIn.logSql(sql.toString());
		c.createStatement().execute(sql.toString());

		// -- populate table
		engine.clearParameters();
		sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(deploymentTable);
		sql.append("(deployment_uuid) SELECT "); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class));
		sql.append(".uuid "); //$NON-NLS-1$
		sql.append("FROM "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(Asset.class));
		sql.append(" ON " + prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		
		StringBuilder where = new StringBuilder();
		if (caFilter != null) {
			String cfilter = AssetFilterSqlGenerator.INSTANCE.asSql(caFilter, prefix(Asset.class), engine);
			if (cfilter.length() > 0) {
				where.append(cfilter);
			}
		}

		if (dateFilter != null) {

			LocalDate[] bits = dateFilter.getDateFilterOption().getDates(); 
			
			if (bits != null){
				StringBuilder df = new StringBuilder();
				String startField = prefix(AssetDeployment.class) + ".start_date"; //$NON-NLS-1$
				String endField = prefix(AssetDeployment.class) + ".end_date"; //$NON-NLS-1$
				if (bits.length == 1){
					String p1 = engine.addParameterValue(bits[0].atStartOfDay());
					String p2 = engine.addParameterValue(bits[0].atTime(LocalTime.MAX));
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
				}else if (bits.length == 2 && dateFilter.getDateFilterOption().isEndDateInclusive()){
					String p1 = engine.addParameterValue(bits[0].atStartOfDay());
					String p2 = engine.addParameterValue(bits[1].atTime(LocalTime.MAX));
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + endField  +" as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) <= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + startField  +" as date) <= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) >= " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
					
				}else if (bits.length == 2){
					String p1 = engine.addParameterValue(bits[0].atStartOfDay());
					String p2 = engine.addParameterValue(bits[1].atStartOfDay());
					
					df.append("( "); //$NON-NLS-1$
					df.append(" ( cast(" + startField + " as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + startField + " as date) < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + endField  +" as date) >= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) < " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(" OR "); //$NON-NLS-1$
					df.append(" ( cast(" + startField  +" as date) <= " + p1 ); //$NON-NLS-1$ //$NON-NLS-2$
					df.append(" and cast(" + endField + " as date) > " + p2 + " ) "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					df.append(")"); //$NON-NLS-1$
				}else {
					throw new IllegalStateException(Messages.AssetDeploymentFilterProcessor_InvalidDateFilter);
				}
				
				if (where.length() > 0) where.append(" and "); //$NON-NLS-1$
				where.append(df);
				
			}
		}
		
		if (where.length() > 0) {
			sql.append(" WHERE "); //$NON-NLS-1$
			sql.append(where);
		}

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}

	}
	
	private void processFilters(IFilter filter, Connection c, IProgressMonitor monitor) throws SQLException {

		IFilterVisitor attProcessor = new IFilterVisitor() {
			@Override
			public void visit(IFilter filter) {
				if (filter instanceof AssetFilter ||
					filter instanceof AssetAttributeFilter){						
					
					String colName = engine.createTempTableName();
					engine.filterTables.put(filter, new FilterTable(colName, "deployment_uuid")); //$NON-NLS-1$
				}
			}
		};
		filter.accept(attProcessor);
		
		SubMonitor progress = SubMonitor.convert(monitor, engine.filterTables.entrySet().size());
		for (Entry<IFilter, FilterTable> cols : engine.filterTables.entrySet()){
			
			IFilter lfilter = cols.getKey();
			FilterTable t = cols.getValue();
			
			progress.subTask(MessageFormat.format(Messages.AssetDeploymentFilterProcessor_TaskName3, lfilter.asString()));
			progress.split(1);
			engine.clearParameters();
			
			StringBuilder sql = new StringBuilder();
			sql.append("CREATE TABLE "); //$NON-NLS-1$
			sql.append(t.tablename);
			sql.append("(" + t.columnname + " char(16) for bit data)"); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());


			sql = new StringBuilder();
			sql.append("CREATE INDEX "); //$NON-NLS-1$
			sql.append(t.tablename + "_deployment_uuid_idx on "); //$NON-NLS-1$
			sql.append(t.tablename + "(" + t.columnname + ") "); //$NON-NLS-1$ //$NON-NLS-2$
			QueryPlugIn.logSql(sql.toString());
			c.createStatement().execute(sql.toString());
						
			if (lfilter instanceof AssetAttributeFilter) {
				processAssetAttributeFilter((AssetAttributeFilter)lfilter, t, c);		
			}else if (lfilter instanceof AssetFilter) {
				processAssetFilter((AssetFilter)lfilter, t, c);
			}else {
				throw new UnsupportedOperationException(MessageFormat.format(Messages.AssetDeploymentFilterProcessor_FilterTypeNotSupported, lfilter.getClass().getName()));
			}
		}
	}
	private void processAssetFilter(AssetFilter assetFilter, FilterTable t, Connection c) throws SQLException {
		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.deployment_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(deploymentTable + " a"); //$NON-NLS-1$
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = a.deployment_uuid "); //$NON-NLS-1$ 
		
		StringBuilder where = new StringBuilder();
		
		if (assetFilter.getAssetOption() == AssetFilterOption.ASSETTYPE) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(Asset.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(Asset.class) + ".asset_type_uuid = " + key); //$NON-NLS-1$
		}
		if (assetFilter.getAssetOption() == AssetFilterOption.ASSET) {
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetDeployment.class) + ".asset_uuid = " + key); //$NON-NLS-1$
		}
		
		if (assetFilter.getAssetOption() == AssetFilterOption.STATIONLOCATION) {
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + key); //$NON-NLS-1$
		}
		if (assetFilter.getAssetOption() == AssetFilterOption.STATION) {
			sql.append(" LEFT JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			String key = engine.addParameterValue(assetFilter.getValue());
			where.append(prefix(AssetStationLocation.class) + ".station_uuid = " + key); //$NON-NLS-1$
		}
		sql.append (" WHERE "); //$NON-NLS-1$
		sql.append(where);
		
		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
	
	private void processAssetAttributeFilter(AssetAttributeFilter assetFilter, FilterTable t, Connection c) throws SQLException {
		//create temporary table for attribute observations
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO "); //$NON-NLS-1$
		sql.append(t.tablename + " (" + t.columnname + ")"); //$NON-NLS-1$ //$NON-NLS-2$	
		sql.append(" SELECT "); //$NON-NLS-1$
		sql.append("a.deployment_uuid "); //$NON-NLS-1$
		sql.append(" FROM "); //$NON-NLS-1$
		sql.append(deploymentTable + " a"); //$NON-NLS-1$
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetDeployment.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(AssetDeployment.class) + ".uuid = a.deployment_uuid "); //$NON-NLS-1$ 
		
		StringBuilder where = new StringBuilder();
		
		Class<?> valueClass = null;
		if (assetFilter.getSource() == Source.ASSET) {
			valueClass = AssetAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(Asset.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".asset_uuid = " + prefix(Asset.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(Asset.class) + ".uuid = " + prefix(valueClass) + ".asset_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
		}else if (assetFilter.getSource() == Source.STATION) {
			valueClass = AssetStationAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".station_uuid = " + prefix(AssetStation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStation.class) + ".uuid = " + prefix(valueClass) + ".station_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			
			 
		}else if (assetFilter.getSource() == Source.STATIONLOCATION) {
			valueClass = AssetStationLocationAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetStationLocation.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".station_location_uuid = " + prefix(AssetStationLocation.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetStationLocation.class) + ".uuid = " + prefix(valueClass) + ".station_location_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
			
		}else if (assetFilter.getSource() == Source.DEPLOYMENT) {
			valueClass = AssetDeploymentAttributeValue.class;
			
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(valueClass));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(AssetDeployment.class) + ".uuid = " + prefix(valueClass) + ".asset_deployment_uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		}
			
		sql.append(" JOIN "); //$NON-NLS-1$
		sql.append(namePrefix(AssetAttribute.class));
		sql.append(" ON " ); //$NON-NLS-1$
		sql.append(prefix(valueClass) + ".attribute_uuid = " + prefix(AssetAttribute.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
		
		String key = engine.addParameterValue(assetFilter.getAttributeKey());
		sql.append(" AND " + prefix(AssetAttribute.class) + ".keyid = " + key);  //$NON-NLS-1$//$NON-NLS-2$
		
		String tprefix = prefix(valueClass);
		if (assetFilter.getAttributeType() == AttributeType.LIST) {
			sql.append(" JOIN "); //$NON-NLS-1$
			sql.append(namePrefix(AssetAttributeListItem.class));
			sql.append(" ON " ); //$NON-NLS-1$
			sql.append(prefix(valueClass) + ".list_item_uuid = " + prefix(AssetAttributeListItem.class) + ".uuid "); //$NON-NLS-1$ //$NON-NLS-2$
				
			tprefix = prefix(AssetAttributeListItem.class);
		}
		
		key = engine.addParameterValue(assetFilter.getAttributeKey());
		where.append(prefix(AssetAttribute.class) + ".keyid = " + key); //$NON-NLS-1$
			
		String q = AssetFilterSqlGenerator.INSTANCE.toSql(assetFilter, tprefix, engine);
		where.append(" AND "); //$NON-NLS-1$
		where.append(q);
		
		sql.append (" WHERE "); //$NON-NLS-1$
		sql.append(where);

		QueryPlugIn.logSql(sql.toString());
		try(PreparedStatement ps = engine.parseQueryString(c, sql.toString())){
			ps.executeUpdate();
		}
	}
		
	
}
