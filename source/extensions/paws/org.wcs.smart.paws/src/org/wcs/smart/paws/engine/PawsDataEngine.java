package org.wcs.smart.paws.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.referencing.CRS;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsRun.Status;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

public class PawsDataEngine {

	private static AtomicLong tableCnter = new AtomicLong();

	private PawsRun run;
	
	private HashMap<String, String> classmappings;
	
	public PawsDataEngine(PawsRun run) {
		this.run = run;
	}
	
	private String createTempTable() {
		return "query_temp_paws_" + tableCnter.incrementAndGet();//$NON-NLS-1$ 
	}
	
	public Path createDataPackage() throws Exception{
		
		try(Session session = HibernateManager.openSession()){
			session.saveOrUpdate(run);
			run.getConservationArea().getFileDataStoreLocation();
		}
		
		Path workingDir = Paths.get( run.getConservationArea().getFileDataStoreLocation() )
				.resolve(PawsPlugIn.PAWS_DIR)
				.resolve("packaging_" + System.nanoTime());
		
		if (!Files.exists(workingDir)){
			Files.createDirectories(workingDir);
		}
		
		Path dataFiles = workingDir.resolve("SMARTdata.json");
		packageData(dataFiles);
		
		packageBasemapFiles(workingDir);
		
		createConfig(workingDir);
		
		Path runDir = PawsManager.INSTANCE.getDirectory(run);
		if (!Files.deleteIfExists(runDir)){
			Files.createDirectories(runDir);
		}
		
		Path packageFile = runDir.resolve("package.zip");
		ZipUtil.createZip(new File[]{workingDir.toFile()}, packageFile.toFile(), new NullProgressMonitor());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				session.saveOrUpdate(run);
				run.setPackageFile(packageFile.getFileName().toString());
				run.setStatus(Status.UPLOADING_DATA);
				run.setRunDate(LocalDateTime.now());
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();;
				throw ex;
			}
		}
		return packageFile;
	}
	
	private void createConfig(Path target) throws Exception{
		Path configPath = target.resolve("config.json");
		
		JSONObject config = new JSONObject();
		
		try(Session session = HibernateManager.openSession()){
			
			run = (PawsRun) session.merge(run);
			run.getConfiguration().findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
			
			
			config.put("container_name", "TODO");
			config.put("run_id", run.getRunId());
			
			PawsParameter pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
			if (pp == null) throw new Exception("Timezone value must be provided.  Update the configuration and try again.");
			config.put("time_zone", pp.getValue());
			
			pp = run.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
			if (pp == null) throw new Exception("CRS must be provided.  Update the configuration and try again.");
			UUID projUuid = UuidUtils.stringToUuid(pp.getValue().split(":")[0]);
			Projection p = session.get(Projection.class,projUuid);
			if (p == null)  throw new Exception("CRS must be provided.  Update the configuration and try again.");
			CoordinateReferenceSystem crs = CRS.parseWKT(p.getDefinition());
			//TODO - looks like we need the proj4 string
			//coordinate_reference_system_proj_4
			config.put("coordinate_reference_system_wkt", crs.toWKT());
			
			//TODO: bounds
			
			JSONArray layers = new JSONArray();
			PawsParameter.FixedParameter[] bm = {
					PawsParameter.FixedParameter.LYR_BOUNDARY,
					PawsParameter.FixedParameter.LYR_WATER,
					PawsParameter.FixedParameter.LYR_ROAD,
					PawsParameter.FixedParameter.LYR_CONTOUR
			};
			for (PawsParameter.FixedParameter ll : bm){
				pp = run.getConfiguration().findParameter(ll.name());
				if (pp != null){
					layers.add(ll.name() + ".zip");
				}
			}
			config.put("geo_feature_shape_file_names", layers);
			
			//TODO: dates
			config.put("observation_file_name", "SMARTdata.json");
			config.put("protected_are_name", run.getConservationArea().getName());
			
		
		}
		
		Files.write(configPath, Collections.singletonList(config.toJSONString()), StandardCharsets.UTF_8);
		
	}
	
	private void packageBasemapFiles(Path target) throws Exception{
		PawsParameter.FixedParameter[] bm = {
				PawsParameter.FixedParameter.LYR_BOUNDARY,
				PawsParameter.FixedParameter.LYR_CONTOUR,
				PawsParameter.FixedParameter.LYR_ROAD,
				PawsParameter.FixedParameter.LYR_WATER,
		};
		
		PawsConfiguration configuration;
		
		try(Session session = HibernateManager.openSession()){
			configuration = session.get(PawsConfiguration.class, run.getConfiguration().getUuid());
			configuration.getParameters().forEach(pp->pp.getValue());
		}
		
		for (PawsParameter.FixedParameter layer : bm){
			PawsParameter pp = configuration.findParameter(layer.name());
			if (pp != null && pp.getValue() != null){
				Path zip = target.resolve(layer.name() + ".zip");
				
				if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)){
					//export area to shapefiles
				}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)){
					String filename = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
				
					Path shpfile = PawsManager.INSTANCE.getDirectory(configuration).resolve(filename);
					
					List<File> allFiles = new ArrayList<>();
					//find all files with the same filesname;
					allFiles.add(shpfile.toFile());
					String targetname = SharedUtils.getFilenameWithoutExtension(shpfile.getFileName().toString());
					allFiles.addAll(Files.list(shpfile.getParent())
						.filter(item -> SharedUtils.getFilenameWithoutExtension(item.getFileName().toString()).equals(targetname) )
						.map(item->item.toFile())
						.collect(Collectors.toList()));
					
					ZipUtil.createZip(allFiles, zip.toFile(), new NullProgressMonitor());
				}
			}
		}
	}
	
	private void packageData(Path datafile) throws Exception {
		classmappings = new HashMap<>();
		
		List<String> tempTables = new ArrayList<>();
		
		String mastertable = createTempTable();
		
		try(Session session = HibernateManager.openSession()){
			PawsConfiguration configuration = session.get(PawsConfiguration.class, run.getConfiguration().getUuid());
			
			List<PawsSimpleClass> simple = QueryFactory.buildQuery(session, PawsSimpleClass.class, 
					new Object[] {"configuration", configuration}).list();
			
			List<PawsQueryClass> queries = QueryFactory.buildQuery(session, PawsQueryClass.class, 
					new Object[] {"configuration", configuration}).list();
			
			StringBuilder create = new StringBuilder();
			create.append("CREATE TABLE ");
			create.append(mastertable);
			create.append("( wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, datetime timestamp ");
			
			StringBuilder select = new StringBuilder();
			select.append("SELECT ");
			StringBuilder from = new StringBuilder();
			
			boolean first = true;
			int n = 2;
			for (PawsSimpleClass pc : simple) {
				String table = createTempTable();
				tempTables.add(table);
				SimpleClassEngine e = new SimpleClassEngine(pc, run.getDataStartDate(), run.getDataEndDate());
				e.process(session, table);
			
				if (first) {
					create.append(",pawsclass1 varchar(8192)");
					from.append(table + " a ");
					select.append("a.wp_uuid, a.obs_uuid, a.x, a.y, a.datetime, a.pawsclass as pawsclass1");
					first = false;
					classmappings.put("pawsclass1", pc.getClassification());
				}else {
					create.append(",pawsclass" + n + " varchar(8192)");
					from.append(" LEFT JOIN " + table + " ON a.obs_uuid = " + table + ".obs_uuid ");
					select.append("," + table + ".pawsclass as pawsclass" + n);
					classmappings.put("pawsclass" + n, pc.getClassification());
					n++;
				}
				
			}
			create.append(")");
			
			StringBuilder alldata = new StringBuilder();
			alldata.append("INSERT INTO ");
			alldata.append(mastertable);
			alldata.append(" ");
			alldata.append(select.toString());
			alldata.append(" FROM ");
			alldata.append(from.toString());
			
			System.out.println(alldata.toString());
			
			session.doWork(new Work(){

				@Override
				public void execute(Connection c) throws SQLException {
					c.createStatement().execute(create.toString());
					c.createStatement().execute(alldata.toString());
				}
				
			});
			
			
			writeToJson(session, mastertable, datafile);
			
			//dorp all temp tables
			tempTables.add(mastertable);
			session.doWork(new Work(){
			
				@Override
				public void execute(Connection c) throws SQLException {
					for(String table : tempTables){
						try{
							c.createStatement().execute("DROP TABLE " + table);
						}catch (Exception ex){
							PawsPlugIn.log(ex.getMessage(), ex);
						}
					}
				}
			});
		}
		
	}
	
	private void writeToJson(Session session, String tablename, Path filename) throws IOException {
		
		try(BufferedWriter writer = Files.newBufferedWriter(filename, StandardCharsets.UTF_8)){
			writer.write("[");
			writer.newLine();
			try(ScrollableResults results = session.createNativeQuery("SELECT * FROM " + tablename).scroll()){
				while(results.next()) {
					
					Object[] items = results.get();
					
					byte[] wpuuid = (byte[]) items[0];
					byte[] obsuuid = (byte[]) items[1];
					double x = (double)items[2];
					double y = (double)items[3];
					Timestamp datetime = (Timestamp)items[4];
					
					JSONObject feature = new JSONObject();
					
					feature.put("type", "Feature");
					
					JSONObject geometry = new JSONObject();
					geometry.put("type", "Point");
					JSONArray coords = new JSONArray();
					coords.add(x);
					coords.add(y);
					geometry.put("coordinates", coords);
					
					feature.put("geometry", geometry);
					
					JSONObject properties = new JSONObject();
					properties.put("datetime", datetime);
					properties.put("wp_uuid", UuidUtils.uuidToString( UuidUtils.byteToUUID(wpuuid)));
					properties.put("obs_uuid", UuidUtils.uuidToString( UuidUtils.byteToUUID(obsuuid)));
					for (int i = 1; i <= classmappings.size(); i ++) {
						String col = "pawsclass" + i;
						String value = (String)items[4+i];
						properties.put(col, value);
					}
					feature.put("properties", properties);
	
					
					writer.write(feature.toJSONString());
					writer.write(",");
					writer.newLine();
				}
			}
			
			
			writer.write("]");
		}
	}
	
	
}
