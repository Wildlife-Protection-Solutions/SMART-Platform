package org.wcs.smart.connect.query.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.model.QueryColumn;

import au.com.bytecode.opencsv.CSVWriter;

public class ShpExporter {
public static final String FORMAT_KEY = "shp"; //$NON-NLS-1$
	
	private final Logger logger = Logger.getLogger(ShpExporter.class.getName());
	
	private File f;
	private Locale l;
	
	public ShpExporter(File f,  Locale l){
		this.f = f;
		this.l = l;
	}
	
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(SimpleQuery query, AbstractDbFeatureResultSet results, Session session){
		
		session.doWork(new Work(){
			@Override
			public void execute(Connection c) throws SQLException {
				try{
					URL shpFileURL = f.toURI().toURL();
					
					FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
			        Map<String, Serializable> params = new HashMap<String, Serializable>();
					params.put(ShapefileDataStoreFactory.URLP.key, shpFileURL);
					
					List<QueryColumn> columns = query.getQueryColumns(l, session);
					DataStore shapefile = factory.createNewDataStore(params);
					SimpleFeatureType type = DataUtilities.createType("smartqueryresults", results.getFeatureSchemaDef(columns, false));
					
					ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
					
					try (ResultSet rs = results.getQueryResultSet(c)){
						while(rs.next()){
							SimpleFeature sf = results.toFeature(rs, columns, c, type);
							features.add(sf);
						}
					}
					
					shapefile.createSchema(type);
					
					FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
							(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
					fs.setTransaction(new DefaultTransaction());
					fs.addFeatures( DataUtilities.collection(features) );
					fs.getTransaction().commit();
				}catch (Exception ex){
					throw new SQLException(ex);
				}
			}			
		});
	}
	
	/**
	 * Exports simple queries whose results are represented by a memory collection.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 * @throws SQLException
	 */
	public void exportResults(SimpleQuery query, 
			IMemoryTableResultSet<IResultItem> results, 
			Session session) throws SQLException{
		
//		if (!(results instanceof AbstractDbFeatureResultSet)){
//			return;
//		}
//		
//		AbstractDbFeatureResultSet fsetresults = (AbstractDbFeatureResultSet) results;
//		session.doWork(new Work(){
//			@Override
//			public void execute(Connection c) throws SQLException {
//				try{
//					URL shpFileURL = f.toURI().toURL();
//					
//					FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory("shp"); //$NON-NLS-1$
//			        Map<String, Serializable> params = new HashMap<String, Serializable>();
//					params.put(ShapefileDataStoreFactory.URLP.key, shpFileURL);
//					
//					List<QueryColumn> columns = query.getQueryColumns(l, session);
//					DataStore shapefile = factory.createNewDataStore(params);
//					SimpleFeatureType type = DataUtilities.createType("smartqueryresults", fsetresults.getFeatureSchemaDef(columns, false));
//					
//					ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
//					
//					for (Iterator<? extends IResultItem> iterator = results.getIterator(); iterator.hasNext();) {
//						IResultItem item = iterator.next();
//						features.add(fsetresults.toFeature(item, columns));
//					}
//					
//					shapefile.createSchema(type);
//					
//					
//					FeatureStore<SimpleFeatureType, SimpleFeature> fs = 
//							(FeatureStore<SimpleFeatureType, SimpleFeature>) shapefile.getFeatureSource(shapefile.getTypeNames()[0]);
//					fs.setTransaction(new DefaultTransaction());
//					fs.addFeatures( DataUtilities.collection(features) );
//					fs.getTransaction().commit();
//				}catch (Exception ex){
//					throw new SQLException(ex);
//				}
//			}			
//		});
		
		
	
	}
}
