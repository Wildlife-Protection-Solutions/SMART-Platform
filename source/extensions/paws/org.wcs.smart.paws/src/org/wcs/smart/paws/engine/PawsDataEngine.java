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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsRun.Status;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.paws.model.PawsWorkspace;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

import au.com.bytecode.opencsv.CSVWriter;

public class PawsDataEngine {

	public static final String DATA_FILE_NAME  = "SMARTdata.csv";
	
	public static final String CONFIG_FILE_NAME  = "config.json";
	
	private static AtomicLong tableCnter = new AtomicLong();

	private PawsRun run;
	
	private HashMap<String, String> classmappings;
	private List<Path> packageFiles;
	private CoordinateReferenceSystem targetCrs;
	
	
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
			
			PawsParameter pp = mrun.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
			if (pp == null) throw new Exception("CRS must be provided.  Update the configuration and try again.");
			UUID projUuid = UuidUtils.stringToUuid(pp.getValue().split(":")[0]);
			Projection p = session.get(Projection.class,projUuid);
			if (p == null)  throw new Exception("CRS must be provided.  Update the configuration and try again.");
			targetCrs = CRS.parseWKT(p.getDefinition());
			
			workingDir = PawsManager.INSTANCE.getDirectory(mrun);
		}
		
		
		
		if (!Files.exists(workingDir)){
			Files.createDirectories(workingDir);
		}
		
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
	
	private void createConfig(Path target) throws Exception{
		Path configPath = target.resolve(CONFIG_FILE_NAME);
		
		JSONObject config = new JSONObject();
		
		try(Session session = HibernateManager.openSession()){
			
			run = (PawsRun) session.merge(run);
			
			
			PawsWorkspace ws = QueryFactory.buildQuery(session, PawsWorkspace.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
			if (ws == null || !ws.isConfigured()){
				throw new Exception("PAWS Workspace not configured.  You must first configure the PAWS Workspace before you can run paws analysis.");
			}
			String url = ws.getUrl() + "?" + ws.getClientId();
			config.put("container_name", url);
			
			config.put("run_id", run.getRunId());
			
			//CRS - everything must be in this crs
			config.put("geo_wkt_file", "crs.wkt");
			writeCRS(target.resolve("crs.wkt"), targetCrs.toWKT());
			
			PawsParameter pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
			//TODO: this needs to be in meters; so assuming the projection is in meters
			config.put("spatial_resoltuion", pp.getValue());
			
			
			//area_boundary
			//must be provided in lat/long
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_BNDS.name());
			String[] bits = pp.getValue().split("\\s+");
			double x1 = Double.parseDouble(bits[0]);
			double y1 = Double.parseDouble(bits[1]);
			double x2 = Double.parseDouble(bits[2]);
			double y2 = Double.parseDouble(bits[3]);
			ReferencedEnvelope re = new ReferencedEnvelope(x1,x2, y1, y2, targetCrs);
			re = ReprojectUtils.reproject(re, SmartDB.DATABASE_CRS);
			
			JSONObject bnds = new JSONObject();
			bnds.put("longitude_min", re.getMinX());
			bnds.put("longitude_max", re.getMaxX());
			bnds.put("latitude_min", re.getMinY());
			bnds.put("latitude_max", re.getMaxY());
			config.put("area_boundary", bnds);
			
			
			//shapefiles
			JSONObject shapefiles = new JSONObject();
			config.put("geo_feature_shape_files", shapefiles);
			
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
			shapefiles.put("boundary_file_name", "ca_boundary");
			shapefiles.put("boundary_file_name", "ca_boundary.zip");
			
			JSONArray other = new JSONArray();
			shapefiles.put("additional_shape_files", other);
			for (PawsParameter ppc : run.getConfiguration().getParameters()) {
				if (!ppc.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())) continue;
				String value = ppc.getValue();
				if (value == null || value.isBlank()) continue;
				
				if (value.startsWith(PawsParameter.AREA_PREFIX)) {
					Area.AreaType atype = Area.AreaType.valueOf( value.substring(PawsParameter.AREA_PREFIX.length()) );
					
					JSONObject item = new JSONObject();
					item.put("file_name", atype.name() + ".shp");
					item.put("layer_name",  atype);
					other.add(item);
					
				}else if (value.startsWith(PawsParameter.FILE_PREFIX)) {
					String filename = value.substring(PawsParameter.FILE_PREFIX.length());
					JSONObject item = new JSONObject();
					item.put("file_name", filename);
					item.put("layer_name",  SharedUtils.getFilenameWithoutExtension(filename));
					other.add(item);
				}
			}

			//patrol_observation
			JSONObject patrolobs = new JSONObject();
			config.put("patrol_observations", patrolobs);
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
			patrolobs.put("start_date", formatter.format(run.getDataStartDate()));
			patrolobs.put("end_date", formatter.format(run.getDataEndDate()));

			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
			if (pp == null) throw new Exception("Timezone value must be provided.  Update the configuration and try again.");
			patrolobs.put("time_zone", pp.getValue());
			patrolobs.put("file_name", DATA_FILE_NAME);
			
			
			//illegal class mappings
			JSONObject mappings = new JSONObject();
			config.put("illegal_activith_class_mapping", mappings);
			
			config.put("protected_are_name", run.getConservationArea().getName());
			
		
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.TRAINING_RES.name());
			config.put("temporal_training_resolution", PawsParameter.TrainingResolution.valueOf(pp.getValue()).key);
			
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.CLASSIFIER_MODEL.name());
			config.put("classifier_model", PawsParameter.ClassifierModel.valueOf(pp.getValue()).key);

			JSONObject modelexperimentation = new JSONObject();
			config.put("model_experimentation", modelexperimentation);
			modelexperimentation.put("train_start_year", run.getTrainStartYear());
			modelexperimentation.put("train_end_year", run.getTrainEndYear());
			modelexperimentation.put("test_start_year", run.getTestStartYear());
			modelexperimentation.put("test_end_year", run.getTestEndYear());
			
			JSONObject modelforecasting = new JSONObject();
			config.put("model_forecasting", modelexperimentation);
			modelforecasting.put("start_year", run.getForecastStartYear());
			modelforecasting.put("end_year", run.getForecastEndYear());
			
			
		}
		
		Files.write(configPath, Collections.singletonList(config.toJSONString()), StandardCharsets.UTF_8);
		
	}
	
	private void writeCRS(Path target, String text) throws IOException {
		Files.write(target, text.getBytes());
		packageFiles.add(target);
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
			Path zip = target.resolve("ca_boundary.shp");
			exportArea(Area.AreaType.valueOf(pp.getValue().substring(PawsParameter.AREA_PREFIX.length())), zip);
			
		}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)){
			String filename = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
			Path srcShp = PawsManager.INSTANCE.getDirectory(configuration).resolve(filename);
			Path targetShp = target.resolve(srcShp.getFileName());
			packageShapefile(srcShp, targetShp);
		}
		
		//ALL OTHER LAYERS
		for (PawsParameter ppw : configuration.getParameters()) {
			if (!ppw.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())) continue;
			if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)){
				//export area to shapefiles
				Area.AreaType type = Area.AreaType.valueOf(pp.getValue().substring(PawsParameter.AREA_PREFIX.length()));
				Path targetshp = target.resolve(type.name() + ".shp");
				exportArea(type, targetshp);
			}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
				String filename = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
				Path srcShp = PawsManager.INSTANCE.getDirectory(configuration).resolve(filename);
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
	private void packageShapefile(Path shpFile, Path targetFile) throws Exception{
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toUri().toURL());
		DataStore raw = DataStoreFinder.getDataStore(params);
		
		Name name = raw.getNames().get(0);
		CoordinateReferenceSystem srcCrs = raw.getSchema(name).getCoordinateReferenceSystem();
		
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
		Path zipFile = targetFile.getParent().resolve(targetname + ".zip");
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
	private void exportArea(Area.AreaType type, Path targetFile) throws Exception {
		
		String schema = "the_geom:MultiPolygon:srid=4326,fid:String,key:String"; //$NON-NLS-1$ //$NON-NLS-2$

		SimpleFeatureType ftype = SimpleFeatureTypeBuilder.retype( DataUtilities.createType(type.name(), schema.toString()), targetCrs);

		List<SimpleFeature> features = new ArrayList<>();
		MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, targetCrs, true);

		try(Session session = HibernateManager.openSession()){
			List<Area> areas = QueryFactory.buildQuery(session, Area.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
					new Object[] {"type", type}).getResultList();
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
		Path zipFile = targetFile.getParent().resolve(targetname + ".zip");
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
						new Object[] {"configuration", configuration}).list();
				
				List<PawsQueryClass> queries = QueryFactory.buildQuery(session, PawsQueryClass.class, 
						new Object[] {"configuration", configuration}).list();
				
				
				
				if (simple.isEmpty() && queries.isEmpty()) return;
				
				//create a master table
				StringBuilder create = new StringBuilder();
				create.append("CREATE TABLE ");
				create.append(mastertable);
				create.append("( wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, datetime timestamp,");
				
				int n = 1;
				for (PawsSimpleClass pc : simple) {
					create.append("pawsclass" + (n++) + " varchar(8192),");
					classmappings.put("pawsclass" + n, pc.getClassification());
				}
				for (PawsQueryClass qc : queries) {
					create.append("pawsclass" + (n++) + " varchar(8192),");
					classmappings.put("pawsclass" + n, qc.getClassification());
				}
				create.deleteCharAt(create.length() - 1);
				create.append(")");
				
				System.out.println(create.toString());
				session.createNativeQuery(create.toString()).executeUpdate();
				
				//create all data table
				String alldata = createTempTable();
				tempTables.add(alldata);
				create = new StringBuilder();
				create.append("CREATE TABLE ");
				create.append(alldata);
				create.append("( wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, datetime timestamp");
				create.append(")");
				System.out.println(create.toString());
				session.createNativeQuery(create.toString()).executeUpdate();
				
				//populate add data table
				create = new StringBuilder();
				create.append("INSERT INTO ");
				create.append(alldata);
				create.append("(wp_uuid, obs_uuid, x, y, datetime)");
				create.append(" SELECT a.uuid, obs.uuid, a.x, a.y, a.datetime ");
				create.append(" FROM smart.waypoint a ");
				create.append(" JOIN smart.wp_observation obs ON obs.wp_uuid = a.uuid " );
				create.append(" WHERE a.datetime between :start and :end ");
				System.out.println(create.toString());
				session.createNativeQuery(create.toString())
					.setParameter("start", run.getDataStartDate())
					.setParameter("end", run.getDataEndDate())
					.executeUpdate();
				
				create = new StringBuilder();
				create.append("INSERT INTO ");
				create.append(mastertable);
				create.append(" SELECT a.wp_uuid, a.obs_uuid, a.x, a.y, a.datetime ");
				
				StringBuilder from = new StringBuilder();
				from.append(alldata);
				from.append(" a ");
				
				
				for (PawsSimpleClass pc : simple) {
					String table = createTempTable();
					tempTables.add(table);
					SimpleClassEngine e = new SimpleClassEngine(pc, run.getDataStartDate(), run.getDataEndDate());
					e.process(session, table);
					
					create.append(",");
					create.append(table + ".pawsclass");
					from.append(" LEFT JOIN " + table + " ON a.obs_uuid = " + table + ".obs_uuid ");
				}
				
				List<QueryClassEngine> engines = new ArrayList<>();
				for (PawsQueryClass pc : queries) {
					QueryClassEngine e = new QueryClassEngine(pc, run.getDataStartDate(), run.getDataEndDate());
					e.process(session);
					engines.add(e);
					
					create.append(",");
					create.append(" case when " + e.getTable() + "." + e.getObsColumn() + " is null then null else '" + pc.getClassification() + "' end");
					from.append(" LEFT JOIN " + e.getTable() + " ON a.obs_uuid = " + e.getTable() + "." + e.getObsColumn() + " ");
				}
				
				//execute
				create.append(" FROM ");
				create.append(from);
				
				System.out.println(create.toString());
				session.createNativeQuery(create.toString()).executeUpdate();
				
				
				writeToCsv(session, mastertable, datafile, simple.size() + queries.size());
				
				packageFiles.add(datafile);
				
				//drop all temp tables
				for (String table : tempTables) {
					System.out.println("DROP TABLE " + table);
					session.createNativeQuery("DROP TABLE " + table).executeUpdate();
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
			headers.add("Waypoint Date");
			headers.add("Start Date");
			headers.add("End Date");
			headers.add("Patrol ID");
			headers.add("X");
			headers.add("Y");
			for (int i = 1; i <= classmappings.size(); i ++) {
				headers.add("CLASS" + i);
			}
			writer.writeNext(headers.toArray(new String[headers.size()]));
			
			//join to patrols to get start date, end date and patrol id if available
			StringBuilder select = new StringBuilder();
			select.append("SELECT a.*, p.id, p.start_date, p.end_date FROM ");
			select.append(tablename);
			select.append(" a LEFT JOIN smart.patrol_waypoint pw on pw.wp_uuid = a.wp_uuid LEFT JOIN smart.patrol_leg_day pld on pld.uuid = pw.leg_day_uuid ");
			select.append(" left join smart.patrol_leg pl on pld.patrol_leg_uuid = pl.uuid ");
			select.append(" left join smart.patrol p on p.uuid = pl.patrol_uuid ");
			select.append(" WHERE ");
			for (int i = 1; i <= numclasses; i ++) {
				select.append("pawsclass" + i + " is not null OR ");
			}
			select.deleteCharAt(select.length() - 1);
			select.deleteCharAt(select.length() - 1);
			select.deleteCharAt(select.length() - 1);
			
			
			try(ScrollableResults results = session.createNativeQuery(select.toString()).scroll()){
				while(results.next()) {
					String[] data = new String[headers.size()];
					
					Object[] items = results.get();
					
					byte[] wpuuid = (byte[]) items[0];
					byte[] obsuuid = (byte[]) items[1];
					double x = (double)items[2];
					double y = (double)items[3];
					Timestamp datetime = (Timestamp)items[4];
					
					//Waypoint Date
					int index = 0;
					data[index++] = DateFormat.getDateTimeInstance().format(datetime);
					
					//Start Date
					//TODO
					data[index++] = DateFormat.getDateInstance().format((Date)items[6 + classmappings.size() ]);
					
					//End Date
					//TODO
					data[index++] = DateFormat.getDateInstance().format((Date)items[7 + classmappings.size() ]);
					
					//PatrolID
					//TODO
					data[index++] = (String) items[5 + classmappings.size() ];
					
					//TODO: this needs to be reprojected to target CRS
					Point pnt = ReprojectUtils.transform(x, y, targetCrs);
					//X
					data[index++] = String.valueOf( pnt.getX() );
					//Y
					data[index++] = String.valueOf( pnt.getY() );					
					
					for (int i = 1; i <= classmappings.size(); i ++) {
						data[index++] = (String)items[4+i];
					}
					writer.writeNext(data);
				}
			}
		}
	}
	
	
}
