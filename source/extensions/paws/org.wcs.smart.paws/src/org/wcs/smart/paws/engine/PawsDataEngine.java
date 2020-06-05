/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.engine;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsRun.Status;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * PAWS Engine for complining data and starting a PAWS analysis
 * 
 * @author Emily
 *
 */
public class PawsDataEngine {

	public static final String DATA_FILE_NAME  = "SMARTdata.csv"; //$NON-NLS-1$
	
	public static final String CONFIG_FILE_NAME  = "config.json"; //$NON-NLS-1$
	
	private static AtomicLong tableCnter = new AtomicLong();

	private PawsRun run;
	
	//map from the classification name to a list of columns
	//that match this classification
	private HashMap<String, List<String>> classmappings;
	private List<Path> packageFiles;
	
	//the crs of the input file 
	private CoordinateReferenceSystem targetCrs = SmartDB.DATABASE_CRS;
	
	
	public PawsDataEngine(PawsRun run) {
		this.run = run;
	}
	
	private String createTempTable() {
		return "query_temp_paws_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	public Path createDataPackage() throws Exception{
		
		packageFiles = new ArrayList<>();
		
		Path workingDir = null;
		try(Session session = HibernateManager.openSession()){
			PawsRun mrun = (PawsRun) session.merge(run);
			workingDir = PawsFileManager.INSTANCE.getDirectory(mrun);
		}
		
		if (!Files.exists(workingDir)) Files.createDirectories(workingDir);
		
		
		Path dataFiles = workingDir.resolve(DATA_FILE_NAME);
		packageData(dataFiles);
		packageBasemapFiles(workingDir);
		createConfig(workingDir);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				session.saveOrUpdate(run);
				run.setPackageFile(workingDir.getFileName().toString());
				run.setStatus(Status.UPLOADING_DATA);
				run.setRunDate(LocalDateTime.now());
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();;
				throw ex;
			}
		}
		return workingDir;
	}
	
	public List<Path> getDataFiles(){
		return this.packageFiles;
	}
	
	@SuppressWarnings("unchecked")
	private void createConfig(Path target) throws Exception{
		Path configPath = target.resolve(CONFIG_FILE_NAME);
		
		JSONObject config = new JSONObject();
		
		try(Session session = HibernateManager.openSession()){
			
			run = (PawsRun) session.merge(run);

			config.put("container_name", run.getContainerName()); //$NON-NLS-1$
			config.put("run_id", run.getRunId()); //$NON-NLS-1$
			
			PawsParameter pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
			//this needs to be in meters
			config.put("spatial_resolution", pp.getValue()); //$NON-NLS-1$
					
			
			//shapefiles
			JSONObject shapefiles = new JSONObject();
			config.put("geo_feature_shape_files", shapefiles); //$NON-NLS-1$
			
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
			shapefiles.put("boundary_layer_name", "ca_boundary"); //$NON-NLS-1$ //$NON-NLS-2$
			shapefiles.put("boundary_file_name", "ca_boundary.zip"); //$NON-NLS-1$ //$NON-NLS-2$
			
			JSONArray other = new JSONArray();
			shapefiles.put("additional_shape_files", other); //$NON-NLS-1$
			for (PawsParameter ppc : run.getConfiguration().getParameters()) {
				if (!ppc.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())) continue;
				String value = ppc.getValue();
				if (value == null || value.isBlank()) continue;
				
				if (value.startsWith(PawsParameter.AREA_PREFIX)) {
					Area.AreaType atype = Area.AreaType.valueOf( value.substring(PawsParameter.AREA_PREFIX.length()) );
					
					JSONObject item = new JSONObject();
					item.put("file_name", atype.name() + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
					item.put("layer_name",  atype); //$NON-NLS-1$
					other.add(item);
					
				}else if (value.startsWith(PawsParameter.FILE_PREFIX)) {
					String filename = value.substring(PawsParameter.FILE_PREFIX.length());
					JSONObject item = new JSONObject();
					item.put("file_name",  SharedUtils.getFilenameWithoutExtension(filename) + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
					item.put("layer_name",  SharedUtils.getFilenameWithoutExtension(filename)); //$NON-NLS-1$
					other.add(item);
				}
			}

			//patrol_observation
			JSONObject patrolobs = new JSONObject();
			config.put("patrol_observations", patrolobs); //$NON-NLS-1$
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy"); //$NON-NLS-1$
			patrolobs.put("start_date", formatter.format(run.getDataStartDate())); //$NON-NLS-1$
			patrolobs.put("end_date", formatter.format(run.getDataEndDate())); //$NON-NLS-1$

//			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
//			if (pp == null) throw new Exception("Timezone value must be provided.  Update the configuration and try again.");
//			patrolobs.put("time_zone", pp.getValue());
			patrolobs.put("file_name", DATA_FILE_NAME); //$NON-NLS-1$
			
			
			//illegal class mappings
			JSONArray mappings = new JSONArray();
			
			//just add single mapping for now
			
			
			for (Entry<String, List<String>> firstmapping : classmappings.entrySet()) {
				
				JSONObject mapping = new JSONObject();
				mapping.put("classification_class", firstmapping.getKey()); //$NON-NLS-1$
				
				JSONArray filters = new JSONArray();
				
				JSONObject filter = new JSONObject();
				
				JSONArray columns = new JSONArray();
				for (String col : firstmapping.getValue()) {
					JSONObject column = new JSONObject();
					column.put("column_name", col); //$NON-NLS-1$
					column.put("match_value", firstmapping.getKey()); //$NON-NLS-1$
					columns.add(column);
				}
				
				filter.put("filter", columns); //$NON-NLS-1$
				filters.add(filter);
				
				
				mapping.put("classification_class_filters", filters); //$NON-NLS-1$
				
				mappings.add(mapping);
			}
			config.put("illegal_activity_class_mappings", mappings); //$NON-NLS-1$
			
//			config.put("protected_area_name", run.getConservationArea().getName());
		
			JSONObject modelexperimentation = new JSONObject();
			config.put("model_experimentation", modelexperimentation); //$NON-NLS-1$
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.TRAINING_RES.name());
			modelexperimentation.put("temporal_training_resolution_month_count", Integer.valueOf( (String)pp.getValue()) ); //$NON-NLS-1$
			modelexperimentation.put("train_start_year", run.getTrainStartYear()); //$NON-NLS-1$
			modelexperimentation.put("train_end_year", run.getTrainEndYear()); //$NON-NLS-1$
			
			JSONObject modelforecasting = new JSONObject();
			config.put("model_forecasting", modelforecasting); //$NON-NLS-1$
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.CLASSIFIER_MODEL.name());
			modelforecasting.put("classifier_model", PawsParameter.ClassifierModel.valueOf(pp.getValue()).key); //$NON-NLS-1$
			modelforecasting.put("start_year", run.getForecastStartYear()); //$NON-NLS-1$
			modelforecasting.put("end_year", run.getForecastEndYear()); //$NON-NLS-1$
		}
		
		Files.write(configPath, Collections.singletonList(config.toJSONString()), StandardCharsets.UTF_8);		
	}

	private void packageBasemapFiles(Path target) throws Exception{
		
		PawsConfiguration configuration;
		
		try(Session session = HibernateManager.openSession()){
			configuration = session.get(PawsConfiguration.class, run.getConfiguration().getUuid());
			configuration.getParameters().forEach(pp->pp.getValue());
			configuration.getConservationArea().getFileDataStoreLocation();
		}
		
		
		//BOUNDARY
		PawsParameter pp = configuration.findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
		
				
		if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)){
			//export area to shapefiles
			Path zip = target.resolve("ca_boundary.shp"); //$NON-NLS-1$
			exportArea(Area.AreaType.valueOf(pp.getValue().substring(PawsParameter.AREA_PREFIX.length())), zip);
			
		}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)){
			String filename = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
			Path srcShp = PawsFileManager.INSTANCE.getDirectory(configuration).resolve(filename);
			Path targetShp = target.resolve(srcShp.getFileName());
			packageShapefile(srcShp, targetShp);
		}
		
		//ALL OTHER LAYERS
		for (PawsParameter ppw : configuration.getParameters()) {
			if (!ppw.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())) continue;
			if (ppw.getValue().startsWith(PawsParameter.AREA_PREFIX)){
				//export area to shapefiles
				Area.AreaType type = Area.AreaType.valueOf(pp.getValue().substring(PawsParameter.AREA_PREFIX.length()));
				Path targetshp = target.resolve(type.name() + ".shp"); //$NON-NLS-1$
				exportArea(type, targetshp);
			}else if (ppw.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
				String filename = ppw.getValue().substring(PawsParameter.FILE_PREFIX.length());
				Path srcShp = PawsFileManager.INSTANCE.getDirectory(configuration).resolve(filename);
				Path targetShp = target.resolve(filename);
				packageShapefile(srcShp, targetShp);
			}
		}
	}
	
	/**
	 * Exports shapefile to targetFile (shape), then zips the results
	 * @param shpFile
	 * @param targetFile
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void packageShapefile(Path shpFile, Path targetFile) throws Exception{
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toUri().toURL());
		DataStore raw = DataStoreFinder.getDataStore(params);
		
		Name name = raw.getNames().get(0);
		
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
		params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, targetFile.toUri().toURL());
		
		DataStore shapefile = factory.createNewDataStore(params);
		
		SimpleFeatureType ftype = SimpleFeatureTypeBuilder.retype(raw.getSchema(name), targetCrs);
		shapefile.createSchema(ftype);
		
		ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(raw.getFeatureSource(name).getFeatures(), targetCrs);
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
				(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
		fs.setTransaction(new DefaultTransaction());
		fs.addFeatures( rfc );
		fs.getTransaction().commit();
		
		shapefile.dispose();
		
		
		//zip 
		String targetname = SharedUtils.getFilenameWithoutExtension(targetFile.getFileName().toString());
		Path zipFile = targetFile.getParent().resolve(targetname + ".zip"); //$NON-NLS-1$
		zipShapefile(targetFile, zipFile);
		
	}
	
	/**
	 * 
	 * Exports area to shapefile and then zips up results
	 * 
	 * @param type area type
	 * @param targetFile target shapefile
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void exportArea(Area.AreaType type, Path targetFile) throws Exception {
		
		String schema = "the_geom:MultiPolygon:srid=4326,fid:String,key:String"; //$NON-NLS-1$

		SimpleFeatureType ftype = SimpleFeatureTypeBuilder.retype( DataUtilities.createType(type.name(), schema.toString()), targetCrs);

		List<SimpleFeature> features = new ArrayList<>();
		MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, targetCrs, true);

		try(Session session = HibernateManager.openSession()){
			List<Area> areas = QueryFactory.buildQuery(session, Area.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"type", type}).getResultList(); //$NON-NLS-1$
			for (Area a : areas) {
				String fid = UuidUtils.uuidToString(a.getUuid());
				Geometry g = a.getGeometry();
				g = JTS.transform(g, transform);
				features.add(SimpleFeatureBuilder.build(ftype, new Object[] {g, fid, a.getKeyId()}, fid));
			}
		}
		
		FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, (URL)targetFile.toUri().toURL());
	
		DataStore shapefile = factory.createNewDataStore(params);
		shapefile.createSchema(ftype);
		FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
				(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
		fs.setTransaction(new DefaultTransaction());
		
		fs.addFeatures( DataUtilities.collection(features) );
		fs.getTransaction().commit();
		
		shapefile.dispose();
		
		String targetname = SharedUtils.getFilenameWithoutExtension(targetFile.getFileName().toString());
		Path zipFile = targetFile.getParent().resolve(targetname + ".zip"); //$NON-NLS-1$
		zipShapefile(targetFile, zipFile);
		
	}
	
	private void zipShapefile(Path shapefile, Path zipFile) throws IOException {
		String targetname = SharedUtils.getFilenameWithoutExtension(zipFile.getFileName().toString());

		List<File> allFiles = new ArrayList<>();
		try(Stream<Path> files = Files.list(shapefile.getParent())){
			allFiles.addAll(files.filter(item -> SharedUtils.getFilenameWithoutExtension(item.getFileName().toString()).equals(targetname))
				.map(item->item.toFile())
				.collect(Collectors.toList()));
		}
		ZipUtil.createZip(allFiles, zipFile.toFile(), new NullProgressMonitor());
		
		for (File f : allFiles ) {
			Files.delete(f.toPath());
		}
		packageFiles.add(zipFile);
	}
	
	private void packageData(Path datafile) throws Exception {
		classmappings = new HashMap<>();
		
		List<String> tempTables = new ArrayList<>();
		
		String mastertable = createTempTable();
		tempTables.add(mastertable);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
			
				PawsConfiguration configuration = session.get(PawsConfiguration.class, run.getConfiguration().getUuid());
				
				List<PawsSimpleClass> simple = QueryFactory.buildQuery(session, PawsSimpleClass.class, 
						new Object[] {"configuration", configuration}).list(); //$NON-NLS-1$
				
				List<PawsQueryClass> queries = QueryFactory.buildQuery(session, PawsQueryClass.class, 
						new Object[] {"configuration", configuration}).list(); //$NON-NLS-1$
				
				
				
				if (simple.isEmpty() && queries.isEmpty()) return;
				
				//create a master table
				StringBuilder create = new StringBuilder();
				create.append("CREATE TABLE "); //$NON-NLS-1$
				create.append(mastertable);
				create.append("( wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, "); //$NON-NLS-1$
				create.append(" wp_datetime timestamp,"); //$NON-NLS-1$
				
				int n = 1;
				for (PawsSimpleClass pc : simple) {
					String key = pc.getClassification();
					String colname = "pawsclass" + n; //$NON-NLS-1$
					
					if (!classmappings.containsKey(key)) classmappings.put(key, new ArrayList<>());
					classmappings.get(key).add(colname);
					
					create.append(colname + " varchar(8192),"); //$NON-NLS-1$
					n++;
				}
				for (PawsQueryClass qc : queries) {
					String key = qc.getClassification();
					String colname = "pawsclass" + n; //$NON-NLS-1$
					
					if (!classmappings.containsKey(key)) classmappings.put(key, new ArrayList<>());
					classmappings.get(key).add(colname);
					
					create.append(colname + " varchar(8192),"); //$NON-NLS-1$
					n++;
				}
				create.deleteCharAt(create.length() - 1);
				create.append(")"); //$NON-NLS-1$
				
				session.createNativeQuery(create.toString()).executeUpdate();
				
				//create all data table
				String alldata = createTempTable();
				tempTables.add(alldata);
				create = new StringBuilder();
				create.append("CREATE TABLE "); //$NON-NLS-1$
				create.append(alldata);
				create.append("( wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, "); //$NON-NLS-1$
				create.append(" wp_datetime timestamp)"); //$NON-NLS-1$

				session.createNativeQuery(create.toString()).executeUpdate();
				
				//populate all data table
				create = new StringBuilder();
				create.append("INSERT INTO "); //$NON-NLS-1$
				create.append(alldata);
				create.append("(wp_uuid, obs_uuid, x, y, wp_datetime)"); //$NON-NLS-1$
				create.append(" SELECT a.uuid, obs.uuid, a.x, a.y, a.datetime"); //$NON-NLS-1$
				create.append(" FROM smart.waypoint a "); //$NON-NLS-1$
				create.append(" LEFT JOIN smart.wp_observation_group g on g.wp_uuid = a.uuid "); //$NON-NLS-1$
				create.append(" LEFT JOIN smart.wp_observation obs ON obs.wp_group_uuid = g.uuid " ); //$NON-NLS-1$
				create.append(" WHERE a.ca_uuid = :ca AND a.source='PATROL' AND cast(a.datetime as date) between :start and :end "); //$NON-NLS-1$

				session.createNativeQuery(create.toString())
					.setParameter("ca", run.getConservationArea().getUuid()) //$NON-NLS-1$
					.setParameter("start", run.getDataStartDate()) //$NON-NLS-1$
					.setParameter("end", run.getDataEndDate()) //$NON-NLS-1$
					.executeUpdate();
				
				create = new StringBuilder();
				create.append("INSERT INTO "); //$NON-NLS-1$
				create.append(mastertable);
				create.append(" SELECT a.wp_uuid, a.obs_uuid, a.x, a.y, a.wp_datetime"); //$NON-NLS-1$
				
				StringBuilder from = new StringBuilder();
				from.append(alldata);
				from.append(" a "); //$NON-NLS-1$
				
				
				for (PawsSimpleClass pc : simple) {
					String table = createTempTable();
					tempTables.add(table);
					SimpleClassEngine e = new SimpleClassEngine(pc, alldata);//, run.getDataStartDate(), run.getDataEndDate());
					e.process(session, table);
					
					create.append(","); //$NON-NLS-1$
					create.append(table + ".pawsclass"); //$NON-NLS-1$
					from.append(" LEFT JOIN " + table + " ON a.obs_uuid = " + table + ".obs_uuid "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				
				List<QueryClassEngine> engines = new ArrayList<>();
				for (PawsQueryClass pc : queries) {
					QueryClassEngine e = new QueryClassEngine(pc, run.getDataStartDate(), run.getDataEndDate());
					e.process(session);
					engines.add(e);
					
					create.append(","); //$NON-NLS-1$
					create.append(" case when " + e.getTable() + "." + e.getObsColumn() + " is null then null else '" + pc.getClassification() + "' end"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					from.append(" LEFT JOIN " + e.getTable() + " ON a.obs_uuid = " + e.getTable() + "." + e.getObsColumn() + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				
				//execute
				create.append(" FROM "); //$NON-NLS-1$
				create.append(from);
				
				session.createNativeQuery(create.toString()).executeUpdate();
				
				writeToCsv(session, mastertable, datafile, simple.size() + queries.size());
				
				packageFiles.add(datafile);
				
				//drop all temp tables
				for (String table : tempTables) {
					session.createNativeQuery("DROP TABLE " + table).executeUpdate(); //$NON-NLS-1$
				}
				for (QueryClassEngine e : engines) e.dispose(session);
			}finally {
				session.getTransaction().commit();
			}
		}
		
	}
	
	private void writeToCsv(Session session, String tablename, Path filename, int numclasses) throws IOException {
		
		
		try(CSVWriter writer = new CSVWriter(Files.newBufferedWriter(filename, StandardCharsets.UTF_8))){
			//headers
			ArrayList<String> headers = new ArrayList<>();
			headers.add("Waypoint Date"); //$NON-NLS-1$
			headers.add("Waypoint Time"); //$NON-NLS-1$
			headers.add("Patrol Start Date"); //$NON-NLS-1$
			headers.add("Patrol End Date"); //$NON-NLS-1$
			headers.add("Patrol ID"); //$NON-NLS-1$
			headers.add("X"); //$NON-NLS-1$
			headers.add("Y"); //$NON-NLS-1$
			int datacols = 0;
			for (Entry<String, List<String>> items : classmappings.entrySet()) {
				for (String col :  items.getValue()) {
					headers.add(col);
					datacols ++;
				}
			}
			writer.writeNext(headers.toArray(new String[headers.size()]));
			
			//join to patrols to get start date, end date and patrol id if available
			StringBuilder select = new StringBuilder();
			select.append("SELECT p.id, p.start_date, p.end_date, a.* FROM "); //$NON-NLS-1$
			select.append(tablename);
			select.append(" a LEFT JOIN smart.patrol_waypoint pw on pw.wp_uuid = a.wp_uuid "); //$NON-NLS-1$
			select.append(" LEFT JOIN smart.patrol_leg_day pld on pld.uuid = pw.leg_day_uuid "); //$NON-NLS-1$
			select.append(" left join smart.patrol_leg pl on pld.patrol_leg_uuid = pl.uuid "); //$NON-NLS-1$
			select.append(" left join smart.patrol p on p.uuid = pl.patrol_uuid "); //$NON-NLS-1$
//			select.append(" WHERE ");
//			for (int i = 1; i <= numclasses; i ++) {
//				select.append("pawsclass" + i + " is not null OR ");
//			}
//			select.deleteCharAt(select.length() - 1);
//			select.deleteCharAt(select.length() - 1);
//			select.deleteCharAt(select.length() - 1);
			
			SimpleDateFormat ff = new SimpleDateFormat("MMM dd, yyyy");
			try(ScrollableResults results = session.createNativeQuery(select.toString()).scroll()){
				while(results.next()) {
					String[] data = new String[headers.size()];
					
					Object[] items = results.get();
					
					String pid = (String)items[0];
					Date pstart = (Date)items[1];
					Date pend = (Date)items[2];
					
//					byte[] wpuuid = (byte[]) items[0];
//					byte[] obsuuid = (byte[]) items[1];
					double x = (double)items[5];
					double y = (double)items[6];
					Timestamp datetime = (Timestamp)items[7];
										
					//Waypoint Date
					int index = 0;
					data[index++] = ff.format(datetime);
					data[index++] = DateFormat.getTimeInstance().format(datetime);
					
					//Start Date
					data[index++] = ff.format(pstart);
					
					//End Date
					data[index++] = ff.format(pend);
					
					//PatrolID
					data[index++] = pid;
					
					//X
					data[index++] = String.valueOf(x);
					//Y
					data[index++] = String.valueOf(y);					
					
					
					for (int i = 1; i <= datacols; i ++) {
						data[index++] = (String)items[7+i];
					}
					writer.writeNext(data);
				}
			}
		}
	}
	
	
}
